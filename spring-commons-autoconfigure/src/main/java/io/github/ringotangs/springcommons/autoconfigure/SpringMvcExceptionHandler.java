package io.github.ringotangs.springcommons.autoconfigure;

import io.github.ringotangs.springcommons.core.ProblemDefinition;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;

/**
 * 将常见 Spring MVC 异常转换为稳定、安全的 Problem Details 响应。
 *
 * <p>Converts common Spring MVC exceptions into stable and safe Problem Details
 * responses.</p>
 */
@RestControllerAdvice
@Order(0)
public class SpringMvcExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSource messageSource;
    private final ExceptionHandlerProperties properties;

    /**
     * 使用消息源和异常处理配置创建 Spring MVC 异常处理器。
     *
     * <p>Creates the Spring MVC exception handler with a message source and exception
     * handling properties.</p>
     */
    public SpringMvcExceptionHandler(
            MessageSource messageSource,
            ExceptionHandlerProperties properties
    ) {
        this.messageSource = Objects.requireNonNull(
                messageSource,
                "messageSource must not be null"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "properties must not be null"
        );
        setMessageSource(messageSource);
    }

    @Nullable
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        SpringMvcProblemType problemType = SpringMvcProblemType.resolve(exception);
        if (problemType == null) {
            return super.handleExceptionInternal(
                    exception,
                    body,
                    headers,
                    statusCode,
                    request
            );
        }

        if (problemType == SpringMvcProblemType.INTERNAL_SERVER_ERROR) {
            logger.error("Unhandled Spring MVC exception", exception);
        }

        return super.handleExceptionInternal(
                exception,
                createProblemDetail(problemType, statusCode),
                headers,
                statusCode,
                request
        );
    }

    private ProblemDetail createProblemDetail(
            SpringMvcProblemType problemType,
            HttpStatusCode statusCode
    ) {
        ProblemDefinition definition = problemType.getDefinition();
        String title = definition.title();
        String detail = definition.defaultDetail();
        if (properties.isI18nEnabled()) {
            title = messageSource.getMessage(
                    definition.messageCode() + ".title",
                    null,
                    title,
                    LocaleContextHolder.getLocale()
            );
            detail = messageSource.getMessage(
                    definition.messageCode() + ".detail",
                    null,
                    detail,
                    LocaleContextHolder.getLocale()
            );
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                statusCode,
                Objects.requireNonNull(detail)
        );
        problemDetail.setType(definition.type());
        problemDetail.setTitle(Objects.requireNonNull(title));
        return problemDetail;
    }
}
