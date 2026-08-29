package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitViolation;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitManager;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.limit.MissingIssueRateLimitRuleException;
import io.github.ringotangs.ringoboot.verification.limit.TestIssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.store.StoreResult;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class AbstractVerificationServiceTest {

    private static final VerificationKey LOGIN = new VerificationKey("account", "login", "user@example.com");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void issuesDispatchesAndVerifiesThroughServiceWithoutExposingCodeInResult() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());

        IssueResult.Accepted issued = assertInstanceOf(IssueResult.Accepted.class, template.issue(LOGIN));

        assertEquals(NOW.plus(Duration.ofMinutes(5)), issued.expiresAt());
        assertEquals(LOGIN, template.delivery().context().key());
        assertEquals("123456", template.delivery().code());
        assertFalse(issued.toString().contains("123456"));
        assertEquals(VerifyResult.SUCCESS, template.verify(LOGIN, "123456"));
    }

    @Test
    void doesNotDispatchWhenIssuanceIsThrottled() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());

        template.issue(LOGIN);
        IssueResult.Throttled throttled = assertInstanceOf(IssueResult.Throttled.class, template.issue(LOGIN));

        assertEquals(1, template.dispatches());
        assertEquals(Duration.ofSeconds(60), throttled.retryAfter());
        assertEquals(
                List.of(new IssueLimitViolation("test-key-cooldown", Duration.ofSeconds(60))), throttled.violations());
    }

    @Test
    void convenienceConstructorUsesPermitAllLimiter() {
        CapturingVerificationService template =
                new CapturingVerificationService(length -> "123456", new InMemoryVerificationStore());

        assertInstanceOf(IssueResult.Accepted.class, template.issue(LOGIN));
        assertInstanceOf(IssueResult.Accepted.class, template.issue(LOGIN));

        assertEquals(2, template.dispatches());
    }

    @Test
    void usesServiceVerificationPolicy() {
        AtomicInteger requestedLength = new AtomicInteger();
        CapturingVerificationService template = template(
                length -> {
                    requestedLength.set(length);
                    return "1234";
                },
                new InMemoryVerificationStore(),
                new VerificationPolicy(4, Duration.ofMinutes(1), 2));

        IssueResult.Accepted issued = assertInstanceOf(IssueResult.Accepted.class, template.issue(LOGIN));

        assertEquals(4, requestedLength.get());
        assertEquals(NOW.plus(Duration.ofMinutes(1)), issued.expiresAt());
    }

    @Test
    void invalidatesCodeWhenDispatchFailsButKeepsIssueLimit() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());
        CodeSenderException failure = new CodeSenderException(VerificationChannel.EMAIL, "provider unavailable");
        template.failWith(failure);

        CodeSenderException thrown = assertThrows(CodeSenderException.class, () -> template.issue(LOGIN));
        template.failWith(null);
        IssueResult result = template.issue(LOGIN);

        assertSame(failure, thrown);
        assertInstanceOf(IssueResult.Throttled.class, result);
    }

    @Test
    void invalidatesRejectedCodeButKeepsIssueLimit() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());
        template.respondWith(CodeSendResult.REJECTED);

        CodeDeliveryRejectedException rejected =
                assertThrows(CodeDeliveryRejectedException.class, () -> template.issue(LOGIN));
        assertEquals(VerificationChannel.EMAIL, rejected.channel());
        template.respondWith(CodeSendResult.ACCEPTED);

        assertInstanceOf(IssueResult.Throttled.class, template.issue(LOGIN));
    }

    @Test
    void keepsCodeWhenDeliveryOutcomeIsUnknown() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());
        template.respondWith(CodeSendResult.UNKNOWN);

        assertInstanceOf(IssueResult.Uncertain.class, template.issue(LOGIN));
        assertInstanceOf(IssueResult.Throttled.class, template.issue(LOGIN));
        assertEquals(VerifyResult.SUCCESS, template.verify(LOGIN, "123456"));
    }

    @Test
    void preservesDispatchFailureAndSuppressesInvalidationFailure() {
        VerificationStoreException invalidationFailure = new VerificationStoreException("cleanup unavailable");
        VerificationStore store = new StubVerificationStore() {
            @Override
            public boolean invalidate(VerificationKey key, String code) {
                throw invalidationFailure;
            }
        };
        CapturingVerificationService template = template(length -> "123456", store);
        CodeSenderException dispatchFailure =
                new CodeSenderException(VerificationChannel.EMAIL, "delivery unavailable");
        template.failWith(dispatchFailure);

        CodeSenderException thrown = assertThrows(CodeSenderException.class, () -> template.issue(LOGIN));

        assertSame(dispatchFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(invalidationFailure, thrown.getSuppressed()[0]);
    }

    @Test
    void propagatesStoreFailureDuringIssuance() {
        VerificationStoreException failure = new VerificationStoreException("storage unavailable");
        VerificationStore store = new StubVerificationStore() {
            @Override
            public StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt) {
                throw failure;
            }
        };
        CapturingVerificationService template = template(length -> "123456", store);

        VerificationStoreException thrown = assertThrows(VerificationStoreException.class, () -> template.issue(LOGIN));

        assertSame(failure, thrown);
        assertEquals(0, template.dispatches());
    }

    @Test
    void propagatesStoreFailureDuringVerification() {
        VerificationStoreException failure = new VerificationStoreException("storage unavailable");
        VerificationStore store = new StubVerificationStore() {
            @Override
            public VerifyResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt) {
                throw failure;
            }
        };
        CapturingVerificationService template = template(length -> "123456", store);

        VerificationStoreException thrown =
                assertThrows(VerificationStoreException.class, () -> template.verify(LOGIN, "123456"));

        assertSame(failure, thrown);
    }

    @Test
    void passesSameCustomizedContextToRateLimiterAndDelivery() {
        AtomicReference<IssueContext> captured = new AtomicReference<>();
        IssueRateLimiter limiter = (context, requestedAt) -> {
            captured.set(context);
            return new IssueLimitResult.Allowed();
        };
        CapturingVerificationService template = new CapturingVerificationService(
                length -> "123456",
                new InMemoryVerificationStore(),
                limiter,
                VerificationPolicy.defaults(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        template.customizeWith(
                context -> context.with("ip-address", "203.0.113.10").with("service", "capturing"));
        assertInstanceOf(IssueResult.Accepted.class, template.issue(LOGIN));

        assertSame(captured.get(), template.delivery().context());
        assertEquals(LOGIN, captured.get().key());
        assertEquals(VerificationChannel.EMAIL, captured.get().channel());
        assertEquals("203.0.113.10", captured.get().attribute("ip-address").orElseThrow());
        assertEquals("capturing", captured.get().attribute("service").orElseThrow());
        assertEquals(1, template.customizations());
    }

    @Test
    void rejectsInvalidCustomizerResults() {
        VerificationKey otherKey = new VerificationKey("account", "login", "other@example.com");
        CapturingVerificationService changedKey = template(length -> "123456", new InMemoryVerificationStore());
        changedKey.customizeWith(context -> IssueContext.of(otherKey, context.channel(), context.policy()));
        CapturingVerificationService changedChannel = template(length -> "123456", new InMemoryVerificationStore());
        changedChannel.customizeWith(
                context -> IssueContext.of(context.key(), VerificationChannel.SMS, context.policy()));
        CapturingVerificationService changedPolicy = template(length -> "123456", new InMemoryVerificationStore());
        changedPolicy.customizeWith(context ->
                IssueContext.of(context.key(), context.channel(), new VerificationPolicy(4, Duration.ofMinutes(1), 2)));
        CapturingVerificationService nullCustomizedContext =
                template(length -> "123456", new InMemoryVerificationStore());
        nullCustomizedContext.customizeWith(context -> null);

        assertEquals(
                "issue context customizer must preserve the verification key",
                assertThrows(IllegalArgumentException.class, () -> changedKey.issue(LOGIN))
                        .getMessage());
        assertEquals(
                "issue context customizer must preserve the verification channel",
                assertThrows(IllegalArgumentException.class, () -> changedChannel.issue(LOGIN))
                        .getMessage());
        assertEquals(
                "issue context customizer must preserve the verification policy",
                assertThrows(IllegalArgumentException.class, () -> changedPolicy.issue(LOGIN))
                        .getMessage());
        assertEquals(
                "customized issue context must not be null",
                assertThrows(NullPointerException.class, () -> nullCustomizedContext.issue(LOGIN))
                        .getMessage());
    }

    @Test
    void doesNotGenerateStoreOrDispatchWhenNoRateLimitRuleMatches() {
        AtomicInteger generations = new AtomicInteger();
        AtomicInteger stores = new AtomicInteger();
        VerificationStore store = new StubVerificationStore() {
            @Override
            public StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt) {
                stores.incrementAndGet();
                return super.store(key, code, policy, issuedAt);
            }
        };
        IssueRateLimiter limiter = (context, requestedAt) -> {
            throw new MissingIssueRateLimitRuleException(context.key());
        };
        CapturingVerificationService template = new CapturingVerificationService(
                length -> {
                    generations.incrementAndGet();
                    return "123456";
                },
                store,
                limiter,
                VerificationPolicy.defaults(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(MissingIssueRateLimitRuleException.class, () -> template.issue(LOGIN));
        assertEquals(0, generations.get());
        assertEquals(0, stores.get());
        assertEquals(0, template.dispatches());
    }

    @Test
    void validatesRequiredServiceArgumentsBeforeIssuance() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());

        assertThrows(
                NullPointerException.class,
                () -> new CapturingVerificationService(
                        null,
                        new InMemoryVerificationStore(),
                        VerificationPolicy.defaults(),
                        Clock.fixed(NOW, ZoneOffset.UTC)));
        NullPointerException nullPolicy = assertThrows(
                NullPointerException.class,
                () -> new CapturingVerificationService(
                        length -> "123456", new InMemoryVerificationStore(), null, Clock.fixed(NOW, ZoneOffset.UTC)));
        assertEquals("verificationPolicy must not be null", nullPolicy.getMessage());
        assertThrows(
                NullPointerException.class,
                () -> new CapturingVerificationService(
                        length -> "123456",
                        new InMemoryVerificationStore(),
                        null,
                        VerificationPolicy.defaults(),
                        Clock.fixed(NOW, ZoneOffset.UTC)));
        assertThrows(
                NullPointerException.class,
                () -> new CapturingVerificationService(
                        length -> "123456",
                        new InMemoryVerificationStore(),
                        IssueRateLimiter.permitAll(),
                        VerificationPolicy.defaults(),
                        null));
        assertThrows(NullPointerException.class, () -> template.issue((VerificationKey) null));
    }

    @Test
    void declaresDefaultAndClockAwareDependencyConstructors() {
        var constructors = AbstractVerificationService.class.getDeclaredConstructors();

        assertEquals(2, constructors.length);
        assertDoesNotThrow(() -> AbstractVerificationService.class.getDeclaredConstructor(
                CodeGenerator.class, VerificationStore.class, IssueRateLimiter.class, VerificationPolicy.class));
        assertDoesNotThrow(() -> AbstractVerificationService.class.getDeclaredConstructor(
                CodeGenerator.class,
                VerificationStore.class,
                IssueRateLimiter.class,
                VerificationPolicy.class,
                Clock.class));
    }

    private CapturingVerificationService template(CodeGenerator generator, VerificationStore store) {
        return template(generator, store, VerificationPolicy.defaults());
    }

    private CapturingVerificationService template(
            CodeGenerator generator, VerificationStore store, VerificationPolicy verificationPolicy) {
        return new CapturingVerificationService(
                generator, store, testIssueRateLimiter(), verificationPolicy, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private IssueRateLimiter testIssueRateLimiter() {
        IssueRateLimitRule rule = new TestIssueRateLimitRule(
                "test-key-cooldown",
                context -> IssueLimitBucket.of(
                        context.key().namespace(),
                        context.key().purpose(),
                        context.key().subject()),
                1,
                Duration.ofSeconds(60));
        return new IssueRateLimitManager(List.of(rule), new InMemoryIssueRateLimitStore());
    }

    private static final class CapturingVerificationService extends AbstractVerificationService {

        private final AtomicReference<TestDispatch> delivery = new AtomicReference<>();
        private final AtomicInteger dispatches = new AtomicInteger();
        private final AtomicInteger customizations = new AtomicInteger();
        private UnaryOperator<IssueContext> customizer = context -> context.with("service", "capturing");
        private RuntimeException failure;
        private CodeSendResult result = CodeSendResult.ACCEPTED;

        private CapturingVerificationService(CodeGenerator generator, VerificationStore store) {
            super(generator, store, IssueRateLimiter.permitAll(), VerificationPolicy.defaults());
        }

        private CapturingVerificationService(
                CodeGenerator generator, VerificationStore store, VerificationPolicy verificationPolicy, Clock clock) {
            this(generator, store, IssueRateLimiter.permitAll(), verificationPolicy, clock);
        }

        private CapturingVerificationService(
                CodeGenerator generator,
                VerificationStore store,
                IssueRateLimiter issueRateLimiter,
                VerificationPolicy verificationPolicy,
                Clock clock) {
            super(generator, store, issueRateLimiter, verificationPolicy, clock);
        }

        @Override
        protected CodeSendResult dispatch(IssueContext context, String code, Instant expiresAt) {
            dispatches.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
            delivery.set(new TestDispatch(context, code, expiresAt));
            return result;
        }

        @Override
        protected VerificationChannel channel() {
            return VerificationChannel.EMAIL;
        }

        @Override
        protected IssueContext customizeIssueContext(IssueContext context) {
            customizations.incrementAndGet();
            return customizer.apply(context);
        }

        private TestDispatch delivery() {
            return delivery.get();
        }

        private int dispatches() {
            return dispatches.get();
        }

        private int customizations() {
            return customizations.get();
        }

        private void customizeWith(UnaryOperator<IssueContext> customizer) {
            this.customizer = customizer;
        }

        private void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        private void respondWith(CodeSendResult result) {
            this.result = result;
        }

        private record TestDispatch(IssueContext context, String code, Instant expiresAt) {}
    }

    private static class StubVerificationStore implements VerificationStore {

        @Override
        public StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt) {
            return new StoreResult(NOW.plusSeconds(60));
        }

        @Override
        public VerifyResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt) {
            return VerifyResult.NOT_FOUND;
        }

        @Override
        public boolean invalidate(VerificationKey key, String code) {
            return true;
        }
    }
}
