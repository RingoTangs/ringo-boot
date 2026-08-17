package io.github.ringotangs.ringoboot.verification.sender;

/**
 * 表示发送端对验证码派发请求的判断结果。
 *
 */
public enum CodeSendResult {
    /** 供应商明确接受了发送请求。 */
    ACCEPTED,

    /** 供应商明确拒绝了发送请求，确定不会发送。 */
    REJECTED,

    /** 无法确认供应商是否接受，例如请求超时或响应丢失。 */
    UNKNOWN
}
