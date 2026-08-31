package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import java.time.Duration;
import java.util.Objects;

/**
 * 表示 {@link IssueLimitManager} 为一次签发请求解析出的不可变签发配额。
 *
 * <p>配额是规则声明和当前额度桶的快照，限流状态存储不需要再次访问 {@link IssueContext} 或执行
 * {@link IssueLimitRule}。同一次请求的全部配额必须由 {@link IssueLimitStore} 原子处理。
 *
 * @param ruleId 产生该配额的稳定规则标识
 * @param bucket 本次请求在该规则下的额度桶
 * @param maxIssues 滚动窗口内允许签发的最大次数
 * @param window 滚动窗口长度
 */
public record IssueLimitQuota(String ruleId, IssueLimitBucket bucket, int maxIssues, Duration window) {

    /**
     * 创建并校验已解析的签发配额。
     *
     * @throws NullPointerException 当规则标识、额度桶或窗口为 {@code null} 时
     * @throws IllegalArgumentException 当规则标识格式非法、最大签发次数不为正数或窗口不为正数时
     */
    public IssueLimitQuota {
        Objects.requireNonNull(bucket, "bucket must not be null");
        IssueLimitValidator.validateRuleDefinition(ruleId, maxIssues, window);
    }
}
