package io.github.ringotangs.springcommons.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class SpringMvcExceptionHandlerTest {

    private final SpringMvcExceptionHandler handler = new SpringMvcExceptionHandler(
            new StaticMessageSource(),
            new ExceptionHandlerProperties()
    );

    @Test
    void returnsStableProblemAndPreservesHeadersForMethodNotAllowed(
            CapturedOutput output
    ) throws Exception {
        ResponseEntity<Object> response = handler.handleException(
                new HttpRequestMethodNotSupportedException("POST", List.of("GET")),
                webRequest()
        );

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getAllow()).containsExactly(HttpMethod.GET);
        assertThat(response.getBody()).isInstanceOfSatisfying(
                ProblemDetail.class,
                problem -> {
                    assertThat(problem.getType()).isEqualTo(URI.create(
                            "urn:problem:mvc:method-not-allowed"
                    ));
                    assertThat(problem.getTitle()).isEqualTo("Method Not Allowed");
                    assertThat(problem.getDetail()).isEqualTo(
                            "Method 'POST' is not supported."
                    );
                }
        );
        assertThat(output).doesNotContain("Unhandled Spring MVC exception");
    }

    @Test
    void logsAndHidesInternalSpringMvcException(CapturedOutput output) throws Exception {
        ConversionNotSupportedException exception = new ConversionNotSupportedException(
                "secret-value",
                Integer.class,
                new IllegalStateException("internal-secret")
        );

        ResponseEntity<Object> response = handler.handleException(exception, webRequest());

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOfSatisfying(
                ProblemDetail.class,
                problem -> {
                    assertThat(problem.getType()).isEqualTo(URI.create(
                            "urn:problem:mvc:internal-server-error"
                    ));
                    assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
                    assertThat(problem.getDetail()).doesNotContain("secret");
                }
        );
        assertThat(output)
                .contains("ERROR")
                .contains("Unhandled Spring MVC exception")
                .contains(ConversionNotSupportedException.class.getName());
    }

    @Test
    void preservesUnmappedErrorResponseProblemType(CapturedOutput output) throws Exception {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Custom conflict"
        );
        body.setType(URI.create("urn:problem:application:custom-conflict"));
        ErrorResponseException exception = new ErrorResponseException(
                HttpStatus.CONFLICT,
                body,
                null
        );

        ResponseEntity<Object> response = handler.handleException(exception, webRequest());

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isSameAs(body);
        assertThat(output).doesNotContain("Unhandled Spring MVC exception");
    }

    @Test
    void usesSpringNativeMessageCodesWhenLibraryI18nIsDisabled() throws Exception {
        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException("POST", List.of("GET"));
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage(
                exception.getTitleMessageCode(),
                Locale.CHINA,
                "Spring 本地化标题"
        );
        messageSource.addMessage(
                exception.getDetailMessageCode(),
                Locale.CHINA,
                "Spring 本地化详情"
        );
        ExceptionHandlerProperties properties = new ExceptionHandlerProperties();
        properties.setI18nEnabled(false);
        SpringMvcExceptionHandler localizedHandler = new SpringMvcExceptionHandler(
                messageSource,
                properties
        );

        org.springframework.context.i18n.LocaleContextHolder.setLocale(Locale.CHINA);
        try {
            ResponseEntity<Object> response = localizedHandler.handleException(
                    exception,
                    webRequest()
            );

            assertThat(response).isNotNull();
            assertThat(response.getBody()).isInstanceOfSatisfying(
                    ProblemDetail.class,
                    problem -> {
                        assertThat(problem.getType()).isEqualTo(URI.create(
                                "urn:problem:mvc:method-not-allowed"
                        ));
                        assertThat(problem.getTitle()).isEqualTo("Spring 本地化标题");
                        assertThat(problem.getDetail()).isEqualTo("Spring 本地化详情");
                    }
            );
        }
        finally {
            org.springframework.context.i18n.LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void preservesExpectedAsyncTimeoutAndDoesNotLogIt(CapturedOutput output)
            throws Exception {
        ResponseEntity<Object> response = handler.handleException(
                new AsyncRequestTimeoutException(),
                webRequest()
        );

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isInstanceOfSatisfying(
                ProblemDetail.class,
                problem -> assertThat(problem.getType()).isEqualTo(URI.create(
                        "urn:problem:mvc:request-timeout"
                ))
        );
        assertThat(output).doesNotContain("Unhandled Spring MVC exception");
    }

    private ServletWebRequest webRequest() {
        return new ServletWebRequest(
                new MockHttpServletRequest(),
                new MockHttpServletResponse()
        );
    }
}
