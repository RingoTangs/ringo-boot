package io.github.ringotangs.springcommons.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringMvcProblemTypeTest {

    @ParameterizedTest
    @MethodSource("exceptionMappings")
    void mapsSpringMvcExceptionsByClientSemantics(
            Class<? extends Exception> exceptionType,
            SpringMvcProblemType expected
    ) {
        assertThat(SpringMvcProblemType.resolve(mock(exceptionType))).isEqualTo(expected);
    }

    @Test
    void mapsRequestAndReturnValueValidationDifferently() {
        HandlerMethodValidationException exception = mock(
                HandlerMethodValidationException.class
        );
        when(exception.isForReturnValue()).thenReturn(false, true);

        assertThat(SpringMvcProblemType.resolve(exception))
                .isEqualTo(SpringMvcProblemType.VALIDATION_FAILED);
        assertThat(SpringMvcProblemType.resolve(exception))
                .isEqualTo(SpringMvcProblemType.INTERNAL_SERVER_ERROR);
    }

    @Test
    void leavesUnknownExceptionsUnmapped() {
        assertThat(SpringMvcProblemType.resolve(new IllegalStateException())).isNull();
    }

    @Test
    void exposesMvcSpecificProblemTypeUris() {
        assertThat(SpringMvcProblemType.INVALID_PARAMETER.getType()).isEqualTo(
                URI.create("urn:problem:mvc:invalid-parameter")
        );
        assertThat(SpringMvcProblemType.INTERNAL_SERVER_ERROR.getType()).isEqualTo(
                URI.create("urn:problem:mvc:internal-server-error")
        );
    }

    private static Stream<Arguments> exceptionMappings() {
        return Stream.of(
                Arguments.of(
                        HttpRequestMethodNotSupportedException.class,
                        SpringMvcProblemType.METHOD_NOT_ALLOWED
                ),
                Arguments.of(
                        HttpMediaTypeNotSupportedException.class,
                        SpringMvcProblemType.UNSUPPORTED_MEDIA_TYPE
                ),
                Arguments.of(
                        HttpMediaTypeNotAcceptableException.class,
                        SpringMvcProblemType.NOT_ACCEPTABLE
                ),
                Arguments.of(
                        MissingServletRequestParameterException.class,
                        SpringMvcProblemType.MISSING_REQUEST_VALUE
                ),
                Arguments.of(
                        ServletRequestBindingException.class,
                        SpringMvcProblemType.MISSING_REQUEST_VALUE
                ),
                Arguments.of(TypeMismatchException.class, SpringMvcProblemType.INVALID_PARAMETER),
                Arguments.of(
                        HttpMessageNotReadableException.class,
                        SpringMvcProblemType.MALFORMED_REQUEST
                ),
                Arguments.of(
                        MethodArgumentNotValidException.class,
                        SpringMvcProblemType.VALIDATION_FAILED
                ),
                Arguments.of(NoResourceFoundException.class, SpringMvcProblemType.NOT_FOUND),
                Arguments.of(
                        MaxUploadSizeExceededException.class,
                        SpringMvcProblemType.PAYLOAD_TOO_LARGE
                ),
                Arguments.of(
                        AsyncRequestTimeoutException.class,
                        SpringMvcProblemType.REQUEST_TIMEOUT
                ),
                Arguments.of(
                        MissingPathVariableException.class,
                        SpringMvcProblemType.INTERNAL_SERVER_ERROR
                ),
                Arguments.of(
                        ConversionNotSupportedException.class,
                        SpringMvcProblemType.INTERNAL_SERVER_ERROR
                ),
                Arguments.of(
                        HttpMessageNotWritableException.class,
                        SpringMvcProblemType.INTERNAL_SERVER_ERROR
                ),
                Arguments.of(
                        MethodValidationException.class,
                        SpringMvcProblemType.INTERNAL_SERVER_ERROR
                )
        );
    }
}
