package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DefaultVerificationServiceTest {

    private static final VerificationKey LOGIN = new VerificationKey("login", "user@example.com");
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private final MutableClock clock = new MutableClock(START);
    private final InMemoryVerificationStore store = new InMemoryVerificationStore(new SecureRandom());

    @Test
    void issuesCodeWithDefaultPolicyAndRedactsToString() {
        VerificationService service = service(length -> "123456");

        IssueResult.Issued issued = assertInstanceOf(IssueResult.Issued.class, service.issue(LOGIN));

        assertEquals("123456", issued.code());
        assertEquals(START.plus(Duration.ofMinutes(5)), issued.expiresAt());
        assertTrue(!issued.toString().contains("123456"));
    }

    @Test
    void throttlesReissueUntilIntervalElapsesAndThenReplacesCode() {
        AtomicInteger sequence = new AtomicInteger(111110);
        VerificationService service = service(length -> Integer.toString(sequence.incrementAndGet()));

        IssueResult.Issued first = assertInstanceOf(IssueResult.Issued.class, service.issue(LOGIN));
        IssueResult.Throttled throttled = assertInstanceOf(IssueResult.Throttled.class, service.issue(LOGIN));
        assertEquals(Duration.ofSeconds(60), throttled.retryAfter());

        clock.advance(Duration.ofSeconds(60));
        IssueResult.Issued second = assertInstanceOf(IssueResult.Issued.class, service.issue(LOGIN));

        assertEquals(VerificationResult.MISMATCH, service.verify(LOGIN, first.code()));
        assertEquals(VerificationResult.SUCCESS, service.verify(LOGIN, second.code()));
    }

    @Test
    void invalidatingOldCodeDoesNotRemoveNewCode() {
        AtomicInteger sequence = new AtomicInteger(111110);
        VerificationService service = service(length -> Integer.toString(sequence.incrementAndGet()));
        VerificationPolicy policy = new VerificationPolicy(6, Duration.ofMinutes(5), 5, Duration.ZERO);
        IssueResult.Issued first = assertInstanceOf(IssueResult.Issued.class, service.issue(LOGIN, policy));
        IssueResult.Issued second = assertInstanceOf(IssueResult.Issued.class, service.issue(LOGIN, policy));

        assertTrue(!service.invalidate(LOGIN, first.code()));
        assertEquals(VerificationResult.SUCCESS, service.verify(LOGIN, second.code()));
    }

    @Test
    void expiresAndRemovesCodeAtExpirationBoundary() {
        VerificationService service = service(length -> "123456");
        service.issue(LOGIN);

        clock.advance(Duration.ofMinutes(5));

        assertEquals(VerificationResult.EXPIRED, service.verify(LOGIN, "123456"));
        assertEquals(VerificationResult.NOT_FOUND, service.verify(LOGIN, "123456"));
    }

    @Test
    void limitsAttemptsAndRemovesExhaustedCode() {
        VerificationService service = service(length -> "123456");
        VerificationPolicy policy = new VerificationPolicy(6, Duration.ofMinutes(5), 2, Duration.ZERO);
        service.issue(LOGIN, policy);

        assertEquals(VerificationResult.MISMATCH, service.verify(LOGIN, ""));
        assertEquals(VerificationResult.ATTEMPTS_EXHAUSTED, service.verify(LOGIN, "000000"));
        assertEquals(VerificationResult.NOT_FOUND, service.verify(LOGIN, "123456"));
    }

    @Test
    void isolatesPurposeAndSubject() {
        VerificationService service = service(length -> "123456");
        VerificationKey registration = new VerificationKey("register", LOGIN.subject());
        VerificationKey otherUser = new VerificationKey(LOGIN.purpose(), "other@example.com");
        service.issue(LOGIN);
        service.issue(registration);
        service.issue(otherUser);

        assertEquals(VerificationResult.SUCCESS, service.verify(LOGIN, "123456"));
        assertEquals(VerificationResult.SUCCESS, service.verify(registration, "123456"));
        assertEquals(VerificationResult.SUCCESS, service.verify(otherUser, "123456"));
    }

    @Test
    void permitsOnlyOneConcurrentSuccess() throws Exception {
        VerificationService service = service(length -> "123456");
        service.issue(LOGIN);
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(threads)) {
            @SuppressWarnings("unchecked")
            Future<VerificationResult>[] futures = new Future[threads];
            for (int index = 0; index < threads; index++) {
                futures[index] = executor.submit(() -> {
                    start.await();
                    return service.verify(LOGIN, "123456");
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

    @Test
    void doesNotKeepPlaintextCodeInMemoryEntries() throws Exception {
        VerificationService service = service(length -> "987654");
        service.issue(LOGIN);

        Field entriesField = InMemoryVerificationStore.class.getDeclaredField("entries");
        entriesField.setAccessible(true);
        Object entries = entriesField.get(store);

        assertInstanceOf(Map.class, entries);
        assertTrue(!entries.toString().contains("987654"));
    }

    @Test
    void rejectsNullInputsAndGeneratedCode() {
        VerificationService service = service(length -> "123456");
        assertThrows(NullPointerException.class, () -> service.issue(null));
        assertThrows(NullPointerException.class, () -> service.issue(LOGIN, null));
        assertThrows(NullPointerException.class, () -> service.verify(LOGIN, null));
        assertThrows(NullPointerException.class, () -> service(null).issue(LOGIN));
    }

    @Test
    void rejectsInvalidGeneratedCode() {
        assertThrows(IllegalStateException.class, () -> service(length -> "").issue(LOGIN));
        assertThrows(IllegalStateException.class, () -> service(length -> "123").issue(LOGIN));
    }

    private VerificationService service(CodeGenerator generator) {
        return new DefaultVerificationService(generator, store, VerificationPolicy.defaults(), clock);
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
