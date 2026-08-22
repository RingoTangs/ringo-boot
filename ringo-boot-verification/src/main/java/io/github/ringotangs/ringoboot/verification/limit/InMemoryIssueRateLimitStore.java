package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于进程内存的线程安全签发限流状态存储。
 *
 * <p>每个 {@code ruleId + bucket} 对应一条按签发时间排序的历史队列。{@link #acquire(List, Instant)} 使用同步临界区完成所有
 * 窗口清理、额度检查和记录写入，因此单 JVM 内不会出现部分消费。
 *
 * <p><strong>使用限制：</strong>历史状态不会跨进程共享，也不会持久化。该实现仅适用于单元测试、本地开发和单实例应用；多实例生产
 * 环境应使用能够提供跨进程原子性的 Redis Store 或自定义 Store。
 */
public final class InMemoryIssueRateLimitStore implements IssueRateLimitStore {

    /** 每处理指定次数的额度获取后扫描并移除全部过期空桶。 */
    private static final long CLEANUP_INTERVAL = 256;

    /** 无状态的允许结果，可在成功获取额度时安全复用。 */
    private static final IssueLimitResult.Allowed ALLOWED = new IssueLimitResult.Allowed();

    /**
     * 按规则 ID 和额度桶内容保存滚动窗口历史。
     *
     * <p>{@link HistoryKey} 和 {@link IssueLimitBucket} 都是使用组件值实现相等判断的 record。后续请求即使创建新的 Key 和 Bucket
     * 对象，只要规则 ID 和 Bucket 分段内容相同，就会命中同一条历史记录，而不是按照对象地址创建新记录。
     */
    private final Map<HistoryKey, History> histories = new HashMap<>();

    /** 自实例创建以来处理的额度获取次数，用于触发周期性全局清理。 */
    private long acquisitions;

    /**
     * 创建一个初始不包含任何额度历史的内存限流 Store。
     *
     * <p>实例之间不共享状态，应用重启后历史也不会保留。
     */
    public InMemoryIssueRateLimitStore() {}

    /**
     * 在单个同步临界区内检查并消费全部签发配额。
     *
     * <p>实现会先清理每条配额窗口外的时间戳，再计算全部受限配额中的最大等待时间。任一配额受限时不会向任何历史队列写入当前时间；
     * 全部允许时才同时写入，从而保证单 JVM 内的全有或全无语义。
     *
     * @param quotas 本次请求需要同时满足的非空配额集合
     * @param requestedAt 请求签发的时间
     * @return 全部配额允许时返回 {@link IssueLimitResult.Allowed}，否则返回等待时间最大的
     *     {@link IssueLimitResult.Throttled}
     * @throws NullPointerException 当配额集合、任一配额或请求时间为 {@code null} 时
     * @throws IllegalArgumentException 当配额集合为空，或者同一规则 ID 在运行期间改变窗口时
     */
    @Override
    public synchronized IssueLimitResult acquire(List<IssueLimitQuota> quotas, Instant requestedAt) {
        Objects.requireNonNull(quotas, "quotas must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        if (quotas.isEmpty()) {
            throw new IllegalArgumentException("quotas must not be empty");
        }

        Map<HistoryKey, ArrayDeque<Instant>> evaluated = new HashMap<>();
        Duration retryAfter = Duration.ZERO;
        for (IssueLimitQuota quota : quotas) {
            Objects.requireNonNull(quota, "quota must not be null");
            HistoryKey key = new HistoryKey(quota.ruleId(), quota.bucket());
            History stored = histories.computeIfAbsent(key, ignored -> new History(quota.window()));
            if (!stored.window().equals(quota.window())) {
                throw new IllegalArgumentException("window changed for issue rate limit rule: " + quota.ruleId());
            }
            ArrayDeque<Instant> history = stored.timestamps();
            removeExpired(history, requestedAt.minus(quota.window()));
            evaluated.put(key, history);
            if (history.size() >= quota.maxIssues()) {
                Duration current =
                        Duration.between(requestedAt, history.getFirst().plus(quota.window()));
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

    /**
     * 周期性移除已经没有有效签发记录的额度桶，避免长期运行时保留无用桶。
     *
     * <p>当前请求涉及的桶在主流程中始终会清理；该扫描专门处理长期不再访问的历史桶，只影响内存占用，不改变限流语义。
     *
     * @param requestedAt 当前额度获取时间，用于计算每条历史的窗口边界
     */
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

    /**
     * 移除位于滚动窗口左边界及其之前的签发记录。
     *
     * @param history 按时间升序排列的签发历史队列
     * @param cutoff 当前滚动窗口左边界；等于该时刻的记录也视为过期
     */
    private static void removeExpired(ArrayDeque<Instant> history, Instant cutoff) {
        while (!history.isEmpty() && !history.getFirst().isAfter(cutoff)) {
            history.removeFirst();
        }
    }

    /**
     * 唯一标识一条规则下的一个额度历史桶。
     *
     * <p>record 自动生成的 {@code equals} 和 {@code hashCode} 按组件值比较，因此 HashMap 查找不依赖对象地址。
     *
     * @param ruleId 产生额度的稳定规则标识
     * @param bucket 由规则为当前请求解析出的额度桶
     */
    private record HistoryKey(String ruleId, IssueLimitBucket bucket) {}

    /**
     * 保存一条额度桶的固定窗口和仍位于窗口内的签发时间队列。
     *
     * @param window 创建历史时使用的滚动窗口，用于检测规则运行期间发生变化
     * @param timestamps 按签发时间升序排列的有效额度消费记录
     */
    private record History(Duration window, ArrayDeque<Instant> timestamps) {

        /**
         * 使用指定窗口创建空历史记录。
         *
         * @param window 当前规则声明的滚动窗口
         */
        private History(Duration window) {
            this(window, new ArrayDeque<>());
        }
    }
}
