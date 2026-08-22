package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

/** 声明一条验证码签发窗口配额规则。规则实现必须无状态且线程安全。 */
public interface IssueRateLimitRule {

    /** 返回全局唯一且稳定的 kebab-case 规则标识。 */
    String id();

    /** 判断规则是否适用于本次签发请求。 */
    default boolean matches(IssueContext context) {
        return true;
    }

    /** 从签发上下文解析本规则的额度桶。 */
    IssueLimitBucket bucket(IssueContext context);

    /** 返回滚动窗口内允许签发的最大次数。 */
    int maxIssues();

    /** 返回滚动窗口长度。 */
    Duration window();

    /** 创建对所有请求生效的简单规则。 */
    static IssueRateLimitRule of(
            String id, Function<IssueContext, IssueLimitBucket> bucketResolver, int maxIssues, Duration window) {
        return new SimpleIssueRateLimitRule(id, context -> true, bucketResolver, maxIssues, window);
    }

    /** 创建带业务匹配条件的简单规则。 */
    static IssueRateLimitRule of(
            String id,
            Predicate<IssueContext> matcher,
            Function<IssueContext, IssueLimitBucket> bucketResolver,
            int maxIssues,
            Duration window) {
        return new SimpleIssueRateLimitRule(id, matcher, bucketResolver, maxIssues, window);
    }
}
