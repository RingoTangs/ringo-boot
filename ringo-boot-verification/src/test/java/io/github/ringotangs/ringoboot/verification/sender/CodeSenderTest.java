package io.github.ringotangs.ringoboot.verification.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.email.EmailCodeDelivery;
import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeDelivery;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeSender;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CodeSenderTest {

    @Test
    void channelSendersAreUsableThroughCommonContract() {
        AtomicReference<EmailCodeDelivery> emailDelivery = new AtomicReference<>();
        AtomicReference<SmsCodeDelivery> smsDelivery = new AtomicReference<>();
        EmailCodeSender emailSender = delivery -> {
            emailDelivery.set(delivery);
            return CodeSendResult.ACCEPTED;
        };
        SmsCodeSender smsSender = delivery -> {
            smsDelivery.set(delivery);
            return CodeSendResult.UNKNOWN;
        };
        EmailCodeDelivery email = new EmailCodeDelivery(
                "account", "login", "user@example.com", "123456", Instant.parse("2026-01-01T00:05:00Z"));
        SmsCodeDelivery sms = new SmsCodeDelivery(
                "account", "login", "+8613800000000", "123456", Instant.parse("2026-01-01T00:05:00Z"));

        CodeSender<EmailCodeDelivery> commonEmailSender = emailSender;
        CodeSender<SmsCodeDelivery> commonSmsSender = smsSender;
        assertEquals(CodeSendResult.ACCEPTED, commonEmailSender.send(email));
        assertEquals(CodeSendResult.UNKNOWN, commonSmsSender.send(sms));

        assertSame(email, emailDelivery.get());
        assertSame(sms, smsDelivery.get());
    }

    @Test
    void propagatesSenderFailureThroughCommonContract() {
        CodeSenderException failure = new CodeSenderException("delivery unavailable");
        CodeSender<EmailCodeDelivery> sender = delivery -> {
            throw failure;
        };
        EmailCodeDelivery delivery = new EmailCodeDelivery(
                "account", "login", "user@example.com", "123456", Instant.parse("2026-01-01T00:05:00Z"));

        CodeSenderException thrown = assertThrows(CodeSenderException.class, () -> sender.send(delivery));

        assertSame(failure, thrown);
    }
}
