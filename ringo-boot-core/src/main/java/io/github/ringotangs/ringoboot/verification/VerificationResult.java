package io.github.ringotangs.ringoboot.verification;

/**
 * 表示一次验证码校验尝试的结果。
 *
 * <p>Represents the outcome of a verification attempt.</p>
 */
public enum VerificationResult {
    /**
     * 验证码匹配并已成功消费。
     *
     * <p>The code matched and was consumed successfully.</p>
     */
    SUCCESS,

    /**
     * 未找到对应的验证码记录。
     *
     * <p>No matching verification record was found.</p>
     */
    NOT_FOUND,

    /**
     * 验证码已过期并被删除。
     *
     * <p>The code expired and its record was removed.</p>
     */
    EXPIRED,

    /**
     * 验证码不匹配，仍可继续尝试。
     *
     * <p>The code did not match and more attempts remain.</p>
     */
    MISMATCH,

    /**
     * 校验次数已耗尽，验证码记录已被删除。
     *
     * <p>All attempts were exhausted and the record was removed.</p>
     */
    ATTEMPTS_EXHAUSTED
}
