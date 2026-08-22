package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于进程内存的线程安全签发限流后端。
 *
 * <p>每个 {@code ruleId + bucket} 对应一条按签发时间排序的历史队列。{@link #acquire(List, Instant)} 使用同步临界区完成所有
 * 窗口清理、额度检查和记录写入，因此单 JVM 内不会出现部分消费。
 *
 * <p><strong>使用限制：</strong>历史状态不会跨进程共享，也不会持久化。该实现仅适用于单元测试、本地开发和单实例应用；多实例生产
 * 环境应使用能够提供跨进程原子性的 Redis 后端或自定义后端。
 */
public final class InMemoryIssueRateLimitBackend implements IssueRateLimitBackend {

    private static final long CLEANUP_INTERVAL = 256;
    private static final IssueLimitResult.Allowed ALLOWED = new IssueLimitResult.Allowed();

    private final Map<HistoryKey, History> histories = new HashMap<>();
    private long acquisitions;

    /** 创建一个初始不包含任何额度历史的内存限流后端。 */
    public InMemoryIssueRateLimitBackend() {}

    /** {@inheritDoc} */
    @Override
    public synchronized IssueLimitResult acquire(List<IssueRateLimitConstraint> constraints, Instant requestedAt) {
        Objects.requireNonNull(constraints, "constraints must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        if (constraints.isEmpty()) {
            return ALLOWED;
        }

        Map<HistoryKey, ArrayDeque<Instant>> evaluated = new HashMap<>();
        Duration retryAfter = Duration.ZERO;
        for (IssueRateLimitConstraint constraint : constraints) {
            Objects.requireNonNull(constraint, "constraint must not be null");
            HistoryKey key = new HistoryKey(constraint.ruleId(), constraint.bucket());
            History stored = histories.computeIfAbsent(key, ignored -> new History(constraint.window()));
            if (!stored.window().equals(constraint.window())) {
                throw new IllegalArgumentException("window changed for issue rate limit rule: " + constraint.ruleId());
            }
            ArrayDeque<Instant> history = stored.timestamps();
            removeExpired(history, requestedAt.minus(constraint.window()));
            evaluated.put(key, history);
            if (history.size() >= constraint.maxIssues()) {
                Duration current =
                        Duration.between(requestedAt, history.getFirst().plus(constraint.window()));
                if (current.compareTo(retryAfter) > 0) {
                    retryAfter = current;
                }
            }
        }

        if (!retryAfter.isZero()) {
            cleanup(requestedAt);
            return new IssueLimitResult.Throttled(retryAfter);
        }
        evaluated.values().forEach(history -> history.addLast(requestedAt));
        cleanup(requestedAt);
        return ALLOWED;
    }

    /** 周期性移除已经没有有效签发记录的额度桶，避免长期运行时保留无用桶。 */
    private void cleanup(Instant requestedAt) {
        if (++acquisitions % CLEANUP_INTERVAL == 0) {
            histories.entrySet().removeIf(entry -> {
                removeExpired(
                        entry.getValue().timestamps(),
                        requestedAt.minus(entry.getValue().window()));
                return entry.getValue().timestamps().isEmpty();
            });
        }
    }

    /** 移除位于滚动窗口左边界及其之前的签发记录。 */
    private static void removeExpired(ArrayDeque<Instant> history, Instant cutoff) {
        while (!history.isEmpty() && !history.getFirst().isAfter(cutoff)) {
            history.removeFirst();
        }
    }

    private record HistoryKey(String ruleId, IssueLimitBucket bucket) {}

    private record History(Duration window, ArrayDeque<Instant> timestamps) {
        private History(Duration window) {
            this(window, new ArrayDeque<>());
        }
    }
}
