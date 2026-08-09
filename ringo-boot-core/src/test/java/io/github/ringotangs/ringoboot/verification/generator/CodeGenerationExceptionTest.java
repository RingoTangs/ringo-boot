package io.github.ringotangs.ringoboot.verification.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationException;
import org.junit.jupiter.api.Test;

class CodeGenerationExceptionTest {

    @Test
    void createsExceptionWithMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("random source unavailable");

        CodeGenerationException withMessage = new CodeGenerationException("generation unavailable");
        CodeGenerationException withCause = new CodeGenerationException("generation unavailable", cause);

        assertEquals("generation unavailable", withMessage.getMessage());
        assertEquals("generation unavailable", withCause.getMessage());
        assertInstanceOf(VerificationException.class, withMessage);
        assertInstanceOf(VerificationException.class, withCause);
        assertSame(cause, withCause.getCause());
    }

    @Test
    void rejectsNullMessageAndCause() {
        assertThrows(NullPointerException.class, () -> new CodeGenerationException(null));
        assertThrows(NullPointerException.class, () -> new CodeGenerationException(null, new Exception()));
        assertThrows(NullPointerException.class, () -> new CodeGenerationException("generation unavailable", null));
    }
}
