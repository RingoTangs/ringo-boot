package io.github.ringotangs.ringoboot.verification.autoconfigure.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerifyResult;
import io.github.ringotangs.ringoboot.verification.store.StoreResult;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

abstract class VerificationStoreContract {

    private static final VerificationPolicy POLICY = new VerificationPolicy(6, Duration.ofMinutes(5), 2);

    protected abstract VerificationStore store();

    @Test
    void storesAndOverwritesReissue() {
        VerificationKey key = key("account");
        Instant now = now();

        StoreResult stored = store().store(key, "123456", POLICY, now);
        StoreResult overwritten = store().store(key, "654321", POLICY, now.plusSeconds(10));

        assertEquals(now.plus(POLICY.ttl()), stored.expiresAt());
        assertEquals(now.plusSeconds(10).plus(POLICY.ttl()), overwritten.expiresAt());
        assertEquals(VerifyResult.MISMATCH, store().verifyAndConsume(key, "123456", now.plusSeconds(11)));
        assertEquals(VerifyResult.SUCCESS, store().verifyAndConsume(key, "654321", now.plusSeconds(12)));
    }

    @Test
    void decrementsAttemptsAndConsumesExhaustedCode() {
        VerificationKey key = key("account");
        Instant now = now();
        store().store(key, "123456", POLICY, now);

        assertEquals(VerifyResult.MISMATCH, store().verifyAndConsume(key, "000000", now.plusSeconds(1)));
        assertEquals(VerifyResult.ATTEMPTS_EXHAUSTED, store().verifyAndConsume(key, "000000", now.plusSeconds(2)));
        assertEquals(VerifyResult.NOT_FOUND, store().verifyAndConsume(key, "123456", now.plusSeconds(3)));
    }

    @Test
    void reportsExpiredThenConsumesRecord() {
        VerificationKey key = key("account");
        Instant now = now();
        store().store(key, "123456", POLICY, now);

        assertEquals(VerifyResult.EXPIRED, store().verifyAndConsume(key, "123456", now.plus(POLICY.ttl())));
        assertEquals(
                VerifyResult.NOT_FOUND,
                store().verifyAndConsume(key, "123456", now.plus(POLICY.ttl()).plusSeconds(1)));
    }

    @Test
    void invalidatesOnlyMatchingCode() {
        VerificationKey key = key("account");
        Instant now = now();
        store().store(key, "123456", POLICY, now);

        assertFalse(store().invalidate(key, "000000"));
        assertTrue(store().invalidate(key, "123456"));
        assertEquals(VerifyResult.NOT_FOUND, store().verifyAndConsume(key, "123456", now.plusSeconds(1)));
    }

    @Test
    void isolatesBusinessNamespaces() {
        String subject = UUID.randomUUID() + "@example.com";
        VerificationKey account = new VerificationKey("account", "login", subject);
        VerificationKey payment = new VerificationKey("payment", "login", subject);
        Instant now = now();
        store().store(account, "123456", POLICY, now);
        store().store(payment, "654321", POLICY, now);

        assertEquals(VerifyResult.SUCCESS, store().verifyAndConsume(account, "123456", now.plusSeconds(1)));
        assertEquals(VerifyResult.SUCCESS, store().verifyAndConsume(payment, "654321", now.plusSeconds(1)));
    }

    @Test
    void permitsOnlyOneConcurrentSuccessfulConsumption() throws Exception {
        VerificationKey key = key("account");
        Instant now = now();
        store().store(key, "123456", POLICY, now);
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(threads)) {
            @SuppressWarnings("unchecked")
            Future<VerifyResult>[] futures = new Future[threads];
            for (int index = 0; index < threads; index++) {
                futures[index] = executor.submit(() -> {
                    start.await();
                    return store().verifyAndConsume(key, "123456", now.plusSeconds(1));
                });
            }
            start.countDown();

            int successes = 0;
            for (Future<VerifyResult> future : futures) {
                if (future.get() == VerifyResult.SUCCESS) {
                    successes++;
                }
            }
            assertEquals(1, successes);
        }
    }

    private VerificationKey key(String namespace) {
        return new VerificationKey(namespace, "login", UUID.randomUUID() + "@example.com");
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
