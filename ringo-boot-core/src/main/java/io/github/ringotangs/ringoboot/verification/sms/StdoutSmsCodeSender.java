package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.sender.CodeDelivery;

/**
 * 将短信验证码输出到标准输出，适用于开发和演示环境。
 *
 * <p>Writes SMS verification codes to standard output for development and
 * demonstration environments.</p>
 *
 * @apiNote 此实现会输出明文验证码，不应在生产环境使用。 / This implementation writes
 *     plaintext codes and must not be used in production.
 */
public final class StdoutSmsCodeSender implements SmsCodeSender {

    /** 创建标准输出短信验证码发送器。 / Creates a standard-output SMS code sender. */
    public StdoutSmsCodeSender() {}

    /**
     * 将验证码及脱敏后的手机号输出到标准输出。
     *
     * <p>Writes the code and masked phone number to standard output.</p>
     *
     * @param delivery 验证码交付内容 / the verification code delivery
     */
    @Override
    public void send(CodeDelivery delivery) {
        System.out.println("DEVELOPMENT ONLY - SMS verification code: purpose="
                + delivery.key().purpose()
                + ", subject="
                + mask(delivery.key().subject())
                + ", code="
                + delivery.code()
                + ", expiresAt="
                + delivery.expiresAt());
    }

    private String mask(String subject) {
        int visibleLength = Math.min(4, subject.length());
        return "***" + subject.substring(subject.length() - visibleLength);
    }
}
