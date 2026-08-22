package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 基于进程内存的线程安全签发限流后端，仅适用于测试、开发和单实例应用。 */
public final class InMemoryIssueRateLimitBackend implements IssueRateLimitBackend {

    private static final long CLEANUP_INTERVAL = 256;
    private static final IssueLimitResult.Allowed ALLOWED = new IssueLimitResult.Allowed();

    private final Map<HistoryKey, History> histories = new HashMap<>();
    private long acquisitions;

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
