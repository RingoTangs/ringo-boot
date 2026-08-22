package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.Objects;

/**
 * 对限流状态存储隔离范围内的全部验证码签发请求应用同一个额度桶。
 *
 * <p>使用内存 Store 时，全局范围为当前 JVM；使用分布式 Store 时，全局范围由 Store 的应用隔离策略决定。例如 Redis Store
 * 在同一应用名称下的全部实例共享额度。
 *
 * @param id 全局唯一的规则标识
 * @param maxIssues 滚动窗口内允许签发的最大次数
 * @param window 滚动窗口长度
 */
public record GlobalIssueRateLimitRule(String id, int maxIssues, Duration window) implements IssueRateLimitRule {

    /** 所有签发请求共享的固定额度桶；具体全局范围由 Store 的隔离策略决定。 */
    private static final IssueLimitBucket GLOBAL_BUCKET = IssueLimitBucket.of("global");

    /**
     * 创建并校验应用全局签发限流规则。
     *
     * @throws NullPointerException 当规则标识或窗口为 {@code null} 时
     * @throws IllegalArgumentException 当规则标识、最大签发次数或窗口非法时
     */
    public GlobalIssueRateLimitRule {
        IssueRateLimitValidator.validateRuleDefinition(id, maxIssues, window);
    }

    /**
     * 返回所有签发请求共享的全局额度桶。
     *
     * @param context 非空签发上下文；仅用于校验调用契约，不参与分桶
     * @return 固定的全局额度桶
     * @throws NullPointerException 当上下文为 {@code null} 时
     */
    @Override
    public IssueLimitBucket bucket(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return GLOBAL_BUCKET;
    }
}
