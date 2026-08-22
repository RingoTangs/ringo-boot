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
     * @param delivery 验证码交付内容
     * @return 始终返回供应商已接受
     */
    @Override
    public CodeSendResult send(EmailCodeDelivery delivery) {
        System.out.println("DEVELOPMENT ONLY - Email verification code: namespace="
                + delivery.getNamespace()
                + ", purpose="
                + delivery.getPurpose()
                + ", email="
                + mask(delivery.getEmail())
                + ", code="
                + delivery.getCode()
                + ", expiresAt="
                + delivery.getExpiresAt());
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
