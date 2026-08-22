package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class InMemoryIssueRateLimiterTest {

    private static final VerificationKey KEY = new VerificationKey("account", "login", "user@example.com");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void throttlesSameKeyUntilIntervalElapses() {
        InMemoryIssueRateLimiter limiter = new InMemoryIssueRateLimiter(Duration.ofSeconds(60));

        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(KEY, NOW));
        IssueLimitResult.Throttled throttled =
                assertInstanceOf(IssueLimitResult.Throttled.class, limiter.acquire(KEY, NOW.plusSeconds(10)));
        assertEquals(Duration.ofSeconds(50), throttled.retryAfter());
        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(KEY, NOW.plusSeconds(60)));
    }

    @Test
    void isolatesVerificationKeysAndSupportsExplicitDisable() {
        InMemoryIssueRateLimiter limiter = new InMemoryIssueRateLimiter(Duration.ofSeconds(60));
        VerificationKey other = new VerificationKey("account", "login", "other@example.com");

        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(KEY, NOW));
        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(other, NOW));

        InMemoryIssueRateLimiter disabled = new InMemoryIssueRateLimiter(Duration.ZERO);
        assertInstanceOf(IssueLimitResult.Allowed.class, disabled.acquire(KEY, NOW));
        assertInstanceOf(IssueLimitResult.Allowed.class, disabled.acquire(KEY, NOW));
    }

    @Test
    void permitsExactlyOneConcurrentAcquisition() throws Exception {
        InMemoryIssueRateLimiter limiter = new InMemoryIssueRateLimiter(Duration.ofSeconds(60));
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<IssueLimitResult>> tasks = new ArrayList<>();
        for (int index = 0; index < threads; index++) {
            tasks.add(() -> {
                start.await();
                return limiter.acquire(KEY, NOW);
            });
        }
        try (var executor = Executors.newFixedThreadPool(threads)) {
            var futures = tasks.stream().map(executor::submit).toList();
            start.countDown();
            long allowed = 0;
            for (var future : futures) {
                if (future.get() instanceof IssueLimitResult.Allowed) {
                    allowed++;
                }
            }
            assertEquals(1, allowed);
        }
    }

    @Test
    void appliesSubjectQuotaAcrossNamespacesAndPurposes() {
        InMemoryIssueRateLimiter limiter = InMemoryIssueRateLimiter.withPolicy(IssueRateLimitPolicy.of(
                new IssueRateLimitRule(
                        Set.of(IssueLimitDimension.NAMESPACE, IssueLimitDimension.PURPOSE, IssueLimitDimension.SUBJECT),
                        1,
                        Duration.ofSeconds(60)),
                new IssueRateLimitRule(Set.of(IssueLimitDimension.SUBJECT), 2, Duration.ofHours(1))));
        VerificationKey registration = new VerificationKey("account", "registration", KEY.subject());
        VerificationKey payment = new VerificationKey("payment", "confirmation", KEY.subject());
        VerificationKey otherSubject = new VerificationKey("account", "login", "other@example.com");

        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(KEY, NOW));
        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(registration, NOW.plusSeconds(60)));
        IssueLimitResult.Throttled throttled =
                assertInstanceOf(IssueLimitResult.Throttled.class, limiter.acquire(payment, NOW.plusSeconds(120)));
        assertEquals(Duration.ofMinutes(58), throttled.retryAfter());
        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(otherSubject, NOW.plusSeconds(120)));
    }

    @Test
    void usesRollingWindowAndExpiresLeftBoundary() {
        InMemoryIssueRateLimiter limiter = InMemoryIssueRateLimiter.withPolicy(IssueRateLimitPolicy.of(
                new IssueRateLimitRule(Set.of(IssueLimitDimension.SUBJECT), 2, Duration.ofHours(1))));
        VerificationKey registration = new VerificationKey("account", "registration", KEY.subject());
        VerificationKey payment = new VerificationKey("payment", "confirmation", KEY.subject());

        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(KEY, NOW));
        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(registration, NOW.plusSeconds(1_800)));
        IssueLimitResult.Throttled throttled =
                assertInstanceOf(IssueLimitResult.Throttled.class, limiter.acquire(payment, NOW.plusSeconds(3_540)));
        assertEquals(Duration.ofMinutes(1), throttled.retryAfter());
        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(payment, NOW.plusSeconds(3_600)));
    }

    @Test
    void consumesNoRulesWhenAnyRuleRejectsRequest() {
        InMemoryIssueRateLimiter limiter = InMemoryIssueRateLimiter.withPolicy(IssueRateLimitPolicy.of(
                new IssueRateLimitRule(
                        Set.of(IssueLimitDimension.NAMESPACE, IssueLimitDimension.PURPOSE, IssueLimitDimension.SUBJECT),
                        1,
                        Duration.ofHours(1)),
                new IssueRateLimitRule(Set.of(IssueLimitDimension.SUBJECT), 2, Duration.ofDays(1))));
        VerificationKey registration = new VerificationKey("account", "registration", KEY.subject());
        VerificationKey payment = new VerificationKey("payment", "confirmation", KEY.subject());

        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(KEY, NOW));
        assertInstanceOf(IssueLimitResult.Throttled.class, limiter.acquire(KEY, NOW.plusSeconds(10)));
        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(registration, NOW.plusSeconds(20)));
        assertInstanceOf(IssueLimitResult.Throttled.class, limiter.acquire(payment, NOW.plusSeconds(30)));
    }

    @Test
    void returnsLongestRetryAfterWhenMultipleRulesReject() {
        InMemoryIssueRateLimiter limiter = InMemoryIssueRateLimiter.withPolicy(IssueRateLimitPolicy.of(
                new IssueRateLimitRule(
                        Set.of(IssueLimitDimension.NAMESPACE, IssueLimitDimension.PURPOSE, IssueLimitDimension.SUBJECT),
                        1,
                        Duration.ofMinutes(10)),
                new IssueRateLimitRule(Set.of(IssueLimitDimension.SUBJECT), 1, Duration.ofHours(1))));

        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(KEY, NOW));
        IssueLimitResult.Throttled throttled =
                assertInstanceOf(IssueLimitResult.Throttled.class, limiter.acquire(KEY, NOW.plusSeconds(60)));

        assertEquals(Duration.ofMinutes(59), throttled.retryAfter());
    }

    @Test
    void supportsNamespaceAndNamespacePurposeDimensions() {
        InMemoryIssueRateLimiter namespaceLimiter = InMemoryIssueRateLimiter.withPolicy(
                IssueRateLimitPolicy.of(rule(Set.of(IssueLimitDimension.NAMESPACE), 1, Duration.ofHours(1))));
        VerificationKey sameNamespace = new VerificationKey("account", "registration", "other@example.com");
        VerificationKey otherNamespace = new VerificationKey("payment", "login", KEY.subject());

        assertInstanceOf(IssueLimitResult.Allowed.class, namespaceLimiter.acquire(KEY, NOW));
        assertInstanceOf(IssueLimitResult.Throttled.class, namespaceLimiter.acquire(sameNamespace, NOW.plusSeconds(1)));
        assertInstanceOf(IssueLimitResult.Allowed.class, namespaceLimiter.acquire(otherNamespace, NOW.plusSeconds(1)));

        InMemoryIssueRateLimiter purposeLimiter = InMemoryIssueRateLimiter.withPolicy(IssueRateLimitPolicy.of(
                rule(Set.of(IssueLimitDimension.NAMESPACE, IssueLimitDimension.PURPOSE), 1, Duration.ofHours(1))));
        VerificationKey samePurpose = new VerificationKey("account", "login", "other@example.com");

        assertInstanceOf(IssueLimitResult.Allowed.class, purposeLimiter.acquire(KEY, NOW));
        assertInstanceOf(IssueLimitResult.Throttled.class, purposeLimiter.acquire(samePurpose, NOW.plusSeconds(1)));
        assertInstanceOf(IssueLimitResult.Allowed.class, purposeLimiter.acquire(sameNamespace, NOW.plusSeconds(1)));
    }

    @Test
    void supportsIpDeviceAndSessionDimensions() {
        InMemoryIssueRateLimiter limiter = InMemoryIssueRateLimiter.withPolicy(IssueRateLimitPolicy.of(
                rule(Set.of(IssueLimitDimension.IP_ADDRESS), 1, Duration.ofHours(1)),
                rule(Set.of(IssueLimitDimension.DEVICE_ID), 1, Duration.ofHours(1)),
                rule(Set.of(IssueLimitDimension.SESSION_ID), 1, Duration.ofHours(1))));
        IssueContext initial = context("ip-1", "device-1", "session-1");

        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(initial, NOW));
        assertInstanceOf(
                IssueLimitResult.Throttled.class,
                limiter.acquire(context("ip-1", "device-2", "session-2"), NOW.plusSeconds(1)));
        assertInstanceOf(
                IssueLimitResult.Throttled.class,
                limiter.acquire(context("ip-2", "device-1", "session-2"), NOW.plusSeconds(1)));
        assertInstanceOf(
                IssueLimitResult.Throttled.class,
                limiter.acquire(context("ip-2", "device-2", "session-1"), NOW.plusSeconds(1)));
        assertInstanceOf(
                IssueLimitResult.Allowed.class,
                limiter.acquire(context("ip-2", "device-2", "session-2"), NOW.plusSeconds(1)));
    }

    @Test
    void supportsCombinedAdditionalAndKeyDimensions() {
        InMemoryIssueRateLimiter limiter = InMemoryIssueRateLimiter.withPolicy(IssueRateLimitPolicy.of(
                rule(Set.of(IssueLimitDimension.IP_ADDRESS, IssueLimitDimension.PURPOSE), 1, Duration.ofHours(1))));
        VerificationKey registration = new VerificationKey("account", "registration", KEY.subject());
        VerificationKey otherSubject = new VerificationKey("account", "login", "other@example.com");

        assertInstanceOf(
                IssueLimitResult.Allowed.class,
                limiter.acquire(IssueContext.of(KEY).with(IssueLimitDimension.IP_ADDRESS, "ip-1"), NOW));
        assertInstanceOf(
                IssueLimitResult.Allowed.class,
                limiter.acquire(
                        IssueContext.of(registration).with(IssueLimitDimension.IP_ADDRESS, "ip-1"),
                        NOW.plusSeconds(1)));
        assertInstanceOf(
                IssueLimitResult.Throttled.class,
                limiter.acquire(
                        IssueContext.of(otherSubject).with(IssueLimitDimension.IP_ADDRESS, "ip-1"),
                        NOW.plusSeconds(1)));
    }

    @Test
    void failsWhenRequiredDimensionIsMissingWithoutConsumingQuota() {
        InMemoryIssueRateLimiter limiter = InMemoryIssueRateLimiter.withPolicy(
                IssueRateLimitPolicy.of(rule(Set.of(IssueLimitDimension.IP_ADDRESS), 1, Duration.ofHours(1))));

        assertThrows(IllegalArgumentException.class, () -> limiter.acquire(KEY, NOW));
        assertInstanceOf(
                IssueLimitResult.Allowed.class,
                limiter.acquire(IssueContext.of(KEY).with(IssueLimitDimension.IP_ADDRESS, "203.0.113.10"), NOW));
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(NullPointerException.class, () -> new InMemoryIssueRateLimiter(null));
        assertThrows(NullPointerException.class, () -> InMemoryIssueRateLimiter.withPolicy(null));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryIssueRateLimiter(Duration.ofSeconds(-1)));
        InMemoryIssueRateLimiter limiter = new InMemoryIssueRateLimiter();
        assertThrows(NullPointerException.class, () -> limiter.acquire((IssueContext) null, NOW));
        assertThrows(NullPointerException.class, () -> limiter.acquire(KEY, null));
    }

    private IssueContext context(String ipAddress, String deviceId, String sessionId) {
        return IssueContext.of(KEY)
                .with(IssueLimitDimension.IP_ADDRESS, ipAddress)
                .with(IssueLimitDimension.DEVICE_ID, deviceId)
                .with(IssueLimitDimension.SESSION_ID, sessionId);
    }

    private IssueRateLimitRule rule(Set<IssueLimitDimension> dimensions, int maxIssues, Duration window) {
        return new IssueRateLimitRule(dimensions, maxIssues, window);
    }
}
