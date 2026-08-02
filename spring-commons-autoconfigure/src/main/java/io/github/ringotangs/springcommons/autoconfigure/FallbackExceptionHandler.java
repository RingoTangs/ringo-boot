package io.github.ringotangs.springcommons.autoconfigure;

import io.github.ringotangs.springcommons.core.ProblemDefinition;
import io.github.ringotangs.springcommons.core.ProblemException;
import io.github.ringotangs.springcommons.core.ProblemType;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * 将未被专用处理器处理的异常转换为安全的 Problem Details 响应。
 *
 * <p>Converts exceptions not handled by a dedicated handler into safe Problem Details
 * responses.</p>
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class FallbackExceptionHandler {


    private static final ProblemDefinition INTERNAL_SERVER_ERROR_DEFINITION =
            ProblemDefinition.of(
                    "urn:problem:spring-commons:internal-server-error",
                    "problem.internal-server-error",
                    "Internal server error",
                    "An unexpected error occurred",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );

    private static final ProblemType INTERNAL_SERVER_ERROR =
            () -> INTERNAL_SERVER_ERROR_DEFINITION;

    private final ProblemExceptionHandler problemExceptionHandler;
    private final MessageSource messageSource;
    private final ProblemExceptionHandlerProperties properties;

    /**
     * 创建全局异常兜底处理器。
     *
     * <p>Creates the global fallback exception handler.</p>
     */
    public FallbackExceptionHandler(
            ProblemExceptionHandler problemExceptionHandler,
            MessageSource messageSource,
            ProblemExceptionHandlerProperties properties
    ) {
        this.problemExceptionHandler = Objects.requireNonNull(
                problemExceptionHandler,
                "problemExceptionHandler must not be null"
        );
        this.messageSource = Objects.requireNonNull(
                messageSource,
                "messageSource must not be null"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "properties must not be null"
        );
    }

    /**
     * 保留 Spring 框架错误响应，并安全处理其他未知异常。
     *
     * <p>Preserves Spring framework error responses and safely handles all other
     * unexpected exceptions.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception exception) {
        Objects.requireNonNull(exception, "exception must not be null");
        if (exception instanceof ErrorResponse errorResponse) {
            ProblemDetail body = properties.isI18nEnabled()
                    ? errorResponse.updateAndGetBody(
                            messageSource,
                            LocaleContextHolder.getLocale()
                    )
                    : errorResponse.getBody();
            return new ResponseEntity<>(
                    body,
                    errorResponse.getHeaders(),
                    errorResponse.getStatusCode()
            );
        }

        ProblemDetail body = problemExceptionHandler.handleProblemException(
                ProblemException.withCause(INTERNAL_SERVER_ERROR, exception)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
