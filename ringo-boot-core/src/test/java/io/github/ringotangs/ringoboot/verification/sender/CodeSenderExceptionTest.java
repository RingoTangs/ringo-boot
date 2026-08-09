package io.github.ringotangs.ringoboot.verification.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CodeSenderExceptionTest {

    @Test
    void createsExceptionWithMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("provider unavailable");

        CodeSenderException withMessage = new CodeSenderException("delivery unavailable");
        CodeSenderException withCause = new CodeSenderException("delivery unavailable", cause);

        assertEquals("delivery unavailable", withMessage.getMessage());
        assertEquals("delivery unavailable", withCause.getMessage());
        assertSame(cause, withCause.getCause());
    }

    @Test
    void rejectsNullMessageAndCause() {
        assertThrows(NullPointerException.class, () -> new CodeSenderException(null));
        assertThrows(NullPointerException.class, () -> new CodeSenderException(null, new Exception()));
        assertThrows(NullPointerException.class, () -> new CodeSenderException("delivery unavailable", null));
    }
}
