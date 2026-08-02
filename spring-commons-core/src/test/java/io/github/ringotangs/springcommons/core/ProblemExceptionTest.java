package io.github.ringotangs.springcommons.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProblemExceptionTest {

    private static final ProblemType PROBLEM_TYPE = () -> ProblemDefinition.of(
            "urn:problem:test",
            "Test problem",
            "Default detail",
            400
    );

    @Test
    void usesDefaultDetail() {
        ProblemException exception = new ProblemException(PROBLEM_TYPE);

        assertEquals("Default detail", exception.getMessage());
        assertSame(PROBLEM_TYPE, exception.getProblemType());
        assertNull(exception.getCause());
    }

    @Test
    void preservesCustomDetail() {
        ProblemException exception = new ProblemException(PROBLEM_TYPE, "Custom detail");

        assertEquals("Custom detail", exception.getMessage());
    }

    @Test
    void fallsBackToDefaultDetailForBlankDetails() {
        assertEquals("Default detail", new ProblemException(PROBLEM_TYPE, null).getMessage());
        assertEquals("Default detail", new ProblemException(PROBLEM_TYPE, "").getMessage());
        assertEquals("Default detail", new ProblemException(PROBLEM_TYPE, "   ").getMessage());
    }

    @Test
    void preservesCause() {
        RuntimeException cause = new RuntimeException("cause");
        ProblemException exception = ProblemException.withCause(PROBLEM_TYPE, cause);

        assertEquals("Default detail", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void rejectsNullCauseInFactory() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ProblemException.withCause(PROBLEM_TYPE, null)
        );

        assertEquals("cause must not be null", exception.getMessage());
    }

    @Test
    void rejectsNullProblemType() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ProblemException(null)
        );

        assertEquals("problemType must not be null", exception.getMessage());
    }
}
