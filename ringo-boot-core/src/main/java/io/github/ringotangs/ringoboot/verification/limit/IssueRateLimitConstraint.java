package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.Objects;

/** 管理器解析后提交给存储后端的不可变签发限流约束。 */
public record IssueRateLimitConstraint(String ruleId, IssueLimitBucket bucket, int maxIssues, Duration window) {

    /** 创建并校验已解析的限流约束。 */
    public IssueRateLimitConstraint {
        Objects.requireNonNull(bucket, "bucket must not be null");
        IssueRateLimitManager.validateRuleDefinition(ruleId, maxIssues, window);
    }
}
