package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeSender;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 仅供开发环境使用的控制台短信验证码发送器。 / Development-only console SMS sender. */
final class ConsoleSmsCodeSender implements SmsCodeSender {

    private static final Log LOGGER = LogFactory.getLog(ConsoleSmsCodeSender.class);

    @Override
    public void send(CodeDelivery delivery) {
        LOGGER.warn("DEVELOPMENT ONLY - SMS verification code: purpose="
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
