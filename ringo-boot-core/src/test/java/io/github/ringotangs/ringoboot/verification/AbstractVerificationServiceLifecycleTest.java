package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.sender.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
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

class AbstractVerificationServiceLifecycleTest {

    private static final VerificationKey LOGIN = new VerificationKey("account", "login", "user@example.com");
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private final MutableClock clock = new MutableClock(START);
    private final InMemoryVerificationStore store = new InMemoryVerificationStore(new SecureRandom());

    @Test
    void issuesCodeWithDefaultPolicyAndRedactsToString() {
        TestVerificationService service = service(length -> "123456");

        IssueResult.Accepted issued = assertInstanceOf(IssueResult.Accepted.class, service.issue(LOGIN));

        assertEquals(START.plus(Duration.ofMinutes(5)), issued.expiresAt());
        assertEquals("123456", service.lastCode());
        assertTrue(!issued.toString().contains(service.lastCode()));
    }

    @Test
    void throttlesReissueUntilIntervalElapsesAndThenReplacesCode() {
        AtomicInteger sequence = new AtomicInteger(111110);
        TestVerificationService service = service(length -> Integer.toString(sequence.incrementAndGet()));

        assertInstanceOf(IssueResult.Accepted.class, service.issue(LOGIN));
        String firstCode = service.lastCode();
        IssueResult.Throttled throttled = assertInstanceOf(IssueResult.Throttled.class, service.issue(LOGIN));
        assertEquals(Duration.ofSeconds(60), throttled.retryAfter());

        clock.advance(Duration.ofSeconds(60));
        assertInstanceOf(IssueResult.Accepted.class, service.issue(LOGIN));
        String secondCode = service.lastCode();

        assertEquals(VerificationResult.MISMATCH, service.verify(LOGIN, firstCode));
        assertEquals(VerificationResult.SUCCESS, service.verify(LOGIN, secondCode));
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
        VerificationPolicy policy = new VerificationPolicy(6, Duration.ofMinutes(5), 2);
        service.issue(LOGIN, policy);

        assertEquals(VerificationResult.MISMATCH, service.verify(LOGIN, ""));
        assertEquals(VerificationResult.ATTEMPTS_EXHAUSTED, service.verify(LOGIN, "000000"));
        assertEquals(VerificationResult.NOT_FOUND, service.verify(LOGIN, "123456"));
    }

    @Test
    void isolatesPurposeAndSubject() {
        VerificationService service = service(length -> "123456");
        VerificationKey paymentLogin = new VerificationKey("payment", LOGIN.purpose(), LOGIN.subject());
        VerificationKey registration = new VerificationKey(LOGIN.namespace(), "register", LOGIN.subject());
        VerificationKey otherUser = new VerificationKey(LOGIN.namespace(), LOGIN.purpose(), "other@example.com");
        service.issue(LOGIN);
        service.issue(paymentLogin);
        service.issue(registration);
        service.issue(otherUser);

        assertEquals(VerificationResult.SUCCESS, service.verify(LOGIN, "123456"));
        assertEquals(VerificationResult.SUCCESS, service.verify(paymentLogin, "123456"));
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
        assertThrows(
                CodeGenerationException.class, () -> service(length -> null).issue(LOGIN));
        assertThrows(CodeGenerationException.class, () -> service(length -> "").issue(LOGIN));
        assertThrows(
                CodeGenerationException.class, () -> service(length -> "123").issue(LOGIN));
    }

    @Test
    void propagatesCodeGenerationException() {
        CodeGenerationException failure = new CodeGenerationException("random source unavailable");

        CodeGenerationException thrown = assertThrows(
                CodeGenerationException.class,
                () -> service(length -> {
                            throw failure;
                        })
                        .issue(LOGIN));

        assertTrue(thrown == failure);
    }

    private TestVerificationService service(CodeGenerator generator) {
        return new TestVerificationService(generator, store, VerificationPolicy.defaults(), clock);
    }

    private static final class TestVerificationService extends AbstractVerificationService {

        private String lastCode;

        private TestVerificationService(
                CodeGenerator generator, VerificationStore store, VerificationPolicy policy, Clock clock) {
            super(generator, store, policy, clock);
        }

        @Override
        protected CodeSendResult dispatch(CodeDelivery delivery) {
            lastCode = delivery.code();
            return CodeSendResult.ACCEPTED;
        }

        private String lastCode() {
            return lastCode;
        }
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
