package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryIssueRateLimitBackendTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void enforcesRollingWindowAndSeparatesBuckets() {
        InMemoryIssueRateLimitBackend backend = new InMemoryIssueRateLimitBackend();
        IssueRateLimitConstraint first = constraint("subject-minute", "first", 2, Duration.ofMinutes(1));
        IssueRateLimitConstraint second = constraint("subject-minute", "second", 2, Duration.ofMinutes(1));

        assertInstanceOf(IssueLimitResult.Allowed.class, backend.acquire(List.of(first), NOW));
        assertInstanceOf(IssueLimitResult.Allowed.class, backend.acquire(List.of(first), NOW.plusSeconds(10)));
        IssueLimitResult.Throttled throttled = assertInstanceOf(
                IssueLimitResult.Throttled.class, backend.acquire(List.of(first), NOW.plusSeconds(20)));
        assertEquals(Duration.ofSeconds(40), throttled.retryAfter());
        assertInstanceOf(IssueLimitResult.Allowed.class, backend.acquire(List.of(second), NOW.plusSeconds(20)));
        assertInstanceOf(IssueLimitResult.Allowed.class, backend.acquire(List.of(first), NOW.plusSeconds(60)));
    }

    @Test
    void doesNotPartiallyConsumeWhenOneConstraintIsThrottled() {
        InMemoryIssueRateLimitBackend backend = new InMemoryIssueRateLimitBackend();
        IssueRateLimitConstraint hourly = constraint("subject-hour", "user", 2, Duration.ofHours(1));
        IssueRateLimitConstraint minute = constraint("subject-minute", "user", 1, Duration.ofMinutes(1));

        assertInstanceOf(IssueLimitResult.Allowed.class, backend.acquire(List.of(hourly, minute), NOW));
        assertInstanceOf(
                IssueLimitResult.Throttled.class, backend.acquire(List.of(hourly, minute), NOW.plusSeconds(10)));
        assertInstanceOf(IssueLimitResult.Allowed.class, backend.acquire(List.of(hourly, minute), NOW.plusSeconds(60)));
        assertInstanceOf(IssueLimitResult.Throttled.class, backend.acquire(List.of(hourly), NOW.plusSeconds(61)));
    }

    @Test
    void returnsLargestRetryAfterAcrossConstraints() {
        InMemoryIssueRateLimitBackend backend = new InMemoryIssueRateLimitBackend();
        IssueRateLimitConstraint minute = constraint("ip-minute", "ip", 1, Duration.ofMinutes(1));
        IssueRateLimitConstraint hour = constraint("subject-hour", "user", 1, Duration.ofHours(1));
        backend.acquire(List.of(minute), NOW);
        backend.acquire(List.of(hour), NOW.plusSeconds(10));

        IssueLimitResult.Throttled throttled = assertInstanceOf(
                IssueLimitResult.Throttled.class, backend.acquire(List.of(minute, hour), NOW.plusSeconds(20)));

        assertEquals(Duration.ofMinutes(59).plusSeconds(50), throttled.retryAfter());
    }

    @Test
    void validatesInputsAndStableRuleWindow() {
        InMemoryIssueRateLimitBackend backend = new InMemoryIssueRateLimitBackend();
        IssueRateLimitConstraint minute = constraint("subject-limit", "user", 1, Duration.ofMinutes(1));
        backend.acquire(List.of(minute), NOW);

        assertThrows(
                IllegalArgumentException.class,
                () -> backend.acquire(
                        List.of(constraint("subject-limit", "user", 1, Duration.ofHours(1))), NOW.plusSeconds(1)));
        assertThrows(NullPointerException.class, () -> backend.acquire(null, NOW));
        assertThrows(NullPointerException.class, () -> backend.acquire(Arrays.asList(minute, null), NOW));
        assertInstanceOf(IssueLimitResult.Allowed.class, backend.acquire(List.of(), NOW));
    }

    private IssueRateLimitConstraint constraint(String ruleId, String bucket, int maxIssues, Duration window) {
        return new IssueRateLimitConstraint(ruleId, IssueLimitBucket.of(bucket), maxIssues, window);
    }
}
