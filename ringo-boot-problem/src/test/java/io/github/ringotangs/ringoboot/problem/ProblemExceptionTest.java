package io.github.ringotangs.ringoboot.problem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProblemExceptionTest {

    private static final ProblemDescriptor DESCRIPTOR =
            ProblemDescriptor.of("urn:problem:test", "problem.test", "Test problem", "Default detail", 400);

    private static final ProblemDescriptor PARAMETERIZED_DESCRIPTOR = ProblemDescriptor.of(
            "urn:problem:test:parameterized",
            "problem.test.parameterized",
            "Parameterized problem",
            "User {0} does not exist",
            404);

    @Test
    void usesDefaultDetail() {
        ProblemException exception = new ProblemException(DESCRIPTOR);

        assertEquals("Default detail", exception.getMessage());
        assertSame(DESCRIPTOR, exception.getDescriptor());
        assertNull(exception.getCause());
    }

    @Test
    void preservesCause() {
        RuntimeException cause = new RuntimeException("cause");
        ProblemException exception = ProblemException.withCause(DESCRIPTOR, cause);

        assertEquals("Default detail", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void rejectsNullCauseInFactory() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> ProblemException.withCause(DESCRIPTOR, null));

        assertEquals("cause must not be null", exception.getMessage());
    }

    @Test
    void rejectsNullDescriptor() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> new ProblemException(null));

        assertEquals("descriptor must not be null", exception.getMessage());
    }

    @Test
    void formatsAndPreservesDetailArguments() {
        Object[] arguments = {42};
        ProblemException exception = ProblemException.withDetailArguments(PARAMETERIZED_DESCRIPTOR, arguments);
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
        ProblemException exception = ProblemException.withDetailArguments(PARAMETERIZED_DESCRIPTOR, cause, 42);

        assertSame(cause, exception.getCause());
        assertEquals("User 42 does not exist", exception.getMessage());
    }

    @Test
    void rejectsNullDetailArguments() {
        assertThrows(
                NullPointerException.class, () -> ProblemException.withDetailArguments(DESCRIPTOR, (Object[]) null));
        assertThrows(NullPointerException.class, () -> ProblemException.withDetailArguments(DESCRIPTOR, (Object) null));
        assertTrue(ProblemException.withDetailArguments(DESCRIPTOR)
                .getDetailArguments()
                .isEmpty());
    }
}
