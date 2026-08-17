package io.github.ringotangs.ringoboot.verification.sender;

/**
 * 供应商明确拒绝验证码发送请求时抛出的异常。
 *
 */
public final class CodeDeliveryRejectedException extends CodeSenderException {

    /** 创建不暴露供应商诊断信息的拒绝异常。 */
    public CodeDeliveryRejectedException() {
        super("Verification code delivery was rejected");
    }
}
