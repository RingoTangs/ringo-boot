package io.github.ringotangs.ringoboot.verification.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreKey;
import io.github.ringotangs.ringoboot.verification.store.VerifyResult;
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
        VerificationStoreKey key = key("account");
        Instant now = now();

        Instant stored = store().store(key, "123456", POLICY, now);
        Instant overwritten = store().store(key, "654321", POLICY, now.plusSeconds(10));

        assertEquals(now.plus(POLICY.ttl()), stored);
        assertEquals(now.plusSeconds(10).plus(POLICY.ttl()), overwritten);
        assertEquals(VerifyResult.MISMATCH, store().verifyAndConsume(key, "123456", now.plusSeconds(11)));
        assertEquals(VerifyResult.SUCCESS, store().verifyAndConsume(key, "654321", now.plusSeconds(12)));
    }

    @Test
    void decrementsAttemptsAndConsumesExhaustedCode() {
        VerificationStoreKey key = key("account");
        Instant now = now();
        store().store(key, "123456", POLICY, now);

        assertEquals(VerifyResult.MISMATCH, store().verifyAndConsume(key, "000000", now.plusSeconds(1)));
        assertEquals(VerifyResult.ATTEMPTS_EXHAUSTED, store().verifyAndConsume(key, "000000", now.plusSeconds(2)));
        assertEquals(VerifyResult.NOT_FOUND, store().verifyAndConsume(key, "123456", now.plusSeconds(3)));
    }

    @Test
    void reportsExpiredThenConsumesRecord() {
        VerificationStoreKey key = key("account");
        Instant now = now();
        store().store(key, "123456", POLICY, now);

        assertEquals(VerifyResult.EXPIRED, store().verifyAndConsume(key, "123456", now.plus(POLICY.ttl())));
        assertEquals(
                VerifyResult.NOT_FOUND,
                store().verifyAndConsume(key, "123456", now.plus(POLICY.ttl()).plusSeconds(1)));
    }

    @Test
    void invalidatesOnlyMatchingCode() {
        VerificationStoreKey key = key("account");
        Instant now = now();
        store().store(key, "123456", POLICY, now);

        assertFalse(store().invalidate(key, "000000"));
        assertTrue(store().invalidate(key, "123456"));
        assertEquals(VerifyResult.NOT_FOUND, store().verifyAndConsume(key, "123456", now.plusSeconds(1)));
    }

    @Test
    void isolatesBusinessNamespaces() {
        String subject = UUID.randomUUID() + "@example.com";
        VerificationStoreKey account =
                new VerificationStoreKey(new VerificationKey("account", "login", subject), VerificationChannel.EMAIL);
        VerificationStoreKey payment =
                new VerificationStoreKey(new VerificationKey("payment", "login", subject), VerificationChannel.EMAIL);
        Instant now = now();
        store().store(account, "123456", POLICY, now);
        store().store(payment, "654321", POLICY, now);

        assertEquals(VerifyResult.SUCCESS, store().verifyAndConsume(account, "123456", now.plusSeconds(1)));
        assertEquals(VerifyResult.SUCCESS, store().verifyAndConsume(payment, "654321", now.plusSeconds(1)));
    }

    @Test
    void isolatesVerificationChannels() {
        VerificationKey businessKey = new VerificationKey("account", "login", UUID.randomUUID() + "@example.com");
        VerificationStoreKey email = new VerificationStoreKey(businessKey, VerificationChannel.EMAIL);
        VerificationStoreKey sms = new VerificationStoreKey(businessKey, VerificationChannel.SMS);
        Instant now = now();
        store().store(email, "123456", POLICY, now);
        store().store(sms, "654321", POLICY, now);

        assertEquals(VerifyResult.MISMATCH, store().verifyAndConsume(email, "654321", now.plusSeconds(1)));
        assertEquals(VerifyResult.SUCCESS, store().verifyAndConsume(email, "123456", now.plusSeconds(2)));
        assertEquals(VerifyResult.SUCCESS, store().verifyAndConsume(sms, "654321", now.plusSeconds(2)));
    }

    @Test
    void permitsOnlyOneConcurrentSuccessfulConsumption() throws Exception {
        VerificationStoreKey key = key("account");
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

    private VerificationStoreKey key(String namespace) {
        return new VerificationStoreKey(
                new VerificationKey(namespace, "login", UUID.randomUUID() + "@example.com"), VerificationChannel.EMAIL);
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
