package io.github.ringotangs.springcommons.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.Objects;

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
        this.type = URI.create("urn:problem:mvc:" + category);
    }

    URI getType() {
        return type;
    }

    static @Nullable SpringMvcProblemType resolve(Exception exception) {
        Objects.requireNonNull(exception, "exception must not be null");
        if (exception instanceof MissingPathVariableException
                || exception instanceof ConversionNotSupportedException
                || exception instanceof HttpMessageNotWritableException
                || exception instanceof MethodValidationException) {
            return INTERNAL_SERVER_ERROR;
        }
        if (exception instanceof HandlerMethodValidationException validationException) {
            return validationException.isForReturnValue()
                    ? INTERNAL_SERVER_ERROR
                    : VALIDATION_FAILED;
        }
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return METHOD_NOT_ALLOWED;
        }
        if (exception instanceof HttpMediaTypeNotSupportedException) {
            return UNSUPPORTED_MEDIA_TYPE;
        }
        if (exception instanceof HttpMediaTypeNotAcceptableException) {
            return NOT_ACCEPTABLE;
        }
        if (exception instanceof MissingServletRequestParameterException
                || exception instanceof MissingServletRequestPartException
                || exception instanceof ServletRequestBindingException) {
            return MISSING_REQUEST_VALUE;
        }
        if (exception instanceof TypeMismatchException) {
            return INVALID_PARAMETER;
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return MALFORMED_REQUEST;
        }
        if (exception instanceof MethodArgumentNotValidException) {
            return VALIDATION_FAILED;
        }
        if (exception instanceof NoHandlerFoundException
                || exception instanceof NoResourceFoundException) {
            return NOT_FOUND;
        }
        if (exception instanceof MaxUploadSizeExceededException) {
            return PAYLOAD_TOO_LARGE;
        }
        if (exception instanceof AsyncRequestTimeoutException) {
            return REQUEST_TIMEOUT;
        }
        return null;
    }

}
