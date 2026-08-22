package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 基于进程内存的线程安全验证码签发限流器。
 *
 * <p><strong>API 注意事项：</strong>仅适用于测试、本地开发和单实例应用，状态不会跨实例共享。
 */
public final class InMemoryIssueRateLimiter implements IssueRateLimiter {

    private static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(60);
    private static final long CLEANUP_INTERVAL = 256;
    private static final IssueLimitResult.Allowed ALLOWED = new IssueLimitResult.Allowed();

    private final Map<HistoryKey, ArrayDeque<Instant>> histories = new HashMap<>();
    private final IssueRateLimitPolicy policy;
    private long acquisitions;

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
        Objects.requireNonNull(interval, "interval must not be null");
        if (interval.isNegative()) {
            throw new IllegalArgumentException("interval must not be negative: " + interval);
        }
        this.policy = interval.isZero() ? IssueRateLimitPolicy.none() : singleKeyInterval(interval);
    }

    private InMemoryIssueRateLimiter(IssueRateLimitPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    /**
     * 使用多周期签发限流策略创建限流器。
     *
     * @param policy 需要同时满足的签发限流策略
     * @return 使用给定策略的进程内签发限流器
     * @throws NullPointerException 当策略为 {@code null} 时
     */
    public static InMemoryIssueRateLimiter withPolicy(IssueRateLimitPolicy policy) {
        return new InMemoryIssueRateLimiter(policy);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized IssueLimitResult acquire(VerificationKey key, Instant requestedAt) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        if (policy.rules().isEmpty()) {
            return ALLOWED;
        }

        Map<HistoryKey, ArrayDeque<Instant>> evaluatedHistories = new HashMap<>();
        Duration retryAfter = Duration.ZERO;
        boolean throttled = false;
        for (IssueRateLimitRule rule : policy.rules()) {
            HistoryKey historyKey = new HistoryKey(rule, identity(rule.scope(), key));
            ArrayDeque<Instant> history = histories.computeIfAbsent(historyKey, ignored -> new ArrayDeque<>());
            removeExpired(history, requestedAt.minus(rule.window()));
            evaluatedHistories.put(historyKey, history);
            if (history.size() >= rule.maxIssues()) {
                Duration ruleRetryAfter =
                        Duration.between(requestedAt, history.getFirst().plus(rule.window()));
                if (ruleRetryAfter.compareTo(retryAfter) > 0) {
                    retryAfter = ruleRetryAfter;
                }
                throttled = true;
            }
        }

        if (throttled) {
            cleanupExpiredEntries(requestedAt);
            return new IssueLimitResult.Throttled(retryAfter);
        }
        evaluatedHistories.values().forEach(history -> history.addLast(requestedAt));
        cleanupExpiredEntries(requestedAt);
        return ALLOWED;
    }

    private void cleanupExpiredEntries(Instant requestedAt) {
        if (++acquisitions % CLEANUP_INTERVAL == 0) {
            histories.entrySet().removeIf(entry -> {
                removeExpired(
                        entry.getValue(),
                        requestedAt.minus(entry.getKey().rule().window()));
                return entry.getValue().isEmpty();
            });
        }
    }

    private static IssueRateLimitPolicy singleKeyInterval(Duration interval) {
        return IssueRateLimitPolicy.of(new IssueRateLimitRule(IssueLimitScope.VERIFICATION_KEY, 1, interval));
    }

    private static Object identity(IssueLimitScope scope, VerificationKey key) {
        return switch (scope) {
            case VERIFICATION_KEY -> key;
            case SUBJECT -> key.subject();
        };
    }

    private static void removeExpired(ArrayDeque<Instant> history, Instant cutoff) {
        while (!history.isEmpty() && !history.getFirst().isAfter(cutoff)) {
            history.removeFirst();
        }
    }

    private record HistoryKey(IssueRateLimitRule rule, Object identity) {}
}
