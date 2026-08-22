package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.Objects;

/**
 * 定义一个验证码签发滚动窗口配额。
 *
 * <p>在请求时间 {@code requestedAt} 上，统计区间为
 * {@code (requestedAt - window, requestedAt]}；恰好位于左边界的历史签发已经退出窗口。
 *
 * @param scope 配额累计范围
 * @param maxIssues 窗口内允许签发的最大次数
 * @param window 滚动窗口长度
 */
public record IssueRateLimitRule(IssueLimitScope scope, int maxIssues, Duration window) {

    /**
     * 创建并校验签发配额规则。
     *
     * @throws NullPointerException 当作用域或窗口为 {@code null} 时
     * @throws IllegalArgumentException 当最大签发次数或窗口非法时
     */
    public IssueRateLimitRule {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(window, "window must not be null");
        if (maxIssues <= 0) {
            throw new IllegalArgumentException("maxIssues must be greater than 0: " + maxIssues);
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive: " + window);
        }
    }
}
