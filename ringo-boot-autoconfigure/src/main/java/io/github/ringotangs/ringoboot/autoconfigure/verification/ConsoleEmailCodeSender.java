package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 仅供开发环境使用的控制台邮件验证码发送器。 / Development-only console email sender. */
final class ConsoleEmailCodeSender implements EmailCodeSender {

    private static final Log LOGGER = LogFactory.getLog(ConsoleEmailCodeSender.class);

    @Override
    public void send(CodeDelivery delivery) {
        LOGGER.warn("DEVELOPMENT ONLY - Email verification code: purpose="
                + delivery.key().purpose()
                + ", subject="
                + mask(delivery.key().subject())
                + ", code="
                + delivery.code()
                + ", expiresAt="
                + delivery.expiresAt());
    }

    private String mask(String subject) {
        int separator = subject.indexOf('@');
        if (separator <= 0) {
            return "***";
        }
        return subject.charAt(0) + "***" + subject.substring(separator);
    }
}
