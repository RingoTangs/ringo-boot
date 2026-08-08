package io.github.ringotangs.springcommons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProblemExceptionTest {

    private static final ProblemType PROBLEM_TYPE =
            () -> ProblemDefinition.of("urn:problem:test", "problem.test", "Test problem", "Default detail", 400);

    private static final ProblemType PARAMETERIZED_PROBLEM_TYPE = () -> ProblemDefinition.of(
            "urn:problem:test:parameterized",
            "problem.test.parameterized",
            "Parameterized problem",
            "User {0} does not exist",
            404);

    @Test
    void usesDefaultDetail() {
        ProblemException exception = new ProblemException(PROBLEM_TYPE);

        assertEquals("Default detail", exception.getMessage());
        assertSame(PROBLEM_TYPE, exception.getProblemType());
        assertNull(exception.getCause());
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
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> ProblemException.withCause(PROBLEM_TYPE, null));

        assertEquals("cause must not be null", exception.getMessage());
    }

    @Test
    void rejectsNullProblemType() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> new ProblemException(null));

        assertEquals("problemType must not be null", exception.getMessage());
    }

    @Test
    void formatsAndPreservesDetailArguments() {
        Object[] arguments = {42};
        ProblemException exception = ProblemException.withArguments(PARAMETERIZED_PROBLEM_TYPE, arguments);
        arguments[0] = 99;

        assertEquals("User 42 does not exist", exception.getMessage());
        assertEquals(42, exception.getDetailArguments().getFirst());
        assertThrows(
                UnsupportedOperationException.class,
                () -> exception.getDetailArguments().add(99));
    }

    @Test
    void preservesCauseWithDetailArguments() {
        RuntimeException cause = new RuntimeException("cause");
        ProblemException exception = ProblemException.withArgumentsAndCause(PARAMETERIZED_PROBLEM_TYPE, cause, 42);

        assertSame(cause, exception.getCause());
        assertEquals("User 42 does not exist", exception.getMessage());
    }

    @Test
    void rejectsNullDetailArguments() {
        assertThrows(NullPointerException.class, () -> ProblemException.withArguments(PROBLEM_TYPE, (Object[]) null));
        assertThrows(NullPointerException.class, () -> ProblemException.withArguments(PROBLEM_TYPE, (Object) null));
        assertTrue(ProblemException.withArguments(PROBLEM_TYPE)
                .getDetailArguments()
                .isEmpty());
    }
}
