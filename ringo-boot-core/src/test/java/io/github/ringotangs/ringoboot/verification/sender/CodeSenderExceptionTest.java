package io.github.ringotangs.ringoboot.verification.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationException;
import org.junit.jupiter.api.Test;

class CodeSenderExceptionTest {

    @Test
    void createsExceptionWithMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("provider unavailable");

        CodeSenderException withMessage = new CodeSenderException("delivery unavailable");
        CodeSenderException withCause = new CodeSenderException("delivery unavailable", cause);

        assertEquals("delivery unavailable", withMessage.getMessage());
        assertEquals("delivery unavailable", withCause.getMessage());
        assertInstanceOf(VerificationException.class, withMessage);
        assertInstanceOf(VerificationException.class, withCause);
        assertSame(cause, withCause.getCause());
    }

    @Test
    void rejectsNullMessageAndCause() {
        assertThrows(NullPointerException.class, () -> new CodeSenderException(null));
        assertThrows(NullPointerException.class, () -> new CodeSenderException(null, new Exception()));
        assertThrows(NullPointerException.class, () -> new CodeSenderException("delivery unavailable", null));
    }

    @Test
    void representsExplicitProviderRejectionAsSenderFailure() {
        CodeDeliveryRejectedException exception = new CodeDeliveryRejectedException();

        assertInstanceOf(CodeSenderException.class, exception);
        assertEquals("Verification code delivery was rejected", exception.getMessage());
    }
}
