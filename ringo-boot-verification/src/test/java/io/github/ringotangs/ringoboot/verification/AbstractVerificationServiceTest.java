package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.channel.CodeSendRejectedException;
import io.github.ringotangs.ringoboot.verification.channel.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.channel.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.channel.DeliveryResult;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.context.CompositeIssueContextManager;
import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import io.github.ringotangs.ringoboot.verification.context.IssueContextContributor;
import io.github.ringotangs.ringoboot.verification.context.IssueContextManager;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitExceededException;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimiter;
import io.github.ringotangs.ringoboot.verification.limit.MissingIssueLimitRuleException;
import io.github.ringotangs.ringoboot.verification.limit.RuleBasedIssueLimiter;
import io.github.ringotangs.ringoboot.verification.limit.TestIssueLimitRule;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AbstractVerificationServiceTest {

    private static final VerificationKey LOGIN = new VerificationKey("account", "login", "user@example.com");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void channelSendersPreserveUnknownAndRevokeFailures() {
        for (VerificationChannel channel : List.of(VerificationChannel.EMAIL, VerificationChannel.SMS)) {
            AtomicReference<CodeSendResult> outcome = new AtomicReference<>(CodeSendResult.UNKNOWN);
            CodeSenderException failure = new CodeSenderException(channel, "delivery failed");
            java.util.function.Supplier<CodeSendResult> send = () -> {
                if (outcome.get() == null) {
                    throw failure;
                }
                return outcome.get();
            };
            VerificationService<DeliveryResult> service = channel.equals(VerificationChannel.EMAIL)
                    ? new io.github.ringotangs.ringoboot.verification.channel.email.EmailVerificationService(
                            length -> "123456",
                            new InMemoryVerificationStore(),
                            IssueLimiter.permitAll(),
                            VerificationPolicy.defaults(),
                            context -> context,
                            (context, code, expiresAt) -> send.get())
                    : new io.github.ringotangs.ringoboot.verification.channel.sms.SmsVerificationService(
                            length -> "123456",
                            new InMemoryVerificationStore(),
                            IssueLimiter.permitAll(),
                            VerificationPolicy.defaults(),
                            context -> context,
                            (context, code, expiresAt) -> send.get());

            assertInstanceOf(DeliveryResult.Uncertain.class, service.issue(LOGIN));
            service.verify(LOGIN, "123456");

            outcome.set(CodeSendResult.REJECTED);
            assertThrows(CodeSendRejectedException.class, () -> service.issue(LOGIN));
            assertEquals(
                    VerifyResult.NOT_FOUND,
                    assertThrows(VerificationRejectedException.class, () -> service.verify(LOGIN, "123456"))
                            .result());

            outcome.set(null);
            assertSame(failure, assertThrows(CodeSenderException.class, () -> service.issue(LOGIN)));
            assertEquals(
                    VerifyResult.NOT_FOUND,
                    assertThrows(VerificationRejectedException.class, () -> service.verify(LOGIN, "123456"))
                            .result());
        }
    }

    @Test
    void timestampsStorageAfterCodeGeneration() {
        AtomicReference<Instant> generatedAt = new AtomicReference<>();
        VerificationStore store = new StubVerificationStore() {
            @Override
            public Instant store(VerificationStoreKey key, String code, VerificationPolicy policy, Instant issuedAt) {
                assertFalse(issuedAt.isBefore(generatedAt.get()));
                return issuedAt.plus(policy.ttl());
            }
        };
        template(
                        length -> {
                            generatedAt.set(Instant.now());
                            return "123456";
                        },
                        store)
                .issue(LOGIN);
    }

    @Test
    void issuesDispatchesAndVerifiesThroughServiceWithoutExposingCodeInResult() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());
        Instant earliestExpiration = Instant.now().plus(Duration.ofMinutes(5));

        DeliveryResult.Accepted issued = assertInstanceOf(DeliveryResult.Accepted.class, template.issue(LOGIN));

        assertFalse(issued.expiresAt().isBefore(earliestExpiration));
        assertEquals(LOGIN, template.delivery().context().key());
        assertEquals("123456", template.delivery().code());
        assertFalse(issued.toString().contains("123456"));
        template.verify(LOGIN, "123456");
    }

    @Test
    void doesNotDispatchWhenIssuanceIsThrottled() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());

        template.issue(LOGIN);
        IssueLimitExceededException throttled =
                assertThrows(IssueLimitExceededException.class, () -> template.issue(LOGIN));

        assertEquals(1, template.dispatches());
        assertFalse(throttled.retryAfter().isNegative());
        assertFalse(throttled.retryAfter().compareTo(Duration.ofSeconds(60)) > 0);
        assertEquals("test-key-cooldown", throttled.violations().getFirst().ruleId());
    }

    @Test
    void supportsExplicitPassthroughContextManager() {
        CapturingVerificationService template =
                new CapturingVerificationService(length -> "123456", new InMemoryVerificationStore());

        assertInstanceOf(DeliveryResult.Accepted.class, template.issue(LOGIN));
        assertInstanceOf(DeliveryResult.Accepted.class, template.issue(LOGIN));

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

        Instant earliestExpiration = Instant.now().plus(Duration.ofMinutes(1));
        DeliveryResult.Accepted issued = assertInstanceOf(DeliveryResult.Accepted.class, template.issue(LOGIN));

        assertEquals(4, requestedLength.get());
        assertFalse(issued.expiresAt().isBefore(earliestExpiration));
    }

    @Test
    void invalidatesCodeWhenDispatchFailsButKeepsIssueLimit() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());
        CodeSenderException failure = new CodeSenderException(VerificationChannel.EMAIL, "provider unavailable");
        template.failWith(failure);

        CodeSenderException thrown = assertThrows(CodeSenderException.class, () -> template.issue(LOGIN));
        template.failWith(null);

        assertSame(failure, thrown);
        assertThrows(IssueLimitExceededException.class, () -> template.issue(LOGIN));
    }

    @Test
    void invalidatesRejectedCodeButKeepsIssueLimit() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());
        template.respondWith(CodeSendResult.REJECTED);

        CodeSendRejectedException rejected = assertThrows(CodeSendRejectedException.class, () -> template.issue(LOGIN));
        assertEquals(VerificationChannel.EMAIL, rejected.channel());
        template.respondWith(CodeSendResult.ACCEPTED);

        assertThrows(IssueLimitExceededException.class, () -> template.issue(LOGIN));
    }

    @Test
    void keepsCodeWhenDeliveryOutcomeIsUnknown() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());
        template.respondWith(CodeSendResult.UNKNOWN);

        assertInstanceOf(DeliveryResult.Uncertain.class, template.issue(LOGIN));
        assertThrows(IssueLimitExceededException.class, () -> template.issue(LOGIN));
        template.verify(LOGIN, "123456");
    }

    @Test
    void preservesDispatchFailureAndSuppressesInvalidationFailure() {
        VerificationStoreException invalidationFailure = new VerificationStoreException("cleanup unavailable");
        VerificationStore store = new StubVerificationStore() {
            @Override
            public boolean invalidate(VerificationStoreKey key, String code) {
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
            public Instant store(VerificationStoreKey key, String code, VerificationPolicy policy, Instant issuedAt) {
                throw failure;
            }
        };
        CapturingVerificationService template = template(length -> "123456", store);

        VerificationStoreException thrown = assertThrows(VerificationStoreException.class, () -> template.issue(LOGIN));

        assertSame(failure, thrown);
        assertEquals(0, template.dispatches());
    }

    @Test
    void rejectsNullStoreExpiration() {
        VerificationStore store = new StubVerificationStore() {
            @Override
            public Instant store(VerificationStoreKey key, String code, VerificationPolicy policy, Instant issuedAt) {
                return null;
            }
        };
        CapturingVerificationService template = template(length -> "123456", store);

        NullPointerException thrown = assertThrows(NullPointerException.class, () -> template.issue(LOGIN));

        assertEquals("verification store expiration must not be null", thrown.getMessage());
        assertEquals(0, template.dispatches());
    }

    @Test
    void propagatesStoreFailureDuringVerification() {
        VerificationStoreException failure = new VerificationStoreException("storage unavailable");
        VerificationStore store = new StubVerificationStore() {
            @Override
            public VerifyResult verifyAndConsume(VerificationStoreKey key, String code, Instant verifiedAt) {
                throw failure;
            }
        };
        CapturingVerificationService template = template(length -> "123456", store);

        VerificationStoreException thrown =
                assertThrows(VerificationStoreException.class, () -> template.verify(LOGIN, "123456"));

        assertSame(failure, thrown);
    }

    @Test
    void preservesRejectedStoreResultInException() {
        for (VerifyResult result : VerifyResult.values()) {
            if (result == VerifyResult.SUCCESS) {
                continue;
            }
            VerificationStore store = new StubVerificationStore() {
                @Override
                public VerifyResult verifyAndConsume(VerificationStoreKey key, String code, Instant verifiedAt) {
                    return result;
                }
            };
            CapturingVerificationService template = template(length -> "123456", store);

            VerificationRejectedException thrown =
                    assertThrows(VerificationRejectedException.class, () -> template.verify(LOGIN, "123456"));

            assertSame(result, thrown.result());
        }
    }

    @Test
    void rejectsInvalidVerificationRejectedExceptionResults() {
        assertThrows(NullPointerException.class, () -> new VerificationRejectedException(null));
        assertThrows(IllegalArgumentException.class, () -> new VerificationRejectedException(VerifyResult.SUCCESS));
    }

    @Test
    void rejectsNullStoreVerificationResult() {
        VerificationStore store = new StubVerificationStore() {
            @Override
            public VerifyResult verifyAndConsume(VerificationStoreKey key, String code, Instant verifiedAt) {
                return null;
            }
        };
        CapturingVerificationService template = template(length -> "123456", store);

        NullPointerException thrown = assertThrows(NullPointerException.class, () -> template.verify(LOGIN, "123456"));

        assertEquals("verification store result must not be null", thrown.getMessage());
    }

    @Test
    void scopesAllStoreOperationsToServiceChannel() {
        AtomicReference<VerificationStoreKey> stored = new AtomicReference<>();
        AtomicReference<VerificationStoreKey> verified = new AtomicReference<>();
        AtomicReference<VerificationStoreKey> invalidated = new AtomicReference<>();
        VerificationStore store = new StubVerificationStore() {
            @Override
            public Instant store(VerificationStoreKey key, String code, VerificationPolicy policy, Instant issuedAt) {
                stored.set(key);
                return super.store(key, code, policy, issuedAt);
            }

            @Override
            public VerifyResult verifyAndConsume(VerificationStoreKey key, String code, Instant verifiedAt) {
                verified.set(key);
                return VerifyResult.NOT_FOUND;
            }

            @Override
            public boolean invalidate(VerificationStoreKey key, String code) {
                invalidated.set(key);
                return true;
            }
        };
        CapturingVerificationService template = template(length -> "123456", store);
        template.respondWith(CodeSendResult.REJECTED);

        assertThrows(CodeSendRejectedException.class, () -> template.issue(LOGIN));
        assertThrows(VerificationRejectedException.class, () -> template.verify(LOGIN, "123456"));

        VerificationStoreKey expected = new VerificationStoreKey(LOGIN, VerificationChannel.EMAIL);
        assertEquals(expected, stored.get());
        assertEquals(expected, invalidated.get());
        assertEquals(expected, verified.get());
    }

    @Test
    void appliesContributorsInOrderAndPassesSameContextToLimiterAndDelivery() {
        AtomicReference<IssueContext> captured = new AtomicReference<>();
        IssueLimiter limiter = (context, requestedAt) -> {
            captured.set(context);
            return new IssueLimitResult.Allowed();
        };
        CapturingVerificationService template = new CapturingVerificationService(
                length -> "123456",
                new InMemoryVerificationStore(),
                limiter,
                VerificationPolicy.defaults(),
                new CompositeIssueContextManager(List.of(
                        context -> context.with("ip-address", "203.0.113.10"),
                        context -> context.with("service", "capturing"))));
        assertInstanceOf(DeliveryResult.Accepted.class, template.issue(LOGIN));

        assertSame(captured.get(), template.delivery().context());
        assertEquals(LOGIN, captured.get().key());
        assertEquals(VerificationChannel.EMAIL, captured.get().channel());
        assertEquals("203.0.113.10", captured.get().attribute("ip-address").orElseThrow());
        assertEquals("capturing", captured.get().attribute("service").orElseThrow());
    }

    @Test
    void rejectsInvalidContributorResultsBeforeAcquiringQuota() {
        VerificationKey otherKey = new VerificationKey("account", "login", "other@example.com");
        AtomicInteger acquisitions = new AtomicInteger();
        IssueLimiter limiter = (context, requestedAt) -> {
            acquisitions.incrementAndGet();
            return new IssueLimitResult.Allowed();
        };
        CapturingVerificationService changedKey = serviceWithContributors(
                limiter, List.of(context -> IssueContext.of(otherKey, context.channel(), context.policy())));
        CapturingVerificationService changedChannel = serviceWithContributors(
                limiter, List.of(context -> IssueContext.of(context.key(), VerificationChannel.SMS, context.policy())));
        CapturingVerificationService changedPolicy = serviceWithContributors(
                limiter,
                List.of(context -> IssueContext.of(
                        context.key(), context.channel(), new VerificationPolicy(4, Duration.ofMinutes(1), 2))));
        CapturingVerificationService nullContext =
                serviceWithContributors(limiter, java.util.Arrays.asList(context -> null));

        assertEquals(
                "issue context contributor at index 0 must preserve the verification key",
                assertThrows(IllegalArgumentException.class, () -> changedKey.issue(LOGIN))
                        .getMessage());
        assertEquals(
                "issue context contributor at index 0 must preserve the verification channel",
                assertThrows(IllegalArgumentException.class, () -> changedChannel.issue(LOGIN))
                        .getMessage());
        assertEquals(
                "issue context contributor at index 0 must preserve the verification policy",
                assertThrows(IllegalArgumentException.class, () -> changedPolicy.issue(LOGIN))
                        .getMessage());
        assertEquals(
                "issue context contributor result must not be null: 0",
                assertThrows(NullPointerException.class, () -> nullContext.issue(LOGIN))
                        .getMessage());
        assertEquals(0, acquisitions.get());
    }

    @Test
    void rejectsInvalidCustomManagerResultsBeforeAcquiringQuota() {
        VerificationKey otherKey = new VerificationKey("account", "login", "other@example.com");
        AtomicInteger acquisitions = new AtomicInteger();
        IssueLimiter limiter = (context, requestedAt) -> {
            acquisitions.incrementAndGet();
            return new IssueLimitResult.Allowed();
        };
        List<IssueContextManager> managers = List.of(
                context -> IssueContext.of(otherKey, context.channel(), context.policy()),
                context -> IssueContext.of(context.key(), VerificationChannel.SMS, context.policy()),
                context -> IssueContext.of(
                        context.key(), context.channel(), new VerificationPolicy(4, Duration.ofMinutes(1), 2)));
        List<String> messages = List.of(
                "issue context manager must preserve the verification key",
                "issue context manager must preserve the verification channel",
                "issue context manager must preserve the verification policy");

        for (int index = 0; index < managers.size(); index++) {
            CapturingVerificationService service = new CapturingVerificationService(
                    length -> "123456",
                    new InMemoryVerificationStore(),
                    limiter,
                    VerificationPolicy.defaults(),
                    managers.get(index));

            assertEquals(
                    messages.get(index),
                    assertThrows(IllegalArgumentException.class, () -> service.issue(LOGIN))
                            .getMessage());
        }
        assertEquals(0, acquisitions.get());
    }

    @Test
    void contributorsCannotRemoveOrReplaceExistingAttributes() {
        IssueLimiter limiter = (context, requestedAt) -> new IssueLimitResult.Allowed();
        CapturingVerificationService replaced = serviceWithContributors(
                limiter,
                List.of(
                        context -> context.with("tenant-id", "tenant-1"),
                        context -> context.with("tenant-id", "tenant-2")));
        CapturingVerificationService removed = serviceWithContributors(
                limiter,
                List.of(
                        context -> context.with("tenant-id", "tenant-1"),
                        context -> IssueContext.of(context.key(), context.channel(), context.policy())));

        assertEquals(
                "issue context contributor at index 1 must preserve existing issue context attribute: tenant-id",
                assertThrows(IllegalArgumentException.class, () -> replaced.issue(LOGIN))
                        .getMessage());
        assertEquals(
                "issue context contributor at index 1 must preserve existing issue context attribute: tenant-id",
                assertThrows(IllegalArgumentException.class, () -> removed.issue(LOGIN))
                        .getMessage());
    }

    @Test
    void doesNotGenerateStoreOrDispatchWhenNoLimitRuleMatches() {
        AtomicInteger generations = new AtomicInteger();
        AtomicInteger stores = new AtomicInteger();
        VerificationStore store = new StubVerificationStore() {
            @Override
            public Instant store(VerificationStoreKey key, String code, VerificationPolicy policy, Instant issuedAt) {
                stores.incrementAndGet();
                return super.store(key, code, policy, issuedAt);
            }
        };
        IssueLimiter limiter = (context, requestedAt) -> {
            throw new MissingIssueLimitRuleException(context.key());
        };
        CapturingVerificationService template = new CapturingVerificationService(
                length -> {
                    generations.incrementAndGet();
                    return "123456";
                },
                store,
                limiter,
                VerificationPolicy.defaults(),
                new CompositeIssueContextManager(List.of()));

        assertThrows(MissingIssueLimitRuleException.class, () -> template.issue(LOGIN));
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
                        null, new InMemoryVerificationStore(), VerificationPolicy.defaults()));
        NullPointerException nullPolicy = assertThrows(
                NullPointerException.class,
                () -> new CapturingVerificationService(length -> "123456", new InMemoryVerificationStore(), null));
        assertEquals("verificationPolicy must not be null", nullPolicy.getMessage());
        assertThrows(
                NullPointerException.class,
                () -> new CapturingVerificationService(
                        length -> "123456",
                        new InMemoryVerificationStore(),
                        null,
                        VerificationPolicy.defaults(),
                        new CompositeIssueContextManager(List.of())));
        assertThrows(
                NullPointerException.class,
                () -> new CapturingVerificationService(
                        length -> "123456",
                        new InMemoryVerificationStore(),
                        IssueLimiter.permitAll(),
                        VerificationPolicy.defaults(),
                        (IssueContextManager) null));
        assertThrows(NullPointerException.class, () -> template.issue((VerificationKey) null));
    }

    @Test
    void declaresOnlyFullDependencyConstructor() {
        var constructors = AbstractVerificationService.class.getDeclaredConstructors();

        assertEquals(1, constructors.length);
        assertDoesNotThrow(() -> AbstractVerificationService.class.getDeclaredConstructor(
                CodeGenerator.class,
                VerificationStore.class,
                IssueLimiter.class,
                VerificationPolicy.class,
                IssueContextManager.class));
    }

    private CapturingVerificationService template(CodeGenerator generator, VerificationStore store) {
        return template(generator, store, VerificationPolicy.defaults());
    }

    private CapturingVerificationService template(
            CodeGenerator generator, VerificationStore store, VerificationPolicy verificationPolicy) {
        return new CapturingVerificationService(generator, store, testIssueLimiter(), verificationPolicy);
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

    private CapturingVerificationService serviceWithContributors(
            IssueLimiter limiter, List<IssueContextContributor> contributors) {
        return new CapturingVerificationService(
                length -> "123456",
                new InMemoryVerificationStore(),
                limiter,
                VerificationPolicy.defaults(),
                new CompositeIssueContextManager(contributors));
    }

    private static final class CapturingVerificationService extends AbstractVerificationService<DeliveryResult> {

        private final AtomicReference<TestDispatch> delivery = new AtomicReference<>();
        private final AtomicInteger dispatches = new AtomicInteger();
        private RuntimeException failure;
        private CodeSendResult result = CodeSendResult.ACCEPTED;

        private CapturingVerificationService(CodeGenerator generator, VerificationStore store) {
            this(generator, store, IssueLimiter.permitAll(), VerificationPolicy.defaults());
        }

        private CapturingVerificationService(
                CodeGenerator generator, VerificationStore store, VerificationPolicy verificationPolicy) {
            this(generator, store, IssueLimiter.permitAll(), verificationPolicy);
        }

        private CapturingVerificationService(
                CodeGenerator generator,
                VerificationStore store,
                IssueLimiter issueLimiter,
                VerificationPolicy verificationPolicy) {
            this(generator, store, issueLimiter, verificationPolicy, new CompositeIssueContextManager(List.of()));
        }

        private CapturingVerificationService(
                CodeGenerator generator,
                VerificationStore store,
                IssueLimiter issueLimiter,
                VerificationPolicy verificationPolicy,
                IssueContextManager issueContextManager) {
            super(generator, store, issueLimiter, verificationPolicy, issueContextManager);
        }

        @Override
        protected DeliveryResult completeIssue(IssueContext context, String code, Instant expiresAt) {
            dispatches.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
            delivery.set(new TestDispatch(context, code, expiresAt));
            return switch (result) {
                case ACCEPTED -> new DeliveryResult.Accepted(expiresAt);
                case UNKNOWN -> new DeliveryResult.Uncertain(expiresAt);
                case REJECTED -> throw new CodeSendRejectedException(context.channel());
            };
        }

        @Override
        protected VerificationChannel channel() {
            return VerificationChannel.EMAIL;
        }

        private TestDispatch delivery() {
            return delivery.get();
        }

        private int dispatches() {
            return dispatches.get();
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
        public Instant store(VerificationStoreKey key, String code, VerificationPolicy policy, Instant issuedAt) {
            return NOW.plusSeconds(60);
        }

        @Override
        public VerifyResult verifyAndConsume(VerificationStoreKey key, String code, Instant verifiedAt) {
            return VerifyResult.NOT_FOUND;
        }

        @Override
        public boolean invalidate(VerificationStoreKey key, String code) {
            return true;
        }
    }
}
