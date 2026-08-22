package io.github.ringotangs.ringoboot.verification.limit;

/** 定义验证码签发配额可以使用的累计维度。 */
public enum IssueLimitDimension {

    /** 验证码键中的业务命名空间。 */
    NAMESPACE,

    /** 验证码键中的验证用途。 */
    PURPOSE,

    /** 验证码键中的手机号、邮箱或其他验证主体。 */
    SUBJECT,

    /** 调用方解析并规范化后的来源 IP 地址。 */
    IP_ADDRESS,

    /** 调用方提供的稳定设备标识。 */
    DEVICE_ID,

    /** 调用方提供的会话标识。 */
    SESSION_ID
}
