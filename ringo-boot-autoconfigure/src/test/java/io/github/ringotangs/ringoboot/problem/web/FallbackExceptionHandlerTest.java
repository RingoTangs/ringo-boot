package io.github.ringotangs.ringoboot.problem.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;

@ExtendWith(OutputCaptureExtension.class)
class FallbackExceptionHandlerTest {

    @Test
    void returnsSafeInternalServerErrorForUnexpectedException(CapturedOutput output) {
        FallbackExceptionHandler handler = new FallbackExceptionHandler();

        ResponseEntity<ProblemDetail> response =
                handler.handleException(new IllegalStateException("password=secret; SQL select * from users"));
        ProblemDetail body = response.getBody();
        assertNotNull(body);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(URI.create("urn:problem:fallback:internal-server-error"), body.getType());
        assertEquals("Internal server error", body.getTitle());
        assertEquals("An unexpected error occurred", body.getDetail());
        assertFalse(body.getDetail().contains("secret"));
        assertFalse(body.getDetail().contains("select"));
        assertThat(output)
                .contains("ERROR")
                .contains("Unhandled exception")
                .contains(IllegalStateException.class.getName());
    }

    @Test
    void preservesFrameworkErrorResponseStatusHeadersAndBody(CapturedOutput output) {
        FallbackExceptionHandler handler = new FallbackExceptionHandler();
        FrameworkException exception = new FrameworkException();

        ResponseEntity<ProblemDetail> response = handler.handleException(exception);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("120", response.getHeaders().getFirst("Retry-After"));
        assertEquals(exception.getBody(), response.getBody());
        assertThat(output).doesNotContain("Unhandled exception");
    }

    private static final class FrameworkException extends RuntimeException implements ErrorResponse {

        private final ProblemDetail body =
                ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");

        private final HttpHeaders headers = new HttpHeaders();

        private FrameworkException() {
            headers.set("Retry-After", "120");
        }

        @Override
        public HttpStatusCode getStatusCode() {
            return HttpStatus.TOO_MANY_REQUESTS;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public ProblemDetail getBody() {
            return body;
        }
    }
}
