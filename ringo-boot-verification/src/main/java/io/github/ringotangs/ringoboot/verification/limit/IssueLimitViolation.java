package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.core.KebabCase;
import java.time.Duration;
import java.util.Objects;

/**
 * 表示一条实际阻止验证码签发的限流规则。
 *
 * @param ruleId     稳定的限流规则标识
 * @param retryAfter 该规则再次允许签发前的剩余时间
 */
public record IssueLimitViolation(String ruleId, Duration retryAfter) {

    /**
     * 创建并校验限流规则明细。
     *
     * @throws NullPointerException     当规则标识或等待时间为 {@code null} 时
     * @throws IllegalArgumentException 当规则标识不是 kebab-case 或等待时间为负数时
     */
    public IssueLimitViolation {
        KebabCase.validate("ruleId", ruleId);
        Objects.requireNonNull(retryAfter, "retryAfter must not be null");
        if (retryAfter.isNegative()) {
            throw new IllegalArgumentException("retryAfter must not be negative: " + retryAfter);
        }
    }
}
