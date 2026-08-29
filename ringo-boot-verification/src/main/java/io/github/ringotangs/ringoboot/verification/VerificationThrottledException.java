package io.github.ringotangs.ringoboot.verification;

import io.github.ringotangs.ringoboot.verification.limit.IssueLimitViolation;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * 同一验证键在签发限制周期内被重复签发时抛出的业务异常。
 */
public final class VerificationThrottledException extends VerificationException {

    /**
     * 再次签发验证码前需要等待的时间。
     */
    private final List<IssueLimitViolation> violations;

    /**
     * 使用剩余等待时间创建异常。
     *
     * @param violations 实际阻止本次签发的非空规则明细
     * @throws NullPointerException 当规则明细集合或任一元素为 {@code null} 时
     * @throws IllegalArgumentException 当规则明细为空或包含重复规则 ID 时
     */
    public VerificationThrottledException(List<IssueLimitViolation> violations) {
        super("Verification code issuance is throttled");
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
     * 返回实际阻止本次签发的规则明细。
     *
     * @return 不可变规则明细
     */
    public List<IssueLimitViolation> violations() {
        return violations;
    }

    /**
     * 返回再次签发前需要等待的时间。
     *
     * @return 剩余等待时间
     */
    public Duration retryAfter() {
        return violations.stream()
                .map(IssueLimitViolation::retryAfter)
                .max(Duration::compareTo)
                .orElseThrow();
    }
}
