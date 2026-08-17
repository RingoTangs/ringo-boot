package io.github.ringotangs.ringoboot.verification.sender;

/**
 * 供应商明确拒绝验证码发送请求时抛出的异常。
 *
 * <p>Exception thrown when the provider explicitly rejects a verification-code delivery request.</p>
 */
public final class CodeDeliveryRejectedException extends CodeSenderException {

    /** 创建不暴露供应商诊断信息的拒绝异常。 / Creates a rejection without provider diagnostics. */
    public CodeDeliveryRejectedException() {
        super("Verification code delivery was rejected");
    }
}
