package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于进程内存的线程安全验证码签发限流器。
 *
 * <p><strong>API 注意事项：</strong>仅适用于测试、本地开发和单实例应用，状态不会跨实例共享。
 */
public final class InMemoryIssueRateLimiter implements IssueRateLimiter {

    private static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(60);
    private static final long CLEANUP_INTERVAL = 256;
    private static final IssueLimitResult.Allowed ALLOWED = new IssueLimitResult.Allowed();

    private final ConcurrentMap<VerificationKey, Instant> nextAllowedTimes = new ConcurrentHashMap<>();
    private final AtomicLong acquisitions = new AtomicLong();
    private final Duration interval;

    /** 使用默认的 60 秒签发间隔创建限流器。 */
    public InMemoryIssueRateLimiter() {
        this(DEFAULT_INTERVAL);
    }

    /**
     * 使用指定签发间隔创建限流器。
     *
     * @param interval 同一验证码键的最小签发间隔，零表示不限制
     * @throws NullPointerException 当签发间隔为 {@code null} 时
     * @throws IllegalArgumentException 当签发间隔为负数时
     */
    public InMemoryIssueRateLimiter(Duration interval) {
        this.interval = Objects.requireNonNull(interval, "interval must not be null");
        if (interval.isNegative()) {
            throw new IllegalArgumentException("interval must not be negative: " + interval);
        }
    }

    /** {@inheritDoc} */
    @Override
    public IssueLimitResult acquire(VerificationKey key, Instant requestedAt) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        if (interval.isZero()) {
            return ALLOWED;
        }
        AtomicReference<IssueLimitResult> result = new AtomicReference<>();
        nextAllowedTimes.compute(key, (ignored, nextAllowedAt) -> {
            if (nextAllowedAt != null && requestedAt.isBefore(nextAllowedAt)) {
                result.set(new IssueLimitResult.Throttled(Duration.between(requestedAt, nextAllowedAt)));
                return nextAllowedAt;
            }
            result.set(ALLOWED);
            return requestedAt.plus(interval);
        });
        cleanupExpiredEntries(requestedAt);
        return Objects.requireNonNull(result.get());
    }

    private void cleanupExpiredEntries(Instant requestedAt) {
        if (acquisitions.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            nextAllowedTimes.entrySet().removeIf(entry -> !requestedAt.isBefore(entry.getValue()));
        }
    }
}
