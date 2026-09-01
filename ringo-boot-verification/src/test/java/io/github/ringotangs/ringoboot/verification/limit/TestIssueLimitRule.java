package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/** 仅供 verification 模块测试复用的签发限流规则。 */
public record TestIssueLimitRule(
        String id,
        Predicate<IssueContext> applicability,
        Function<IssueContext, IssueLimitBucket> bucketResolver,
        int maxIssues,
        Duration window)
        implements IssueLimitRule {

    public TestIssueLimitRule {
        Objects.requireNonNull(applicability, "applicability must not be null");
        Objects.requireNonNull(bucketResolver, "bucketResolver must not be null");
    }

    public TestIssueLimitRule(
            String id, Function<IssueContext, IssueLimitBucket> bucketResolver, int maxIssues, Duration window) {
        this(id, context -> true, bucketResolver, maxIssues, window);
    }

    @Override
    public boolean appliesTo(IssueContext context) {
        return applicability.test(Objects.requireNonNull(context, "context must not be null"));
    }

    @Override
    public IssueLimitBucket bucket(IssueContext context) {
        return bucketResolver.apply(Objects.requireNonNull(context, "context must not be null"));
    }
}
