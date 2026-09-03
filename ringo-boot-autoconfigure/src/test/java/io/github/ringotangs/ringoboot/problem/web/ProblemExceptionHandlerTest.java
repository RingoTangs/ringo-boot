package io.github.ringotangs.ringoboot.problem.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.ringotangs.ringoboot.problem.ProblemDescriptor;
import io.github.ringotangs.ringoboot.problem.ProblemException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class ProblemExceptionHandlerTest {

    private static final ProblemDescriptor DESCRIPTOR = ProblemDescriptor.of(
            "urn:problem:test:not-found", "problem.test.not-found", "User not found", "User {0} does not exist", 404);

    private final ProblemExceptionHandler handler = new ProblemExceptionHandler();

    @Test
    void createsProblemDetailWithFormattedDefaultDetail() {
        ProblemDetail problem = handler.handleProblemException(ProblemException.withDetailArguments(DESCRIPTOR, 42));

        assertEquals(URI.create("urn:problem:test:not-found"), problem.getType());
        assertEquals("User not found", problem.getTitle());
        assertEquals("User 42 does not exist", problem.getDetail());
        assertEquals(404, problem.getStatus());
    }
}
