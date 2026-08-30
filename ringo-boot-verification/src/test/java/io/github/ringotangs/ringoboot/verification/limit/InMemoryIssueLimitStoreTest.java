package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryIssueLimitStoreTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void enforcesRollingWindowAndSeparatesBuckets() {
        InMemoryIssueLimitStore store = new InMemoryIssueLimitStore();
        IssueLimitQuota first = quota("subject-minute", "first", 2, Duration.ofMinutes(1));
        IssueLimitQuota second = quota("subject-minute", "second", 2, Duration.ofMinutes(1));

        assertInstanceOf(IssueLimitResult.Allowed.class, store.acquire(List.of(first), NOW));
        assertInstanceOf(IssueLimitResult.Allowed.class, store.acquire(List.of(first), NOW.plusSeconds(10)));
        IssueLimitResult.Throttled throttled =
                assertInstanceOf(IssueLimitResult.Throttled.class, store.acquire(List.of(first), NOW.plusSeconds(20)));
        assertEquals(Duration.ofSeconds(40), throttled.retryAfter());
        assertEquals(
                List.of(new IssueLimitViolation("subject-minute", Duration.ofSeconds(40))), throttled.violations());
        assertInstanceOf(IssueLimitResult.Allowed.class, store.acquire(List.of(second), NOW.plusSeconds(20)));
        assertInstanceOf(IssueLimitResult.Allowed.class, store.acquire(List.of(first), NOW.plusSeconds(60)));
    }

    @Test
    void doesNotPartiallyConsumeWhenOneConstraintIsThrottled() {
        InMemoryIssueLimitStore store = new InMemoryIssueLimitStore();
        IssueLimitQuota hourly = quota("subject-hour", "user", 2, Duration.ofHours(1));
        IssueLimitQuota minute = quota("subject-minute", "user", 1, Duration.ofMinutes(1));

        assertInstanceOf(IssueLimitResult.Allowed.class, store.acquire(List.of(hourly, minute), NOW));
        assertInstanceOf(IssueLimitResult.Throttled.class, store.acquire(List.of(hourly, minute), NOW.plusSeconds(10)));
        assertInstanceOf(IssueLimitResult.Allowed.class, store.acquire(List.of(hourly, minute), NOW.plusSeconds(60)));
        assertInstanceOf(IssueLimitResult.Throttled.class, store.acquire(List.of(hourly), NOW.plusSeconds(61)));
    }

    @Test
    void returnsLargestRetryAfterAcrossConstraints() {
        InMemoryIssueLimitStore store = new InMemoryIssueLimitStore();
        IssueLimitQuota minute = quota("ip-minute", "ip", 1, Duration.ofMinutes(1));
        IssueLimitQuota hour = quota("subject-hour", "user", 1, Duration.ofHours(1));
        store.acquire(List.of(minute), NOW);
        store.acquire(List.of(hour), NOW.plusSeconds(10));

        IssueLimitResult.Throttled throttled = assertInstanceOf(
                IssueLimitResult.Throttled.class, store.acquire(List.of(minute, hour), NOW.plusSeconds(20)));

        assertEquals(Duration.ofMinutes(59).plusSeconds(50), throttled.retryAfter());
        assertEquals(
                List.of(
                        new IssueLimitViolation("ip-minute", Duration.ofSeconds(40)),
                        new IssueLimitViolation(
                                "subject-hour", Duration.ofMinutes(59).plusSeconds(50))),
                throttled.violations());
    }

    @Test
    void validatesInputsAndStableRuleWindow() {
        InMemoryIssueLimitStore store = new InMemoryIssueLimitStore();
        IssueLimitQuota minute = quota("subject-limit", "user", 1, Duration.ofMinutes(1));
        store.acquire(List.of(minute), NOW);

        assertThrows(
                IllegalArgumentException.class,
                () -> store.acquire(
                        List.of(quota("subject-limit", "user", 1, Duration.ofHours(1))), NOW.plusSeconds(1)));
        assertThrows(NullPointerException.class, () -> store.acquire(null, NOW));
        assertThrows(NullPointerException.class, () -> store.acquire(Arrays.asList(minute, null), NOW));
        assertThrows(IllegalArgumentException.class, () -> store.acquire(List.of(), NOW));
    }

    private IssueLimitQuota quota(String ruleId, String bucket, int maxIssues, Duration window) {
        return new IssueLimitQuota(ruleId, IssueLimitBucket.of(bucket), maxIssues, window);
    }
}
