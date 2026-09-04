package io.github.ringotangs.ringoboot.verification.store;

/** 表示验证码存储执行一次原子校验与消费操作的结果。 */
public enum VerifyResult {
    /** 验证码匹配并已成功消费。 */
    SUCCESS,

    /** 未找到对应的验证码记录。 */
    NOT_FOUND,

    /** 验证码已过期并被删除。 */
    EXPIRED,

    /** 验证码不匹配，仍可继续尝试。 */
    MISMATCH,

    /** 校验次数已耗尽，验证码记录已被删除。 */
    ATTEMPTS_EXHAUSTED
}
