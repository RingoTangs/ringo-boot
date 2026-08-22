package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/** 基于函数式匹配器和额度桶解析器的不可变签发限流规则。 */
public record SimpleIssueRateLimitRule(
        String id,
        Predicate<IssueContext> matcher,
        Function<IssueContext, IssueLimitBucket> bucketResolver,
        int maxIssues,
        Duration window)
        implements IssueRateLimitRule {

    /** 创建并校验简单规则。 */
    public SimpleIssueRateLimitRule {
        Objects.requireNonNull(matcher, "matcher must not be null");
        Objects.requireNonNull(bucketResolver, "bucketResolver must not be null");
        IssueRateLimitManager.validateRuleDefinition(id, maxIssues, window);
    }

    /** {@inheritDoc} */
    @Override
    public boolean matches(IssueContext context) {
        return matcher.test(Objects.requireNonNull(context, "context must not be null"));
    }

    /** {@inheritDoc} */
    @Override
    public IssueLimitBucket bucket(IssueContext context) {
        return bucketResolver.apply(Objects.requireNonNull(context, "context must not be null"));
    }
}
