package io.github.ringotangs.ringoboot.verification;

/**
 * 表示验证码校验结果，由存储返回，并在业务拒绝异常中保留失败原因。
 */
public enum VerifyResult {
    /**
     * 验证码匹配并已成功消费。
     */
    SUCCESS,

    /**
     * 未找到对应的验证码记录。
     */
    NOT_FOUND,

    /**
     * 验证码已过期并被删除。
     */
    EXPIRED,

    /**
     * 验证码不匹配，仍可继续尝试。
     */
    MISMATCH,

    /**
     * 校验次数已耗尽，验证码记录已被删除。
     */
    ATTEMPTS_EXHAUSTED
}
