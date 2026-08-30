package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.core.KebabCase;
import java.time.Duration;
import java.util.Objects;

/**
 * 统一校验签发限流规则定义中的公共字段。
 */
final class IssueRateLimitValidator {

    /**
     * 工具类不允许实例化。
     */
    private IssueRateLimitValidator() {}

    /**
     * 校验规则标识、最大签发次数和滚动窗口。
     *
     * @param id        规则标识
     * @param maxIssues 滚动窗口内允许签发的最大次数
     * @param window    滚动窗口长度
     * @throws NullPointerException     当规则标识或窗口为 {@code null} 时
     * @throws IllegalArgumentException 当规则标识不是 kebab-case、最大次数不为正数或窗口不为正数时
     */
    static void validateRuleDefinition(String id, int maxIssues, Duration window) {
        KebabCase.validate("rule id", id);
        Objects.requireNonNull(window, "window must not be null");
        if (maxIssues <= 0) {
            throw new IllegalArgumentException("maxIssues must be greater than 0: " + maxIssues);
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive: " + window);
        }
    }
}
