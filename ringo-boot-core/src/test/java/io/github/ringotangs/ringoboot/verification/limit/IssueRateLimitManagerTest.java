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
    private static final IssueContext CONTEXT =
            IssueContext.of(new VerificationKey("account", "login", "user@example.com"));

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
    void allowsWhenNoRulesMatchWithoutCallingStore() {
        AtomicInteger calls = new AtomicInteger();
        IssueRateLimitManager manager = new IssueRateLimitManager(
                List.of(rule("registration-minute", context -> false, "registration")), (rules, time) -> {
                    calls.incrementAndGet();
                    return new IssueLimitResult.Throttled(Duration.ofSeconds(1));
                });

        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(CONTEXT, NOW));
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

        assertThrows(RuntimeException.class, () -> manager.acquire(CONTEXT, NOW));
        assertEquals(0, calls.get());
    }

    @Test
    void rejectsDuplicateAndInvalidRuleDeclarations() {
        IssueRateLimitRule rule = rule("subject-minute", context -> true, "subject");
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueRateLimitManager(List.of(rule, rule), (rules, time) -> new IssueLimitResult.Allowed()));
        assertThrows(
                NullPointerException.class,
                () -> new IssueRateLimitManager(
                        Arrays.asList(rule, null), (rules, time) -> new IssueLimitResult.Allowed()));
    }

    @Test
    void rejectsNullStoreResult() {
        IssueRateLimitManager manager = new IssueRateLimitManager(
                List.of(rule("subject-minute", context -> true, "subject")), (rules, time) -> null);

        assertThrows(NullPointerException.class, () -> manager.acquire(CONTEXT, NOW));
    }

    private IssueRateLimitRule rule(
            String id, java.util.function.Predicate<IssueContext> matcher, String bucketSegment) {
        return IssueRateLimitRule.of(
                id, matcher, context -> IssueLimitBucket.of(bucketSegment), 1, Duration.ofMinutes(1));
    }
}
