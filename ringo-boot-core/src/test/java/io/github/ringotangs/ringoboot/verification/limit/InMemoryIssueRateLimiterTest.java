package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    void rejectsInvalidArguments() {
        assertThrows(NullPointerException.class, () -> new InMemoryIssueRateLimiter(null));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryIssueRateLimiter(Duration.ofSeconds(-1)));
        InMemoryIssueRateLimiter limiter = new InMemoryIssueRateLimiter();
        assertThrows(NullPointerException.class, () -> limiter.acquire(null, NOW));
        assertThrows(NullPointerException.class, () -> limiter.acquire(KEY, null));
    }
}
