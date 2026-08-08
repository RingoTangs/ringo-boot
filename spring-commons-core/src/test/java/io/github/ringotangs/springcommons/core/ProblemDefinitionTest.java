package io.github.ringotangs.springcommons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

class ProblemDefinitionTest {

    @Test
    void createsDefinitionFromUriString() {
        ProblemDefinition definition =
                ProblemDefinition.of("urn:problem:test", "problem.test", "Test problem", "Default detail", 400);

        assertEquals(URI.create("urn:problem:test"), definition.type());
        assertEquals("problem.test", definition.messageCode());
        assertEquals("Test problem", definition.title());
        assertEquals("Default detail", definition.defaultDetail());
        assertEquals(400, definition.httpStatus());
    }

    @Test
    void uriAndStringFactoriesCreateEqualDefinitions() {
        ProblemDefinition fromString =
                ProblemDefinition.of("urn:problem:test", "problem.test", "Test problem", "Default detail", 400);
        ProblemDefinition fromUri = ProblemDefinition.of(
                URI.create("urn:problem:test"), "problem.test", "Test problem", "Default detail", 400);

        assertEquals(fromString, fromUri);
    }

    @Test
    void acceptsClientAndServerErrorStatuses() {
        ProblemDefinition.of("urn:problem:test:400", "problem.test", "Test", "Detail", 400);
        ProblemDefinition.of("urn:problem:test:599", "problem.test", "Test", "Detail", 599);
    }

    @Test
    void rejectsNonErrorStatuses() {
        IllegalArgumentException belowRange = assertThrows(
                IllegalArgumentException.class,
                () -> ProblemDefinition.of("urn:problem:test:399", "problem.test", "Test", "Detail", 399));
        IllegalArgumentException aboveRange = assertThrows(
                IllegalArgumentException.class,
                () -> ProblemDefinition.of("urn:problem:test:600", "problem.test", "Test", "Detail", 600));

        assertEquals("httpStatus must be between 400 and 599: 399", belowRange.getMessage());
        assertEquals("httpStatus must be between 400 and 599: 600", aboveRange.getMessage());
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThrows(
                NullPointerException.class, () -> new ProblemDefinition(null, "problem.test", "Test", "Detail", 400));
        assertThrows(
                NullPointerException.class,
                () -> ProblemDefinition.of("urn:problem:test", "problem.test", null, "Detail", 400));
        assertThrows(
                NullPointerException.class,
                () -> ProblemDefinition.of("urn:problem:test", "problem.test", "Test", null, 400));
    }

    @Test
    void rejectsMissingMessageCode() {
        assertThrows(
                NullPointerException.class,
                () -> ProblemDefinition.of("urn:problem:test", null, "Test", "Detail", 400));
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ProblemDefinition.of("urn:problem:test", "  ", "Test", "Detail", 400));

        assertEquals("messageCode must not be blank", exception.getMessage());
    }
}
