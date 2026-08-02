package io.github.ringotangs.springcommons.autoconfigure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;

import java.net.URI;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FallbackExceptionHandlerTest {

    private final StaticMessageSource messageSource = new StaticMessageSource();
    private final ProblemExceptionHandlerProperties properties =
            new ProblemExceptionHandlerProperties();

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void returnsSafeInternalServerErrorForUnexpectedException() {
        FallbackExceptionHandler handler = createHandler(new DefaultProblemMessageResolver());

        ResponseEntity<ProblemDetail> response = handler.handleException(
                new IllegalStateException("password=secret; SQL select * from users")
        );
        ProblemDetail body = response.getBody();
        assertNotNull(body);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(
                URI.create("urn:problem:spring-commons:internal-server-error"),
                body.getType()
        );
        assertEquals("Internal server error", body.getTitle());
        assertEquals("An unexpected error occurred", body.getDetail());
        assertFalse(body.getDetail().contains("secret"));
        assertFalse(body.getDetail().contains("select"));
    }

    @Test
    void preservesFrameworkErrorResponseStatusHeadersAndBody() {
        FallbackExceptionHandler handler = createHandler(new DefaultProblemMessageResolver());
        FrameworkException exception = new FrameworkException();

        ResponseEntity<ProblemDetail> response = handler.handleException(exception);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("120", response.getHeaders().getFirst("Retry-After"));
        assertEquals(exception.getBody(), response.getBody());
    }

    @Test
    void localizesUnexpectedExceptionWhenInternationalizationIsEnabled() {
        messageSource.addMessage(
                "problem.internal-server-error.title",
                Locale.SIMPLIFIED_CHINESE,
                "服务器内部错误"
        );
        messageSource.addMessage(
                "problem.internal-server-error.detail",
                Locale.SIMPLIFIED_CHINESE,
                "发生了意外错误"
        );
        properties.setI18nEnabled(true);
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        FallbackExceptionHandler handler = createHandler(
                new MessageSourceProblemMessageResolver(messageSource)
        );

        ProblemDetail body = handler.handleException(
                new IllegalStateException("internal")
        ).getBody();
        assertNotNull(body);

        assertEquals("服务器内部错误", body.getTitle());
        assertEquals("发生了意外错误", body.getDetail());
    }

    private FallbackExceptionHandler createHandler(ProblemMessageResolver resolver) {
        return new FallbackExceptionHandler(
                new ProblemExceptionHandler(resolver),
                messageSource,
                properties
        );
    }

    private static final class FrameworkException extends RuntimeException
            implements ErrorResponse {

        private final ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests"
        );

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
