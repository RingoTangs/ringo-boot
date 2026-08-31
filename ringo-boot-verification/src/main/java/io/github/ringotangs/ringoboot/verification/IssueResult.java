package io.github.ringotangs.ringoboot.verification;

import io.github.ringotangs.ringoboot.verification.limit.IssueLimitViolation;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * 表示验证码签发及交付流程的安全结果，不包含明文验证码。
 */
public sealed interface IssueResult permits IssueResult.Accepted, IssueResult.Uncertain, IssueResult.Throttled {

    /**
     * 表示验证码已成功签发，并且发送供应商明确接受了请求。
     *
     * @param expiresAt 验证码过期时间
     */
    record Accepted(Instant expiresAt) implements IssueResult {

        /**
         * 创建并校验渠道已受理的签发结果。
         *
         * @throws NullPointerException 当过期时间为 {@code null} 时
         */
        public Accepted {
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }
    }

    /**
     * 表示验证码已成功签发，但无法确认发送供应商是否接受请求。
     *
     * @param expiresAt 验证码过期时间
     */
    record Uncertain(Instant expiresAt) implements IssueResult {

        /**
         * 创建并校验渠道受理状态不确定的签发结果。
         */
        public Uncertain {
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }
    }

    /**
     * 表示验证码因签发频率限制而未签发或交付。
     *
     * @param violations 实际阻止本次签发的非空规则明细
     */
    record Throttled(List<IssueLimitViolation> violations) implements IssueResult {

        /**
         * 创建并校验受限流的签发结果。
         *
         * @throws NullPointerException     当规则明细集合或任一元素为 {@code null} 时
         * @throws IllegalArgumentException 当规则明细为空或包含重复规则 ID 时
         */
        @SuppressWarnings("DuplicatedCode")
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
