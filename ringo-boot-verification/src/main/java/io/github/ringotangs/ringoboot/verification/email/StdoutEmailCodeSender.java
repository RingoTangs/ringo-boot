package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;

/**
 * 将邮件验证码输出到标准输出，适用于开发和演示环境。
 *
 *
 * <p><strong>API 注意事项：</strong> 此实现会输出明文验证码，不应在生产环境使用。
 */
public final class StdoutEmailCodeSender implements EmailCodeSender {

    /** 创建标准输出邮件验证码发送器。 */
    public StdoutEmailCodeSender() {}

    /**
     * 将验证码及脱敏后的邮箱地址输出到标准输出。
     *
     *
     * @param message 验证码消息
     * @return 始终返回供应商已接受
     */
    @Override
    public CodeSendResult send(EmailCodeMessage message) {
        System.out.println("DEVELOPMENT ONLY - Email verification code: namespace="
                + message.namespace()
                + ", purpose="
                + message.purpose()
                + ", email="
                + mask(message.email())
                + ", code="
                + message.code()
                + ", expiresAt="
                + message.expiresAt());
        return CodeSendResult.ACCEPTED;
    }

    private String mask(String subject) {
        int separator = subject.indexOf('@');
        if (separator <= 0) {
            return "***";
        }
        return subject.charAt(0) + "***" + subject.substring(separator);
    }
}
