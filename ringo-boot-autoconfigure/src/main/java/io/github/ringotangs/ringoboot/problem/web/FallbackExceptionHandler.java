package io.github.ringotangs.ringoboot.problem.web;

import io.github.ringotangs.ringoboot.problem.ProblemDescriptor;
import io.github.ringotangs.ringoboot.problem.ProblemTypeUri;
import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将未被专用处理器处理的异常转换为安全的 Problem Details 响应。
 *
 * <p>应用自定义 Advice、{@link ProblemExceptionHandler} 和
 * {@link SpringMvcExceptionHandler} 优先处理其负责的异常，最后由本处理器兜底。</p>
 */
@RestControllerAdvice
@Order
public class FallbackExceptionHandler {

    private static final Log logger = LogFactory.getLog(FallbackExceptionHandler.class);

    private static final ProblemDescriptor INTERNAL_SERVER_ERROR = ProblemDescriptor.of(
            ProblemTypeUri.of("fallback", "internal-server-error"),
            "problem.internal-server-error",
            "Internal server error",
            "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR.value());

    /**
     * 保留 Spring 框架错误响应，并安全处理其他未知异常。
     *
     * @param exception 待处理的异常
     * @return Problem Details 响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception exception) {
        Objects.requireNonNull(exception, "exception must not be null");
        if (exception instanceof ErrorResponse errorResponse) {
            return new ResponseEntity<>(
                    errorResponse.getBody(), errorResponse.getHeaders(), errorResponse.getStatusCode());
        }

        logger.error("Unhandled exception", exception);
        ProblemDetail body = ProblemDetailFactory.create(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.defaultDetail());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
