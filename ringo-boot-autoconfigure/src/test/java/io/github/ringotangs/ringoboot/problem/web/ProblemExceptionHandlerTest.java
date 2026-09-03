package io.github.ringotangs.ringoboot.problem.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.problem.ProblemType;
import io.github.ringotangs.ringoboot.problem.ProblemTypeDefinition;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class ProblemExceptionHandlerTest {

    private static final ProblemType PROBLEM_TYPE = () -> ProblemTypeDefinition.of(
            "urn:problem:test:not-found", "problem.test.not-found", "User not found", "User {0} does not exist", 404);

    private final ProblemExceptionHandler handler = new ProblemExceptionHandler();

    @Test
    void createsProblemDetailWithFormattedDefaultDetail() {
        ProblemDetail problem = handler.handleProblemException(ProblemException.withArguments(PROBLEM_TYPE, 42));

        assertEquals(URI.create("urn:problem:test:not-found"), problem.getType());
        assertEquals("User not found", problem.getTitle());
        assertEquals("User 42 does not exist", problem.getDetail());
        assertEquals(404, problem.getStatus());
    }
}
