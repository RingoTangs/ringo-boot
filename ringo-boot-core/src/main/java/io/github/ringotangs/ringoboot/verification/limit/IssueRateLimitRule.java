package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 定义一个验证码签发滚动窗口配额。
 *
 * <p>在请求时间 {@code requestedAt} 上，统计区间为
 * {@code (requestedAt - window, requestedAt]}；恰好位于左边界的历史签发已经退出窗口。
 *
 * @param dimensions 配额累计维度
 * @param maxIssues 窗口内允许签发的最大次数
 * @param window 滚动窗口长度
 */
public record IssueRateLimitRule(Set<IssueLimitDimension> dimensions, int maxIssues, Duration window) {

    /**
     * 创建并校验签发配额规则。
     *
     * @throws NullPointerException 当维度集合、任一维度或窗口为 {@code null} 时
     * @throws IllegalArgumentException 当维度集合为空，或者最大签发次数或窗口非法时
     */
    public IssueRateLimitRule {
        Objects.requireNonNull(dimensions, "dimensions must not be null");
        Objects.requireNonNull(window, "window must not be null");
        if (dimensions.isEmpty()) {
            throw new IllegalArgumentException("dimensions must not be empty");
        }
        dimensions = Collections.unmodifiableSet(EnumSet.copyOf(dimensions));
        if (maxIssues <= 0) {
            throw new IllegalArgumentException("maxIssues must be greater than 0: " + maxIssues);
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive: " + window);
        }
    }
}
