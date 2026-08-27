package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;

/**
 * 将短信验证码输出到标准输出，适用于开发和演示环境。
 *
 *
 * <p><strong>API 注意事项：</strong> 此实现会输出明文验证码，不应在生产环境使用。
 */
public final class StdoutSmsCodeSender implements SmsCodeSender {

    /** 创建标准输出短信验证码发送器。 */
    public StdoutSmsCodeSender() {}

    /**
     * 将验证码及脱敏后的手机号输出到标准输出。
     *
     *
     * @param message 验证码消息
     * @return 始终返回供应商已接受
     */
    @Override
    public CodeSendResult send(SmsCodeMessage message) {
        System.out.println("DEVELOPMENT ONLY - SMS verification code: namespace="
                + message.namespace()
                + ", purpose="
                + message.purpose()
                + ", phoneNumber="
                + mask(message.phoneNumber())
                + ", code="
                + message.code()
                + ", expiresAt="
                + message.expiresAt());
        return CodeSendResult.ACCEPTED;
    }

    private String mask(String subject) {
        int visibleLength = Math.min(4, subject.length());
        return "***" + subject.substring(subject.length() - visibleLength);
    }
}
