package io.github.ringotangs.ringoboot.verification.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeSender;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CodeSenderTest {

    @Test
    void channelSendersAreUsableThroughCommonContract() {
        AtomicReference<CodeDelivery> emailDelivery = new AtomicReference<>();
        AtomicReference<CodeDelivery> smsDelivery = new AtomicReference<>();
        EmailCodeSender emailSender = delivery -> {
            emailDelivery.set(delivery);
            return CodeSendResult.ACCEPTED;
        };
        SmsCodeSender smsSender = delivery -> {
            smsDelivery.set(delivery);
            return CodeSendResult.UNKNOWN;
        };
        CodeDelivery delivery = new CodeDelivery(
                new VerificationKey("account", "login", "user@example.com"),
                "123456",
                Instant.parse("2026-01-01T00:05:00Z"));

        CodeSender commonEmailSender = emailSender;
        CodeSender commonSmsSender = smsSender;
        assertEquals(CodeSendResult.ACCEPTED, commonEmailSender.send(delivery));
        assertEquals(CodeSendResult.UNKNOWN, commonSmsSender.send(delivery));

        assertSame(delivery, emailDelivery.get());
        assertSame(delivery, smsDelivery.get());
    }

    @Test
    void propagatesSenderFailureThroughCommonContract() {
        CodeSenderException failure = new CodeSenderException("delivery unavailable");
        CodeSender sender = delivery -> {
            throw failure;
        };
        CodeDelivery delivery = new CodeDelivery(
                new VerificationKey("account", "login", "user@example.com"),
                "123456",
                Instant.parse("2026-01-01T00:05:00Z"));

        CodeSenderException thrown = assertThrows(CodeSenderException.class, () -> sender.send(delivery));

        assertSame(failure, thrown);
    }
}
