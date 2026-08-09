package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerificationResult;
import io.github.ringotangs.ringoboot.verification.store.StoreResult;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

abstract class VerificationStoreContract {

    private static final VerificationPolicy POLICY =
            new VerificationPolicy(6, Duration.ofMinutes(5), 2, Duration.ofMinutes(1));

    protected abstract VerificationStore store();

    @Test
    void storesAndThrottlesReissue() {
        VerificationKey key = key("account");
        Instant now = Instant.now();

        StoreResult.Stored stored =
                assertInstanceOf(StoreResult.Stored.class, store().store(key, "123456", POLICY, now));
        StoreResult.Throttled throttled = assertInstanceOf(
                StoreResult.Throttled.class, store().store(key, "654321", POLICY, now.plusSeconds(10)));

        assertEquals(now.plus(POLICY.ttl()), stored.expiresAt());
        assertEquals(Duration.ofSeconds(50), throttled.retryAfter());
        assertEquals(VerificationResult.SUCCESS, store().verifyAndConsume(key, "123456", now.plusSeconds(11)));
    }

    @Test
    void decrementsAttemptsAndConsumesExhaustedCode() {
        VerificationKey key = key("account");
        Instant now = Instant.now();
        store().store(key, "123456", POLICY, now);

        assertEquals(VerificationResult.MISMATCH, store().verifyAndConsume(key, "000000", now.plusSeconds(1)));
        assertEquals(
                VerificationResult.ATTEMPTS_EXHAUSTED, store().verifyAndConsume(key, "000000", now.plusSeconds(2)));
        assertEquals(VerificationResult.NOT_FOUND, store().verifyAndConsume(key, "123456", now.plusSeconds(3)));
    }

    @Test
    void reportsExpiredThenConsumesRecord() {
        VerificationKey key = key("account");
        Instant now = Instant.now();
        store().store(key, "123456", POLICY, now);

        assertEquals(VerificationResult.EXPIRED, store().verifyAndConsume(key, "123456", now.plus(POLICY.ttl())));
        assertEquals(
                VerificationResult.NOT_FOUND,
                store().verifyAndConsume(key, "123456", now.plus(POLICY.ttl()).plusSeconds(1)));
    }

    @Test
    void invalidatesOnlyMatchingCode() {
        VerificationKey key = key("account");
        Instant now = Instant.now();
        store().store(key, "123456", POLICY, now);

        assertFalse(store().invalidate(key, "000000"));
        assertTrue(store().invalidate(key, "123456"));
        assertEquals(VerificationResult.NOT_FOUND, store().verifyAndConsume(key, "123456", now.plusSeconds(1)));
    }

    @Test
    void isolatesBusinessNamespaces() {
        String subject = UUID.randomUUID() + "@example.com";
        VerificationKey account = new VerificationKey("account", "login", subject);
        VerificationKey payment = new VerificationKey("payment", "login", subject);
        Instant now = Instant.now();
        store().store(account, "123456", POLICY, now);
        store().store(payment, "654321", POLICY, now);

        assertEquals(VerificationResult.SUCCESS, store().verifyAndConsume(account, "123456", now.plusSeconds(1)));
        assertEquals(VerificationResult.SUCCESS, store().verifyAndConsume(payment, "654321", now.plusSeconds(1)));
    }

    @Test
    void permitsOnlyOneConcurrentSuccessfulConsumption() throws Exception {
        VerificationKey key = key("account");
        Instant now = Instant.now();
        store().store(key, "123456", POLICY, now);
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(threads)) {
            @SuppressWarnings("unchecked")
            Future<VerificationResult>[] futures = new Future[threads];
            for (int index = 0; index < threads; index++) {
                futures[index] = executor.submit(() -> {
                    start.await();
                    return store().verifyAndConsume(key, "123456", now.plusSeconds(1));
                });
            }
            start.countDown();

            int successes = 0;
            for (Future<VerificationResult> future : futures) {
                if (future.get() == VerificationResult.SUCCESS) {
                    successes++;
                }
            }
            assertEquals(1, successes);
        }
    }

    private VerificationKey key(String namespace) {
        return new VerificationKey(namespace, "login", UUID.randomUUID() + "@example.com");
    }
}
