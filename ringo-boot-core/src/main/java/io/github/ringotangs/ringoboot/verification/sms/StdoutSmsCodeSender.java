package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.sender.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;

/**
 * 将短信验证码输出到标准输出，适用于开发和演示环境。
 *
 * <p>Writes SMS verification codes to standard output for development and
 * demonstration environments.</p>
 *
 * <p><strong>API 注意事项 / API note:</strong> 此实现会输出明文验证码，不应在生产环境使用。 / This implementation writes
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
     * @return 始终返回供应商已接受 / always returns provider accepted
     */
    @Override
    public CodeSendResult send(CodeDelivery delivery) {
        System.out.println("DEVELOPMENT ONLY - SMS verification code: namespace="
                + delivery.key().namespace()
                + ", purpose="
                + delivery.key().purpose()
                + ", subject="
                + mask(delivery.key().subject())
                + ", code="
                + delivery.code()
                + ", expiresAt="
                + delivery.expiresAt());
        return CodeSendResult.ACCEPTED;
    }

    private String mask(String subject) {
        int visibleLength = Math.min(4, subject.length());
        return "***" + subject.substring(subject.length() - visibleLength);
    }
}
