package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * 表示 {@link IssueRateLimiter} 尝试获取验证码签发名额的结果。
 *
 * <p>获得名额使用 {@link Allowed} 表达，正常达到额度上限使用 {@link Throttled} 表达。规则配置缺失和基础设施故障不属于正常结果，
 * 分别通过 {@link MissingIssueRateLimitRuleException} 和 {@link IssueRateLimitException} 表达。
 */
public sealed interface IssueLimitResult permits IssueLimitResult.Allowed, IssueLimitResult.Throttled {

    /**
     * 表示全部匹配配额均允许，并且已经消费本次验证码签发名额。
     *
     * <p>后续验证码生成、存储或派发失败时，该名额也不应退还。
     */
    record Allowed() implements IssueLimitResult {}

    /**
     * 表示当前请求受限，尚未获得签发名额。
     *
     * <p>该结果不表示永久拒绝。调用方可以将 {@code retryAfter} 转换为 HTTP {@code Retry-After} 或其他客户端可理解的等待提示。
     *
     * @param violations 实际阻止本次签发的非空规则明细
     */
    record Throttled(List<IssueLimitViolation> violations) implements IssueLimitResult {

        /**
         * 创建并校验限流结果。
         *
         * @throws NullPointerException     当规则明细集合或任一元素为 {@code null} 时
         * @throws IllegalArgumentException 当规则明细为空或包含重复规则 ID 时
         */
        public Throttled {
            Objects.requireNonNull(violations, "violations must not be null");
            violations = List.copyOf(violations);
            if (violations.isEmpty()) {
                throw new IllegalArgumentException("violations must not be empty");
            }
            var ruleIds = new HashSet<String>();
            for (IssueLimitViolation violation : violations) {
                if (!ruleIds.add(violation.ruleId())) {
                    throw new IllegalArgumentException("duplicate issue rate limit rule id: " + violation.ruleId());
                }
            }
        }

        /**
         * 返回距离全部受限规则再次允许签发的最长剩余时间。
         *
         * @return 最大剩余等待时间
         */
        public Duration retryAfter() {
            return violations.stream()
                    .map(IssueLimitViolation::retryAfter)
                    .max(Duration::compareTo)
                    .orElseThrow();
        }
    }
}
