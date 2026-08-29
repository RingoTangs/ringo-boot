package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class IssueRateLimitManagerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final VerificationKey KEY = new VerificationKey("account", "login", "user@example.com");
    private static final IssueContext CONTEXT =
            IssueContext.of(KEY, VerificationChannel.EMAIL, VerificationPolicy.defaults());

    @Test
    void selectsRulesAndSubmitsResolvedQuotas() {
        IssueRateLimitRule subjectRule = rule("subject-minute", context -> true, "user@example.com");
        IssueRateLimitRule skipped = rule("registration-minute", context -> false, "registration");
        AtomicReference<List<IssueLimitQuota>> captured = new AtomicReference<>();
        IssueRateLimitManager manager = new IssueRateLimitManager(List.of(subjectRule, skipped), (rules, time) -> {
            captured.set(rules);
            assertEquals(NOW, time);
            return new IssueLimitResult.Allowed();
        });

        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(CONTEXT, NOW));
        assertEquals(1, captured.get().size());
        assertEquals("subject-minute", captured.get().getFirst().ruleId());
        assertEquals(
                IssueLimitBucket.of("user@example.com"),
                captured.get().getFirst().bucket());
        assertThrows(UnsupportedOperationException.class, () -> captured.get().clear());
    }

    @Test
    void selectsBuiltInRulesByContextChannel() {
        IssueRateLimitRule emailRule = PurposeIssueQuotaRule.builder()
                .id("login-email-minute")
                .namespace("account")
                .purpose("login")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(10)
                .window(Duration.ofMinutes(1))
                .build();
        IssueRateLimitRule smsRule = PurposeIssueQuotaRule.builder()
                .id("login-sms-minute")
                .namespace("account")
                .purpose("login")
                .channel(VerificationChannel.SMS)
                .maxIssues(10)
                .window(Duration.ofMinutes(1))
                .build();
        AtomicReference<List<IssueLimitQuota>> captured = new AtomicReference<>();
        IssueRateLimitManager manager =
                new IssueRateLimitManager(List.of(emailRule, smsRule), (quotas, requestedAt) -> {
                    captured.set(quotas);
                    return new IssueLimitResult.Allowed();
                });

        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(CONTEXT, NOW));
        assertEquals(
                List.of("login-email-minute"),
                captured.get().stream().map(IssueLimitQuota::ruleId).toList());
    }

    @Test
    void ruleThatIgnoresChannelSharesQuotaAcrossChannels() {
        IssueRateLimitRule rule = new TestIssueRateLimitRule(
                "application-minute", context -> IssueLimitBucket.of("application"), 1, Duration.ofMinutes(1));
        IssueRateLimitManager manager = new IssueRateLimitManager(List.of(rule), new InMemoryIssueRateLimitStore());
        IssueContext otherContext = IssueContext.of(
                new VerificationKey("payment", "confirm", "+8613800000000"),
                VerificationChannel.SMS,
                VerificationPolicy.defaults());

        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(CONTEXT, NOW));
        assertInstanceOf(IssueLimitResult.Throttled.class, manager.acquire(otherContext, NOW.plusSeconds(1)));
    }

    @Test
    void rejectsWhenNoRulesMatchWithoutCallingStore() {
        AtomicInteger calls = new AtomicInteger();
        IssueRateLimitManager manager = new IssueRateLimitManager(
                List.of(rule("registration-minute", context -> false, "registration")), (rules, time) -> {
                    calls.incrementAndGet();
                    return new IssueLimitResult.Throttled(
                            List.of(new IssueLimitViolation("test-rule", Duration.ofSeconds(1))));
                });

        MissingIssueRateLimitRuleException exception =
                assertThrows(MissingIssueRateLimitRuleException.class, () -> manager.acquire(CONTEXT, NOW));
        assertEquals("no issue rate limit rule matches namespace=account, purpose=login", exception.getMessage());
        assertEquals(-1, exception.getMessage().indexOf(KEY.subject()));
        assertEquals(0, calls.get());
    }

    @Test
    void resolvesAllBucketsBeforeCallingStore() {
        AtomicInteger calls = new AtomicInteger();
        IssueRateLimitRule missingIp = new TestIssueRateLimitRule(
                "ip-hour",
                context -> IssueLimitBucket.of(context.attribute("ip-address").orElseThrow()),
                10,
                Duration.ofHours(1));
        IssueRateLimitManager manager = new IssueRateLimitManager(List.of(missingIp), (rules, time) -> {
            calls.incrementAndGet();
            return new IssueLimitResult.Allowed();
        });

        assertThrows(RuntimeException.class, () -> manager.acquire(CONTEXT, NOW));
        assertEquals(0, calls.get());
    }

    @Test
    void rejectsDuplicateAndInvalidRuleDeclarations() {
        IssueRateLimitRule rule = rule("subject-minute", context -> true, "subject");
        IssueRateLimitRule invalidRule = new IssueRateLimitRule() {
            @Override
            public String id() {
                return "subject_minute";
            }

            @Override
            public boolean matches(IssueContext context) {
                return true;
            }

            @Override
            public IssueLimitBucket bucket(IssueContext context) {
                return IssueLimitBucket.of("subject");
            }

            @Override
            public int maxIssues() {
                return 1;
            }

            @Override
            public Duration window() {
                return Duration.ofMinutes(1);
            }
        };
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueRateLimitManager(List.of(rule, rule), (rules, time) -> new IssueLimitResult.Allowed()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueRateLimitManager(List.of(invalidRule), (rules, time) -> new IssueLimitResult.Allowed()));
        assertThrows(
                NullPointerException.class,
                () -> new IssueRateLimitManager(
                        Arrays.asList(rule, null), (rules, time) -> new IssueLimitResult.Allowed()));
        assertThrows(
                MissingIssueRateLimitRuleException.class,
                () -> new IssueRateLimitManager(List.of(), (rules, time) -> new IssueLimitResult.Allowed()));
    }

    @Test
    void rejectsNullStoreResult() {
        IssueRateLimitManager manager = new IssueRateLimitManager(
                List.of(rule("subject-minute", context -> true, "subject")), (rules, time) -> null);

        assertThrows(NullPointerException.class, () -> manager.acquire(CONTEXT, NOW));
    }

    @Test
    void usesContextAttributesWhenEvaluatingRules() {
        AtomicReference<IssueLimitQuota> captured = new AtomicReference<>();
        IssueRateLimitRule ipRule = new TestIssueRateLimitRule(
                "ip-hour",
                context -> IssueLimitBucket.of(context.attribute("ip-address").orElseThrow()),
                10,
                Duration.ofHours(1));
        IssueContext expected = CONTEXT.with("ip-address", "203.0.113.10");
        IssueRateLimitManager manager = new IssueRateLimitManager(List.of(ipRule), (quotas, requestedAt) -> {
            captured.set(quotas.getFirst());
            return new IssueLimitResult.Allowed();
        });

        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(expected, NOW));
        assertEquals(IssueLimitBucket.of("203.0.113.10"), captured.get().bucket());
    }

    private IssueRateLimitRule rule(
            String id, java.util.function.Predicate<IssueContext> matcher, String bucketSegment) {
        return new TestIssueRateLimitRule(
                id, matcher, context -> IssueLimitBucket.of(bucketSegment), 1, Duration.ofMinutes(1));
    }
}
