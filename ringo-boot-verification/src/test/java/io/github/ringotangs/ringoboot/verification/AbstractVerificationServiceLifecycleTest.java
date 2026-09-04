package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.channel.DeliveryResult;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.context.CompositeIssueContextManager;
import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitExceededException;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimiter;
import io.github.ringotangs.ringoboot.verification.limit.RuleBasedIssueLimiter;
import io.github.ringotangs.ringoboot.verification.limit.TestIssueLimitRule;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AbstractVerificationServiceLifecycleTest {

    private static final VerificationKey LOGIN = new VerificationKey("account", "login", "user@example.com");
    private final InMemoryVerificationStore store = new InMemoryVerificationStore(new SecureRandom());

    @Test
    void issuesCodeWithDefaultPolicyAndRedactsToString() {
        TestVerificationService service = service(length -> "123456");
        Instant earliestExpiration = Instant.now().plus(Duration.ofMinutes(5));

        DeliveryResult.Accepted issued = assertInstanceOf(DeliveryResult.Accepted.class, service.issue(LOGIN));

        assertTrue(!issued.expiresAt().isBefore(earliestExpiration));
        assertEquals("123456", service.lastCode());
        assertTrue(!issued.toString().contains(service.lastCode()));
    }

    @Test
    void throttlesImmediateReissue() {
        AtomicInteger sequence = new AtomicInteger(111110);
        TestVerificationService service = service(length -> Integer.toString(sequence.incrementAndGet()));

        assertInstanceOf(DeliveryResult.Accepted.class, service.issue(LOGIN));
        IssueLimitExceededException throttled =
                assertThrows(IssueLimitExceededException.class, () -> service.issue(LOGIN));
        assertTrue(!throttled.retryAfter().isNegative());
        assertTrue(throttled.retryAfter().compareTo(Duration.ofSeconds(60)) <= 0);
        assertEquals("test-key-cooldown", throttled.violations().getFirst().ruleId());
    }

    @Test
    void limitsAttemptsAndRemovesExhaustedCode() {
        VerificationPolicy policy = new VerificationPolicy(6, Duration.ofMinutes(5), 2);
        VerificationService<DeliveryResult> service = service(length -> "123456", policy);
        service.issue(LOGIN);

        assertThrows(InvalidVerificationCodeException.class, () -> service.verify(LOGIN, ""));
        assertThrows(InvalidVerificationCodeException.class, () -> service.verify(LOGIN, "000000"));
        assertThrows(InvalidVerificationCodeException.class, () -> service.verify(LOGIN, "123456"));
    }

    @Test
    void isolatesPurposeAndSubject() {
        VerificationService<DeliveryResult> service = service(length -> "123456");
        VerificationKey paymentLogin = new VerificationKey("payment", LOGIN.purpose(), LOGIN.subject());
        VerificationKey registration = new VerificationKey(LOGIN.namespace(), "register", LOGIN.subject());
        VerificationKey otherUser = new VerificationKey(LOGIN.namespace(), LOGIN.purpose(), "other@example.com");
        service.issue(LOGIN);
        service.issue(paymentLogin);
        service.issue(registration);
        service.issue(otherUser);

        service.verify(LOGIN, "123456");
        service.verify(paymentLogin, "123456");
        service.verify(registration, "123456");
        service.verify(otherUser, "123456");
    }

    @Test
    void permitsOnlyOneConcurrentSuccess() throws Exception {
        VerificationService<DeliveryResult> service = service(length -> "123456");
        service.issue(LOGIN);
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(threads)) {
            @SuppressWarnings("unchecked")
            Future<Boolean>[] futures = new Future[threads];
            for (int index = 0; index < threads; index++) {
                futures[index] = executor.submit(() -> {
                    start.await();
                    try {
                        service.verify(LOGIN, "123456");
                        return true;
                    } catch (InvalidVerificationCodeException ignored) {
                        return false;
                    }
                });
            }
            start.countDown();

            int successes = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    successes++;
                }
            }
            assertEquals(1, successes);
        }
    }

    @Test
    void doesNotKeepPlaintextCodeInMemoryEntries() throws Exception {
        VerificationService<DeliveryResult> service = service(length -> "987654");
        service.issue(LOGIN);

        Field entriesField = InMemoryVerificationStore.class.getDeclaredField("entries");
        entriesField.setAccessible(true);
        Object entries = entriesField.get(store);

        assertInstanceOf(Map.class, entries);
        assertTrue(!entries.toString().contains("987654"));
    }

    @Test
    void rejectsNullInputsAndGeneratedCode() {
        VerificationService<DeliveryResult> service = service(length -> "123456");
        assertThrows(NullPointerException.class, () -> service.issue((VerificationKey) null));
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
        return service(generator, VerificationPolicy.defaults());
    }

    private TestVerificationService service(CodeGenerator generator, VerificationPolicy verificationPolicy) {
        return new TestVerificationService(generator, store, testIssueLimiter(), verificationPolicy);
    }

    private IssueLimiter testIssueLimiter() {
        IssueLimitRule rule = new TestIssueLimitRule(
                "test-key-cooldown",
                context -> IssueLimitBucket.of(
                        context.key().namespace(),
                        context.key().purpose(),
                        context.key().subject()),
                1,
                Duration.ofSeconds(60));
        return new RuleBasedIssueLimiter(List.of(rule), new InMemoryIssueLimitStore());
    }

    private static final class TestVerificationService extends AbstractVerificationService<DeliveryResult> {

        private String lastCode;

        private TestVerificationService(
                CodeGenerator generator,
                VerificationStore store,
                IssueLimiter issueLimiter,
                VerificationPolicy verificationPolicy) {
            super(generator, store, issueLimiter, verificationPolicy, new CompositeIssueContextManager(List.of()));
        }

        @Override
        protected DeliveryResult completeIssue(IssueContext context, String code, Instant expiresAt) {
            lastCode = code;
            return new DeliveryResult.Accepted(expiresAt);
        }

        @Override
        protected VerificationChannel channel() {
            return VerificationChannel.EMAIL;
        }

        private String lastCode() {
            return lastCode;
        }
    }
}
