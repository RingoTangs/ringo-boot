package io.github.ringotangs.ringoboot.verification.channel;

/**
 * 供应商明确拒绝验证码发送请求时抛出的异常。
 */
public final class CodeSendRejectedException extends CodeSenderException {

    /**
     * 创建带渠道且不暴露供应商诊断信息的拒绝异常。
     *
     * @param channel 拒绝验证码请求的渠道
     */
    public CodeSendRejectedException(VerificationChannel channel) {
        super(channel, "Verification code send was rejected");
    }
}
