package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * 表示验证码签发请求正常达到一条或多条限流规则的额度上限。
 */
public final class IssueRateLimitExceededException extends IssueRateLimitException {

    private final List<IssueLimitViolation> violations;

    /**
     * 使用实际阻止本次签发的规则明细创建异常。
     *
     * @param violations 非空限流规则明细
     * @throws NullPointerException     当规则明细集合或任一元素为 {@code null} 时
     * @throws IllegalArgumentException 当规则明细为空或包含重复规则 ID 时
     */
    public IssueRateLimitExceededException(List<IssueLimitViolation> violations) {
        super("Verification code issuance rate limit exceeded");
        Objects.requireNonNull(violations, "violations must not be null");
        this.violations = List.copyOf(violations);
        if (this.violations.isEmpty()) {
            throw new IllegalArgumentException("violations must not be empty");
        }
        var ruleIds = new HashSet<String>();
        for (IssueLimitViolation violation : this.violations) {
            if (!ruleIds.add(violation.ruleId())) {
                throw new IllegalArgumentException("duplicate issue rate limit rule id: " + violation.ruleId());
            }
        }
    }

    /**
     * @return 实际阻止本次签发的不可变规则明细
     */
    public List<IssueLimitViolation> violations() {
        return violations;
    }

    /**
     * @return 距离全部受限规则再次允许签发的最长剩余时间
     */
    public Duration retryAfter() {
        return violations.stream()
                .map(IssueLimitViolation::retryAfter)
                .max(Duration::compareTo)
                .orElseThrow();
    }
}
