package io.github.ringotangs.ringoboot.verification;

import java.time.Duration;
import java.util.Objects;

/**
 * 同一验证键在重发间隔内被重复签发时抛出的业务异常。
 *
 * <p>Business exception thrown when the same verification key is issued again during its resend
 * interval.</p>
 */
public final class VerificationThrottledException extends RuntimeException {

    /** 再次签发验证码前需要等待的时间。 / Time to wait before issuing another code. */
    private final Duration retryAfter;

    /**
     * 使用剩余等待时间创建异常。
     *
     * <p>Creates the exception with the remaining wait duration.</p>
     *
     * @param retryAfter 再次签发前需要等待的时间 / time to wait before issuing again
     * @throws NullPointerException 当等待时间为 {@code null} 时 / if the duration is {@code null}
     * @throws IllegalArgumentException 当等待时间为负数时 / if the duration is negative
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
     * <p>Returns the time to wait before issuing again.</p>
     *
     * @return 剩余等待时间 / remaining wait duration
     */
    public Duration retryAfter() {
        return retryAfter;
    }
}
