package io.github.ringotangs.ringoboot.verification.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationException;
import org.junit.jupiter.api.Test;

class CodeSenderExceptionTest {

    @Test
    void createsExceptionWithChannelMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("provider unavailable");

        CodeSenderException withMessage = new CodeSenderException(VerificationChannel.EMAIL, "delivery unavailable");
        CodeSenderException withCause = new CodeSenderException(VerificationChannel.SMS, "delivery unavailable", cause);

        assertEquals(VerificationChannel.EMAIL, withMessage.channel());
        assertEquals(VerificationChannel.SMS, withCause.channel());
        assertEquals("delivery unavailable", withMessage.getMessage());
        assertEquals("delivery unavailable", withCause.getMessage());
        assertInstanceOf(VerificationException.class, withMessage);
        assertInstanceOf(VerificationException.class, withCause);
        assertSame(cause, withCause.getCause());
    }

    @Test
    void rejectsNullChannelMessageAndCause() {
        assertThrows(NullPointerException.class, () -> new CodeSenderException(null, "delivery unavailable"));
        assertThrows(NullPointerException.class, () -> new CodeSenderException(VerificationChannel.EMAIL, null));
        assertThrows(
                NullPointerException.class,
                () -> new CodeSenderException(null, "delivery unavailable", new Exception()));
        assertThrows(
                NullPointerException.class,
                () -> new CodeSenderException(VerificationChannel.EMAIL, null, new Exception()));
        assertThrows(
                NullPointerException.class,
                () -> new CodeSenderException(VerificationChannel.EMAIL, "delivery unavailable", null));
    }

    @Test
    void representsExplicitProviderRejectionAsSenderFailure() {
        VerificationChannel channel = VerificationChannel.of("voice");

        CodeDeliveryRejectedException exception = new CodeDeliveryRejectedException(channel);

        assertInstanceOf(CodeSenderException.class, exception);
        assertEquals(channel, exception.channel());
        assertEquals("Verification code delivery was rejected", exception.getMessage());
    }
}
