package io.github.ringotangs.ringoboot.verification.sender;

/**
 * 表示发送端对验证码派发请求的判断结果。
 *
 * <p>Represents the sender's assessment of a verification-code delivery request.</p>
 */
public enum CodeSendResult {
    /** 供应商明确接受了发送请求。 / The provider explicitly accepted the delivery request. */
    ACCEPTED,

    /** 供应商明确拒绝了发送请求，确定不会发送。 / The provider explicitly rejected the request. */
    REJECTED,

    /** 无法确认供应商是否接受，例如请求超时或响应丢失。 / Provider acceptance is unknown. */
    UNKNOWN
}
