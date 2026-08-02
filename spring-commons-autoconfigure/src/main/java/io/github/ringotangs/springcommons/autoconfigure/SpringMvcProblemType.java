package io.github.ringotangs.springcommons.autoconfigure;

import io.github.ringotangs.springcommons.core.ProblemDefinition;
import io.github.ringotangs.springcommons.core.ProblemType;
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

import java.util.Objects;

enum SpringMvcProblemType implements ProblemType {

    METHOD_NOT_ALLOWED(definition(
            "method-not-allowed",
            "Method not allowed",
            "The request method is not supported for this resource",
            405
    )),
    UNSUPPORTED_MEDIA_TYPE(definition(
            "unsupported-media-type",
            "Unsupported media type",
            "The request content type is not supported",
            415
    )),
    NOT_ACCEPTABLE(definition(
            "not-acceptable",
            "Not acceptable",
            "No acceptable response representation is available",
            406
    )),
    MISSING_REQUEST_VALUE(definition(
            "missing-request-value",
            "Missing request value",
            "A required request value is missing",
            400
    )),
    INVALID_PARAMETER(definition(
            "invalid-parameter",
            "Invalid parameter",
            "A request parameter has an invalid value",
            400
    )),
    MALFORMED_REQUEST(definition(
            "malformed-request",
            "Malformed request",
            "The request body could not be read",
            400
    )),
    VALIDATION_FAILED(definition(
            "validation-failed",
            "Validation failed",
            "The request validation failed",
            400
    )),
    NOT_FOUND(definition(
            "not-found",
            "Resource not found",
            "The requested resource was not found",
            404
    )),
    PAYLOAD_TOO_LARGE(definition(
            "payload-too-large",
            "Payload too large",
            "The request payload is too large",
            413
    )),
    REQUEST_TIMEOUT(definition(
            "request-timeout",
            "Request timeout",
            "The request could not be completed in time",
            503
    )),
    INTERNAL_SERVER_ERROR(ProblemDefinition.of(
            "urn:problem:mvc:internal-server-error",
            "problem.internal-server-error",
            "Internal server error",
            "An unexpected error occurred",
            500
    ));

    private final ProblemDefinition definition;

    SpringMvcProblemType(ProblemDefinition definition) {
        this.definition = definition;
    }

    @Override
    public ProblemDefinition getDefinition() {
        return definition;
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

    private static ProblemDefinition definition(
            String name,
            String title,
            String detail,
            int status
    ) {
        return ProblemDefinition.of(
                "urn:problem:mvc:" + name,
                "problem.spring-mvc." + name,
                title,
                detail,
                status
        );
    }
}
