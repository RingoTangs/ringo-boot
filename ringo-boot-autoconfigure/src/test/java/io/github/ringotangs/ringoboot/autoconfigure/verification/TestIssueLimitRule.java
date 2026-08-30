package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitRule;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/** 仅供自动配置测试复用的签发限流规则。 */
record TestIssueLimitRule(
        String id,
        Predicate<IssueContext> matcher,
        Function<IssueContext, IssueLimitBucket> bucketResolver,
        int maxIssues,
        Duration window)
        implements IssueLimitRule {

    TestIssueLimitRule {
        Objects.requireNonNull(matcher, "matcher must not be null");
        Objects.requireNonNull(bucketResolver, "bucketResolver must not be null");
    }

    TestIssueLimitRule(
            String id, Function<IssueContext, IssueLimitBucket> bucketResolver, int maxIssues, Duration window) {
        this(id, context -> true, bucketResolver, maxIssues, window);
    }

    @Override
    public boolean matches(IssueContext context) {
        return matcher.test(Objects.requireNonNull(context, "context must not be null"));
    }

    @Override
    public IssueLimitBucket bucket(IssueContext context) {
        return bucketResolver.apply(Objects.requireNonNull(context, "context must not be null"));
    }
}
