package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 基于函数式匹配器和额度桶解析器的不可变签发限流规则。
 *
 * <p>该实现适合在 Spring 配置中通过 lambda 快速声明规则。传入的匹配器和解析器必须线程安全，且不得在执行过程中访问远程存储或
 * 修改共享状态。
 *
 * @param id 全局唯一的 kebab-case 规则标识
 * @param matcher 业务适用范围判断函数
 * @param bucketResolver 额度桶解析函数
 * @param maxIssues 滚动窗口内允许签发的最大次数
 * @param window 滚动窗口长度
 */
public record SimpleIssueRateLimitRule(
        String id,
        Predicate<IssueContext> matcher,
        Function<IssueContext, IssueLimitBucket> bucketResolver,
        int maxIssues,
        Duration window)
        implements IssueRateLimitRule {

    /**
     * 创建并校验简单规则。
     *
     * @throws NullPointerException 当规则标识、匹配器、额度桶解析器或窗口为 {@code null} 时
     * @throws IllegalArgumentException 当规则标识不是 kebab-case、最大签发次数不为正数或窗口不为正数时
     */
    public SimpleIssueRateLimitRule {
        Objects.requireNonNull(matcher, "matcher must not be null");
        Objects.requireNonNull(bucketResolver, "bucketResolver must not be null");
        IssueRateLimitValidator.validateRuleDefinition(id, maxIssues, window);
    }

    /**
     * 使用配置的匹配器判断规则是否适用。
     *
     * @param context 非空签发上下文
     * @return 匹配器的判断结果
     * @throws NullPointerException 当上下文为 {@code null} 时
     * @throws RuntimeException 当匹配器执行失败时
     */
    @Override
    public boolean matches(IssueContext context) {
        return matcher.test(Objects.requireNonNull(context, "context must not be null"));
    }

    /**
     * 使用配置的解析器计算额度桶。
     *
     * <p>解析器返回值的非空校验由 {@link IssueRateLimitManager} 统一执行。
     *
     * @param context 非空签发上下文
     * @return 解析器生成的额度桶
     * @throws NullPointerException 当上下文为 {@code null} 时
     * @throws RuntimeException 当额度桶解析器执行失败时
     */
    @Override
    public IssueLimitBucket bucket(IssueContext context) {
        return bucketResolver.apply(Objects.requireNonNull(context, "context must not be null"));
    }
}
