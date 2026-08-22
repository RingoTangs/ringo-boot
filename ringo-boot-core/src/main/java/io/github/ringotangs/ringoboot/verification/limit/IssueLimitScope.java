package io.github.ringotangs.ringoboot.verification.limit;

/** 定义验证码签发配额的累计范围。 */
public enum IssueLimitScope {

    /** 按命名空间、用途和验证主体组成的完整验证码键累计。 */
    VERIFICATION_KEY,

    /** 仅按验证主体累计，跨命名空间和用途共享配额。 */
    SUBJECT
}
