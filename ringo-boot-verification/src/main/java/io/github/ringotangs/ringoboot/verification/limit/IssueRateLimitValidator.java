package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/** 统一校验签发限流规则定义中的公共字段。 */
final class IssueRateLimitValidator {

    /** 规则 ID 格式：仅允许小写字母、数字及分隔单词的单个连字符。 */
    private static final Pattern RULE_ID_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    /** 工具类不允许实例化。 */
    private IssueRateLimitValidator() {}

    /**
     * 校验规则标识、最大签发次数和滚动窗口。
     *
     * @param id 规则标识
     * @param maxIssues 滚动窗口内允许签发的最大次数
     * @param window 滚动窗口长度
     * @throws NullPointerException 当规则标识或窗口为 {@code null} 时
     * @throws IllegalArgumentException 当规则标识不是 kebab-case、最大次数不为正数或窗口不为正数时
     */
    static void validateRuleDefinition(String id, int maxIssues, Duration window) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(window, "window must not be null");
        if (!RULE_ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("rule id must be kebab-case: " + id);
        }
        if (maxIssues <= 0) {
            throw new IllegalArgumentException("maxIssues must be greater than 0: " + maxIssues);
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive: " + window);
        }
    }
}
