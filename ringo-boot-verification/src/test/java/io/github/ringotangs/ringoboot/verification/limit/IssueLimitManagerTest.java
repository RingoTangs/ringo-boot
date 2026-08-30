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

class IssueLimitManagerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final VerificationKey KEY = new VerificationKey("account", "login", "user@example.com");
    private static final IssueContext CONTEXT =
            IssueContext.of(KEY, VerificationChannel.EMAIL, VerificationPolicy.defaults());

    @Test
    void selectsRulesAndSubmitsResolvedQuotas() {
        IssueLimitRule subjectRule = rule("subject-minute", context -> true, "user@example.com");
        IssueLimitRule skipped = rule("registration-minute", context -> false, "registration");
        AtomicReference<List<IssueLimitQuota>> captured = new AtomicReference<>();
        IssueLimitManager manager = new IssueLimitManager(List.of(subjectRule, skipped), (rules, time) -> {
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
        IssueLimitRule emailRule = PurposeQuotaRule.builder()
                .id("login-email-minute")
                .namespace("account")
                .purpose("login")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(10)
                .window(Duration.ofMinutes(1))
                .build();
        IssueLimitRule smsRule = PurposeQuotaRule.builder()
                .id("login-sms-minute")
                .namespace("account")
                .purpose("login")
                .channel(VerificationChannel.SMS)
                .maxIssues(10)
                .window(Duration.ofMinutes(1))
                .build();
        AtomicReference<List<IssueLimitQuota>> captured = new AtomicReference<>();
        IssueLimitManager manager = new IssueLimitManager(List.of(emailRule, smsRule), (quotas, requestedAt) -> {
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
        IssueLimitRule rule = new TestIssueLimitRule(
                "application-minute", context -> IssueLimitBucket.of("application"), 1, Duration.ofMinutes(1));
        IssueLimitManager manager = new IssueLimitManager(List.of(rule), new InMemoryIssueLimitStore());
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
        IssueLimitManager manager = new IssueLimitManager(
                List.of(rule("registration-minute", context -> false, "registration")), (rules, time) -> {
                    calls.incrementAndGet();
                    return new IssueLimitResult.Throttled(
                            List.of(new IssueLimitViolation("test-rule", Duration.ofSeconds(1))));
                });

        MissingIssueLimitRuleException exception =
                assertThrows(MissingIssueLimitRuleException.class, () -> manager.acquire(CONTEXT, NOW));
        assertEquals("no issue rate limit rule matches namespace=account, purpose=login", exception.getMessage());
        assertEquals(-1, exception.getMessage().indexOf(KEY.subject()));
        assertEquals(0, calls.get());
    }

    @Test
    void resolvesAllBucketsBeforeCallingStore() {
        AtomicInteger calls = new AtomicInteger();
        IssueLimitRule missingIp = new TestIssueLimitRule(
                "ip-hour",
                context -> IssueLimitBucket.of(context.attribute("ip-address").orElseThrow()),
                10,
                Duration.ofHours(1));
        IssueLimitManager manager = new IssueLimitManager(List.of(missingIp), (rules, time) -> {
            calls.incrementAndGet();
            return new IssueLimitResult.Allowed();
        });

        assertThrows(RuntimeException.class, () -> manager.acquire(CONTEXT, NOW));
        assertEquals(0, calls.get());
    }

    @Test
    void rejectsDuplicateAndInvalidRuleDeclarations() {
        IssueLimitRule rule = rule("subject-minute", context -> true, "subject");
        IssueLimitRule invalidRule = new IssueLimitRule() {
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
                () -> new IssueLimitManager(List.of(rule, rule), (rules, time) -> new IssueLimitResult.Allowed()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueLimitManager(List.of(invalidRule), (rules, time) -> new IssueLimitResult.Allowed()));
        assertThrows(
                NullPointerException.class,
                () -> new IssueLimitManager(
                        Arrays.asList(rule, null), (rules, time) -> new IssueLimitResult.Allowed()));
        assertThrows(
                MissingIssueLimitRuleException.class,
                () -> new IssueLimitManager(List.of(), (rules, time) -> new IssueLimitResult.Allowed()));
    }

    @Test
    void rejectsNullStoreResult() {
        IssueLimitManager manager = new IssueLimitManager(
                List.of(rule("subject-minute", context -> true, "subject")), (rules, time) -> null);

        assertThrows(NullPointerException.class, () -> manager.acquire(CONTEXT, NOW));
    }

    @Test
    void usesContextAttributesWhenEvaluatingRules() {
        AtomicReference<IssueLimitQuota> captured = new AtomicReference<>();
        IssueLimitRule ipRule = new TestIssueLimitRule(
                "ip-hour",
                context -> IssueLimitBucket.of(context.attribute("ip-address").orElseThrow()),
                10,
                Duration.ofHours(1));
        IssueContext expected = CONTEXT.with("ip-address", "203.0.113.10");
        IssueLimitManager manager = new IssueLimitManager(List.of(ipRule), (quotas, requestedAt) -> {
            captured.set(quotas.getFirst());
            return new IssueLimitResult.Allowed();
        });

        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(expected, NOW));
        assertEquals(IssueLimitBucket.of("203.0.113.10"), captured.get().bucket());
    }

    private IssueLimitRule rule(String id, java.util.function.Predicate<IssueContext> matcher, String bucketSegment) {
        return new TestIssueLimitRule(
                id, matcher, context -> IssueLimitBucket.of(bucketSegment), 1, Duration.ofMinutes(1));
    }
}
