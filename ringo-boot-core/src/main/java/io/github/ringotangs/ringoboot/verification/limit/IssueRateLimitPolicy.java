package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 保存一次验证码签发需要同时满足的全部配额规则。
 *
 * <p>规则顺序保持不变。相同作用域和滚动窗口只能定义一次，空规则集合表示不限制签发频率。
 *
 * @param rules 不可变的签发配额规则集合
 */
public record IssueRateLimitPolicy(List<IssueRateLimitRule> rules) {

    /**
     * 创建并校验签发限流策略。
     *
     * @throws NullPointerException 当规则集合或任一规则为 {@code null} 时
     * @throws IllegalArgumentException 当存在相同作用域和窗口的重复规则时
     */
    public IssueRateLimitPolicy {
        Objects.requireNonNull(rules, "rules must not be null");
        rules = List.copyOf(rules);
        Set<RuleIdentity> identities = new HashSet<>();
        for (IssueRateLimitRule rule : rules) {
            RuleIdentity identity = new RuleIdentity(rule.scope(), rule.window());
            if (!identities.add(identity)) {
                throw new IllegalArgumentException(
                        "duplicate issue rate limit rule for scope " + rule.scope() + " and window " + rule.window());
            }
        }
    }

    /**
     * 使用给定规则创建签发限流策略。
     *
     * @param rules 签发配额规则
     * @return 签发限流策略
     */
    public static IssueRateLimitPolicy of(IssueRateLimitRule... rules) {
        return new IssueRateLimitPolicy(List.of(rules));
    }

    /**
     * 返回不限制签发频率的空策略。
     *
     * @return 空签发限流策略
     */
    public static IssueRateLimitPolicy none() {
        return new IssueRateLimitPolicy(List.of());
    }

    private record RuleIdentity(IssueLimitScope scope, Duration window) {}
}
