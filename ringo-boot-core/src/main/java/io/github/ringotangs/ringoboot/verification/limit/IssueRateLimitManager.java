package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 收集、匹配和解析签发限流规则，并统一提交给限流状态存储。
 *
 * <p>管理器在构造时复制规则集合，并校验规则定义和 ID 唯一性。每次获取名额时，先执行所有规则的
 * {@link IssueRateLimitRule#matches(IssueContext)}，再为匹配规则解析 {@link IssueLimitBucket}。只有全部额度桶成功解析后，才会把
 * 不可变 {@link IssueLimitQuota} 集合一次性提交给 {@link IssueRateLimitStore}。
 *
 * <p>没有规则匹配时直接返回 {@link IssueLimitResult.Allowed}，不会访问状态存储。该类本身不可变；整体线程安全性还依赖规则实现和存储
 * 满足各自的线程安全契约。
 */
public final class IssueRateLimitManager implements IssueRateLimiter {

    private static final IssueLimitResult.Allowed ALLOWED = new IssueLimitResult.Allowed();

    private final List<IssueRateLimitRule> rules;
    private final IssueRateLimitStore store;

    /**
     * 使用规则集合和限流状态存储创建管理器。
     *
     * <p>规则的迭代顺序会被保留，便于获得稳定的匹配和诊断顺序，但所有匹配规则仍以 AND 关系原子执行，顺序不会改变限流结果。
     *
     * @param rules 需要管理的签发限流规则；允许传入空集合
     * @param store 原子保存和消费已解析配额的限流状态存储
     * @throws NullPointerException 当规则集合、任一规则、规则定义字段或 Store 为 {@code null} 时
     * @throws IllegalArgumentException 当规则定义非法或存在重复规则 ID 时
     */
    public IssueRateLimitManager(List<IssueRateLimitRule> rules, IssueRateLimitStore store) {
        Objects.requireNonNull(rules, "rules must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.rules = List.copyOf(rules);
        Set<String> ids = new HashSet<>();
        for (IssueRateLimitRule rule : this.rules) {
            Objects.requireNonNull(rule, "rule must not be null");
            IssueRateLimitValidator.validateRuleDefinition(rule.id(), rule.maxIssues(), rule.window());
            if (!ids.add(rule.id())) {
                throw new IllegalArgumentException("duplicate issue rate limit rule id: " + rule.id());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public IssueLimitResult acquire(IssueContext context, Instant requestedAt) throws IssueRateLimitException {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        List<IssueLimitQuota> quotas = new ArrayList<>();
        for (IssueRateLimitRule rule : rules) {
            if (rule.matches(context)) {
                IssueLimitBucket bucket = Objects.requireNonNull(
                        rule.bucket(context), "issue rate limit rule bucket must not be null: " + rule.id());
                quotas.add(new IssueLimitQuota(rule.id(), bucket, rule.maxIssues(), rule.window()));
            }
        }
        if (quotas.isEmpty()) {
            return ALLOWED;
        }
        return Objects.requireNonNull(
                store.acquire(List.copyOf(quotas), requestedAt), "issue rate limit store result must not be null");
    }
}
