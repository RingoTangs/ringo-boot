package io.github.ringotangs.springcommons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

class ProblemTypeUriTest {

    @Test
    void createsTwoSegmentUri() {
        assertEquals(URI.create("urn:problem:mvc:invalid-parameter"), ProblemTypeUri.of("mvc", "invalid-parameter"));
    }

    @Test
    void createsMultiSegmentUriWithDigitsAndKebabCase() {
        assertEquals(
                URI.create("urn:problem:business2:user-profile:not-found-404"),
                ProblemTypeUri.of("business2", "user-profile", "not-found-404"));
    }

    @Test
    void rejectsMissingProblemSegment() {
        assertThrows(IllegalArgumentException.class, () -> ProblemTypeUri.of("mvc"));
    }

    @Test
    void rejectsNullValues() {
        assertThrows(NullPointerException.class, () -> ProblemTypeUri.of(null, "not-found"));
        assertThrows(NullPointerException.class, () -> ProblemTypeUri.of("mvc", (String[]) null));
        assertThrows(NullPointerException.class, () -> ProblemTypeUri.of("mvc", "valid", null));
    }

    @Test
    void rejectsInvalidDomainAndSegments() {
        assertInvalid(" ");
        assertInvalid("");
        assertInvalid("not:found");
        assertInvalid("Not-found");
        assertInvalid("not_found");
        assertInvalid("not found");
        assertInvalid("-not-found");
        assertInvalid("not-found-");
        assertInvalid("not--found");
        assertThrows(IllegalArgumentException.class, () -> ProblemTypeUri.of("Invalid", "not-found"));
    }

    private static void assertInvalid(String segment) {
        assertThrows(IllegalArgumentException.class, () -> ProblemTypeUri.of("test", segment));
    }
}
