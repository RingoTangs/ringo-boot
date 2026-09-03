package io.github.ringotangs.ringoboot.problem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

class ProblemDescriptorTest {

    @Test
    void createsDescriptorFromUriString() {
        ProblemDescriptor descriptor =
                ProblemDescriptor.of("urn:problem:test", "problem.test", "Test problem", "Default detail", 400);

        assertEquals(URI.create("urn:problem:test"), descriptor.type());
        assertEquals("problem.test", descriptor.messageCode());
        assertEquals("Test problem", descriptor.title());
        assertEquals("Default detail", descriptor.defaultDetail());
        assertEquals(400, descriptor.status());
    }

    @Test
    void uriAndStringFactoriesCreateEqualDescriptors() {
        ProblemDescriptor fromString =
                ProblemDescriptor.of("urn:problem:test", "problem.test", "Test problem", "Default detail", 400);
        ProblemDescriptor fromUri = ProblemDescriptor.of(
                URI.create("urn:problem:test"), "problem.test", "Test problem", "Default detail", 400);

        assertEquals(fromString, fromUri);
    }

    @Test
    void acceptsClientAndServerErrorStatuses() {
        ProblemDescriptor.of("urn:problem:test:400", "problem.test", "Test", "Detail", 400);
        ProblemDescriptor.of("urn:problem:test:599", "problem.test", "Test", "Detail", 599);
    }

    @Test
    void rejectsNonErrorStatuses() {
        IllegalArgumentException belowRange = assertThrows(
                IllegalArgumentException.class,
                () -> ProblemDescriptor.of("urn:problem:test:399", "problem.test", "Test", "Detail", 399));
        IllegalArgumentException aboveRange = assertThrows(
                IllegalArgumentException.class,
                () -> ProblemDescriptor.of("urn:problem:test:600", "problem.test", "Test", "Detail", 600));

        assertEquals("status must be between 400 and 599: 399", belowRange.getMessage());
        assertEquals("status must be between 400 and 599: 600", aboveRange.getMessage());
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThrows(
                NullPointerException.class, () -> new ProblemDescriptor(null, "problem.test", "Test", "Detail", 400));
        assertThrows(
                NullPointerException.class,
                () -> ProblemDescriptor.of("urn:problem:test", "problem.test", null, "Detail", 400));
        assertThrows(
                NullPointerException.class,
                () -> ProblemDescriptor.of("urn:problem:test", "problem.test", "Test", null, 400));
    }

    @Test
    void rejectsMissingMessageCode() {
        assertThrows(
                NullPointerException.class,
                () -> ProblemDescriptor.of("urn:problem:test", null, "Test", "Detail", 400));
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ProblemDescriptor.of("urn:problem:test", "  ", "Test", "Detail", 400));

        assertEquals("messageCode must not be blank", exception.getMessage());
    }
}
