package io.github.ringotangs.springcommons.core;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProblemDefinitionTest {

    @Test
    void createsDefinitionFromUriString() {
        ProblemDefinition definition = ProblemDefinition.of(
                "urn:problem:test",
                "Test problem",
                "Default detail",
                400
        );

        assertEquals(URI.create("urn:problem:test"), definition.type());
        assertEquals("Test problem", definition.title());
        assertEquals("Default detail", definition.defaultDetail());
        assertEquals(400, definition.httpStatus());
    }

    @Test
    void acceptsClientAndServerErrorStatuses() {
        ProblemDefinition.of("urn:problem:test:400", "Test", "Detail", 400);
        ProblemDefinition.of("urn:problem:test:599", "Test", "Detail", 599);
    }

    @Test
    void rejectsNonErrorStatuses() {
        IllegalArgumentException belowRange = assertThrows(
                IllegalArgumentException.class,
                () -> ProblemDefinition.of("urn:problem:test:399", "Test", "Detail", 399)
        );
        IllegalArgumentException aboveRange = assertThrows(
                IllegalArgumentException.class,
                () -> ProblemDefinition.of("urn:problem:test:600", "Test", "Detail", 600)
        );

        assertEquals("httpStatus must be between 400 and 599: 399", belowRange.getMessage());
        assertEquals("httpStatus must be between 400 and 599: 600", aboveRange.getMessage());
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThrows(
                NullPointerException.class,
                () -> new ProblemDefinition(null, "Test", "Detail", 400)
        );
        assertThrows(
                NullPointerException.class,
                () -> ProblemDefinition.of("urn:problem:test", null, "Detail", 400)
        );
        assertThrows(
                NullPointerException.class,
                () -> ProblemDefinition.of("urn:problem:test", "Test", null, 400)
        );
    }
}
