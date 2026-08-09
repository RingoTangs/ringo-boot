package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.problem.ProblemTypeUri;
import java.net.URI;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
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
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

enum SpringMvcProblemType {
    METHOD_NOT_ALLOWED("method-not-allowed"),
    UNSUPPORTED_MEDIA_TYPE("unsupported-media-type"),
    NOT_ACCEPTABLE("not-acceptable"),
    MISSING_REQUEST_VALUE("missing-request-value"),
    INVALID_PARAMETER("invalid-parameter"),
    MALFORMED_REQUEST("malformed-request"),
    VALIDATION_FAILED("validation-failed"),
    NOT_FOUND("not-found"),
    PAYLOAD_TOO_LARGE("payload-too-large"),
    REQUEST_TIMEOUT("request-timeout"),
    INTERNAL_SERVER_ERROR("internal-server-error");

    private final URI type;

    SpringMvcProblemType(String category) {
        this.type = ProblemTypeUri.of("mvc", category);
    }

    URI getType() {
        return type;
    }

    static @Nullable SpringMvcProblemType resolve(Exception exception) {
        Objects.requireNonNull(exception, "exception must not be null");
        return switch (exception) {
            case MissingPathVariableException ignored -> INTERNAL_SERVER_ERROR;
            case ConversionNotSupportedException ignored -> INTERNAL_SERVER_ERROR;
            case HttpMessageNotWritableException ignored -> INTERNAL_SERVER_ERROR;
            case MethodValidationException ignored -> INTERNAL_SERVER_ERROR;
            case HandlerMethodValidationException validationException ->
                validationException.isForReturnValue() ? INTERNAL_SERVER_ERROR : VALIDATION_FAILED;
            case HttpRequestMethodNotSupportedException ignored -> METHOD_NOT_ALLOWED;
            case HttpMediaTypeNotSupportedException ignored -> UNSUPPORTED_MEDIA_TYPE;
            case HttpMediaTypeNotAcceptableException ignored -> NOT_ACCEPTABLE;
            case MissingServletRequestPartException ignored -> MISSING_REQUEST_VALUE;
            case ServletRequestBindingException ignored -> MISSING_REQUEST_VALUE;
            case TypeMismatchException ignored -> INVALID_PARAMETER;
            case HttpMessageNotReadableException ignored -> MALFORMED_REQUEST;
            case MethodArgumentNotValidException ignored -> VALIDATION_FAILED;
            case NoHandlerFoundException ignored -> NOT_FOUND;
            case NoResourceFoundException ignored -> NOT_FOUND;
            case MaxUploadSizeExceededException ignored -> PAYLOAD_TOO_LARGE;
            case AsyncRequestTimeoutException ignored -> REQUEST_TIMEOUT;
            default -> null;
        };
    }
}
