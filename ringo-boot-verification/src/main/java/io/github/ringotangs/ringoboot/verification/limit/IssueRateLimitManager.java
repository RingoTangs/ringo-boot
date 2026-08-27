package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.IssueContext;
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
 * <p>规则集合为空或没有规则匹配当前验证码键时使用严格拒绝策略，抛出 {@link MissingIssueRateLimitRuleException}，不会访问状态
 * 存储。该类本身不可变；整体线程安全性还依赖规则实现和存储满足各自的线程安全契约。
 */
public final class IssueRateLimitManager implements IssueRateLimiter {

    /** 启动时复制并校验完成的不可变规则快照。 */
    private final List<IssueRateLimitRule> rules;

    /** 原子检查并消费已解析签发配额的状态存储。 */
    private final IssueRateLimitStore store;

    /**
     * 使用规则集合和限流状态存储创建管理器。
     *
     * <p>规则的迭代顺序会被保留，便于获得稳定的匹配和诊断顺序，但所有匹配规则仍以 AND 关系原子执行，顺序不会改变限流结果。
     *
     * @param rules 需要管理的非空签发限流规则集合
     * @param store 原子保存和消费已解析配额的限流状态存储
     * @throws NullPointerException 当规则集合、任一规则、规则定义字段或 Store 为 {@code null} 时
     * @throws IllegalArgumentException 当规则定义非法或存在重复规则 ID 时
     * @throws MissingIssueRateLimitRuleException 当规则集合为空时
     */
    public IssueRateLimitManager(List<IssueRateLimitRule> rules, IssueRateLimitStore store) {
        Objects.requireNonNull(rules, "rules must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.rules = List.copyOf(rules);
        if (this.rules.isEmpty()) {
            throw new MissingIssueRateLimitRuleException();
        }
        Set<String> ids = new HashSet<>();
        for (IssueRateLimitRule rule : this.rules) {
            Objects.requireNonNull(rule, "rule must not be null");
            IssueRateLimitValidator.validateRuleDefinition(rule.id(), rule.maxIssues(), rule.window());
            if (!ids.add(rule.id())) {
                throw new IllegalArgumentException("duplicate issue rate limit rule id: " + rule.id());
            }
        }
    }

    /**
     * 使用签发上下文收集全部匹配规则并原子获取一次签发名额。
     *
     * <p>该方法先解析所有匹配规则的额度桶，只有全部额度桶均成功解析后才调用 Store。没有规则匹配时严格拒绝，Store 返回结果为空
     * 也视为实现违反契约。
     *
     * @param context 当前签发流程的上下文
     * @param requestedAt 请求签发的时间
     * @return Store 返回的允许或受限结果
     * @throws NullPointerException 当任一参数、规则返回的额度桶或 Store 返回结果为 {@code null} 时
     * @throws IllegalArgumentException 当规则生成的配额非法时
     * @throws MissingIssueRateLimitRuleException 当没有规则覆盖当前验证码键时
     * @throws RuntimeException 当规则匹配或额度桶解析失败时
     * @throws IssueRateLimitException 当底层限流状态操作失败时
     */
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
            throw new MissingIssueRateLimitRuleException(context.key());
        }
        return Objects.requireNonNull(
                store.acquire(List.copyOf(quotas), requestedAt), "issue rate limit store result must not be null");
    }
}
