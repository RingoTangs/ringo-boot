package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VerificationExceptionTest {

    @Test
    void createsExceptionWithMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("provider unavailable");

        TestVerificationException withMessage = new TestVerificationException("verification unavailable");
        TestVerificationException withCause = new TestVerificationException("verification unavailable", cause);

        assertEquals("verification unavailable", withMessage.getMessage());
        assertEquals("verification unavailable", withCause.getMessage());
        assertSame(cause, withCause.getCause());
    }

    @Test
    void rejectsNullMessageAndCause() {
        assertThrows(NullPointerException.class, () -> new TestVerificationException(null));
        assertThrows(NullPointerException.class, () -> new TestVerificationException(null, new Exception()));
        assertThrows(NullPointerException.class, () -> new TestVerificationException("verification unavailable", null));
    }

    private static final class TestVerificationException extends VerificationException {

        private TestVerificationException(String message) {
            super(message);
        }

        private TestVerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
