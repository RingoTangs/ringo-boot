package io.github.ringotangs.ringoboot.problem.autoconfigure;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 为 Spring MVC 内置异常补充稳定的 Problem Details {@code type}。
 *
 * <p>4xx 与可预期的超时响应保留 Spring MVC 生成的状态、Header 和诊断文本；
 * 框架内部错误返回固定的脱敏内容，并在服务端记录完整异常。</p>
 */
@RestControllerAdvice
@Order(0)
public class SpringMvcExceptionHandler extends ResponseEntityExceptionHandler {

    private final ValidationErrorExtractor validationErrorExtractor;

    /**
     * 使用消息源和异常处理配置创建 Spring MVC 异常处理器。
     *
     * @param messageSource Spring 消息源
     * @param properties    异常处理配置
     */
    public SpringMvcExceptionHandler(MessageSource messageSource, ProblemProperties properties) {
        setMessageSource(Objects.requireNonNull(messageSource, "messageSource must not be null"));
        this.validationErrorExtractor = new ValidationErrorExtractor(messageSource);
        Objects.requireNonNull(properties, "properties must not be null");
    }

    @Nullable
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        SpringMvcProblemType problemType = SpringMvcProblemType.resolve(exception);
        if (problemType == null) {
            return super.handleExceptionInternal(exception, body, headers, statusCode, request);
        }

        if (problemType == SpringMvcProblemType.INTERNAL_SERVER_ERROR) {
            logger.error("Unhandled Spring MVC exception", exception);
            return super.handleExceptionInternal(
                    exception, createSafeInternalServerError(), headers, statusCode, request);
        }

        ResponseEntity<Object> response = super.handleExceptionInternal(exception, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problemDetail) {
            problemDetail.setType(problemType.getType());
            if (problemType == SpringMvcProblemType.VALIDATION_FAILED) {
                var errors = validationErrorExtractor.extract(exception, LocaleContextHolder.getLocale());
                if (!errors.isEmpty()) {
                    problemDetail.setProperty("errors", errors);
                }
            }
        }
        return response;
    }

    private ProblemDetail createSafeInternalServerError() {
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setType(SpringMvcProblemType.INTERNAL_SERVER_ERROR.getType());
        problemDetail.setTitle("Internal server error");
        return problemDetail;
    }
}
