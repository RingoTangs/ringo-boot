package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.Objects;

/**
 * 统一校验签发限流规则定义中的公共字段。
 */
final class IssueLimitValidator {

    /**
     * 工具类不允许实例化。
     */
    private IssueLimitValidator() {}

    /**
     * 校验规则标识、最大签发次数和滚动窗口。
     *
     * @param id        规则标识
     * @param maxIssues 滚动窗口内允许签发的最大次数
     * @param window    滚动窗口长度
     * @throws NullPointerException     当规则标识或窗口为 {@code null} 时
     * @throws IllegalArgumentException 当规则标识格式非法、最大次数不为正数或窗口不为正数时
     */
    static void validateRuleDefinition(String id, int maxIssues, Duration window) {
        IssueLimitRuleId.validate("rule id", id);
        validateRuleDefinition(maxIssues, window);
    }

    /**
     * 校验内置规则自动生成标识以外的公共定义。
     *
     * @param maxIssues 滚动窗口内允许签发的最大次数
     * @param window    滚动窗口长度
     * @throws NullPointerException     当窗口为 {@code null} 时
     * @throws IllegalArgumentException 当最大次数不为正数或窗口不为正数时
     */
    static void validateRuleDefinition(int maxIssues, Duration window) {
        Objects.requireNonNull(window, "window must not be null");
        if (maxIssues <= 0) {
            throw new IllegalArgumentException("maxIssues must be greater than 0: " + maxIssues);
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive: " + window);
        }
    }
}
