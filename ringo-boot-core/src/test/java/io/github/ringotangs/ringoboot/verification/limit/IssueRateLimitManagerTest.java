package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
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

        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(KEY, NOW));
        assertEquals(1, captured.get().size());
        assertEquals("subject-minute", captured.get().getFirst().ruleId());
        assertEquals(
                IssueLimitBucket.of("user@example.com"),
                captured.get().getFirst().bucket());
        assertThrows(UnsupportedOperationException.class, () -> captured.get().clear());
    }

    @Test
    void rejectsWhenNoRulesMatchWithoutCallingStore() {
        AtomicInteger calls = new AtomicInteger();
        IssueRateLimitManager manager = new IssueRateLimitManager(
                List.of(rule("registration-minute", context -> false, "registration")), (rules, time) -> {
                    calls.incrementAndGet();
                    return new IssueLimitResult.Throttled(Duration.ofSeconds(1));
                });

        MissingIssueRateLimitRuleException exception =
                assertThrows(MissingIssueRateLimitRuleException.class, () -> manager.acquire(KEY, NOW));
        assertEquals("no issue rate limit rule matches namespace=account, purpose=login", exception.getMessage());
        assertEquals(-1, exception.getMessage().indexOf(KEY.subject()));
        assertEquals(0, calls.get());
    }

    @Test
    void resolvesAllBucketsBeforeCallingStore() {
        AtomicInteger calls = new AtomicInteger();
        IssueRateLimitRule missingIp = IssueRateLimitRule.of(
                "ip-hour",
                context -> IssueLimitBucket.of(context.attribute("ip-address").orElseThrow()),
                10,
                Duration.ofHours(1));
        IssueRateLimitManager manager = new IssueRateLimitManager(List.of(missingIp), (rules, time) -> {
            calls.incrementAndGet();
            return new IssueLimitResult.Allowed();
        });

        assertThrows(RuntimeException.class, () -> manager.acquire(KEY, NOW));
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

        assertThrows(NullPointerException.class, () -> manager.acquire(KEY, NOW));
    }

    @Test
    void resolvesContextAttributesBeforeEvaluatingRules() {
        AtomicReference<IssueLimitQuota> captured = new AtomicReference<>();
        IssueRateLimitRule ipRule = IssueRateLimitRule.of(
                "ip-hour",
                context -> IssueLimitBucket.of(context.attribute("ip-address").orElseThrow()),
                10,
                Duration.ofHours(1));
        IssueContext expected = IssueContext.of(KEY).with("ip-address", "203.0.113.10");
        IssueRateLimitManager manager = new IssueRateLimitManager(
                List.of(ipRule),
                (quotas, requestedAt) -> {
                    captured.set(quotas.getFirst());
                    return new IssueLimitResult.Allowed();
                },
                key -> expected);

        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(KEY, NOW));
        assertEquals(IssueLimitBucket.of("203.0.113.10"), captured.get().bucket());
    }

    @Test
    void rejectsInvalidResolverResultsBeforeCallingStore() {
        AtomicInteger calls = new AtomicInteger();
        IssueRateLimitStore store = (quotas, requestedAt) -> {
            calls.incrementAndGet();
            return new IssueLimitResult.Allowed();
        };
        IssueRateLimitRule rule = rule("subject-minute", context -> true, "subject");
        IssueRateLimitManager nullContext = new IssueRateLimitManager(List.of(rule), store, key -> null);
        IssueRateLimitManager changedKey = new IssueRateLimitManager(
                List.of(rule),
                store,
                key -> IssueContext.of(new VerificationKey("account", "login", "another@example.com")));

        assertThrows(NullPointerException.class, () -> nullContext.acquire(KEY, NOW));
        assertThrows(IllegalArgumentException.class, () -> changedKey.acquire(KEY, NOW));
        assertEquals(0, calls.get());
    }

    private IssueRateLimitRule rule(
            String id, java.util.function.Predicate<IssueContext> matcher, String bucketSegment) {
        return IssueRateLimitRule.of(
                id, matcher, context -> IssueLimitBucket.of(bucketSegment), 1, Duration.ofMinutes(1));
    }
}
