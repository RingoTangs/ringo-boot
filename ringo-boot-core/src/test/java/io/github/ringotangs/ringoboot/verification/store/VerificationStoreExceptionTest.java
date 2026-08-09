package io.github.ringotangs.ringoboot.verification.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationException;
import org.junit.jupiter.api.Test;

class VerificationStoreExceptionTest {

    @Test
    void createsExceptionWithMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("provider unavailable");

        VerificationStoreException withMessage = new VerificationStoreException("storage unavailable");
        VerificationStoreException withCause = new VerificationStoreException("storage unavailable", cause);

        assertEquals("storage unavailable", withMessage.getMessage());
        assertEquals("storage unavailable", withCause.getMessage());
        assertInstanceOf(VerificationException.class, withMessage);
        assertInstanceOf(VerificationException.class, withCause);
        assertSame(cause, withCause.getCause());
    }

    @Test
    void rejectsNullMessageAndCause() {
        assertThrows(NullPointerException.class, () -> new VerificationStoreException(null));
        assertThrows(NullPointerException.class, () -> new VerificationStoreException(null, new Exception()));
        assertThrows(NullPointerException.class, () -> new VerificationStoreException("storage unavailable", null));
    }
}
