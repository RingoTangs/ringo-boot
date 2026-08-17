package io.github.ringotangs.ringoboot.verification;

import java.time.Duration;
import java.util.Objects;

/**
 * 同一验证键在签发限制周期内被重复签发时抛出的业务异常。
 *
 */
public final class VerificationThrottledException extends RuntimeException {

    /** 再次签发验证码前需要等待的时间。 */
    private final Duration retryAfter;

    /**
     * 使用剩余等待时间创建异常。
     *
     *
     * @param retryAfter 再次签发前需要等待的时间
     * @throws NullPointerException 当等待时间为 {@code null} 时
     * @throws IllegalArgumentException 当等待时间为负数时
     */
    public VerificationThrottledException(Duration retryAfter) {
        super("Verification code issuance is throttled");
        this.retryAfter = Objects.requireNonNull(retryAfter, "retryAfter must not be null");
        if (retryAfter.isNegative()) {
            throw new IllegalArgumentException("retryAfter must not be negative: " + retryAfter);
        }
    }

    /**
     * 返回再次签发前需要等待的时间。
     *
     *
     * @return 剩余等待时间
     */
    public Duration retryAfter() {
        return retryAfter;
    }
}
