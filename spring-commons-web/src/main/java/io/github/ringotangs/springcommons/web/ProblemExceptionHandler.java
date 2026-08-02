package io.github.ringotangs.springcommons.web;

import io.github.ringotangs.springcommons.core.ProblemDefinition;
import io.github.ringotangs.springcommons.core.ProblemException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;
import java.util.Objects;

/**
 * 将 {@link ProblemException} 转换为经过国际化处理的 RFC 9457 Problem Details 响应。
 *
 * <p>Converts {@link ProblemException} instances into localized RFC 9457 Problem Details
 * responses.</p>
 */
@RestControllerAdvice
public class ProblemExceptionHandler {

    private static final String TITLE_SUFFIX = ".title";
    private static final String DETAIL_SUFFIX = ".detail";

    private final MessageSource messageSource;

    /**
     * 使用应用的消息源创建异常处理器。
     *
     * <p>Creates the exception handler with the application's message source.</p>
     *
     * @param messageSource 消息源 / the message source
     */
    public ProblemExceptionHandler(MessageSource messageSource) {
        this.messageSource = Objects.requireNonNull(
                messageSource,
                "messageSource must not be null"
        );
    }

    /**
     * 根据当前请求语言构建 Problem Details 响应。
     *
     * <p>Builds a Problem Details response for the current request locale.</p>
     *
     * @param exception 问题异常 / the problem exception
     * @return 国际化的 Problem Details / the localized Problem Details
     */
    @ExceptionHandler(ProblemException.class)
    public ProblemDetail handleProblemException(ProblemException exception) {
        ProblemDefinition definition = exception.getProblemType().getDefinition();
        Locale locale = LocaleContextHolder.getLocale();
        String title = messageSource.getMessage(
                definition.messageCode() + TITLE_SUFFIX,
                null,
                definition.title(),
                locale
        );
        String detail = resolveDetail(exception, definition, locale);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(definition.httpStatus()),
                detail
        );
        problem.setType(definition.type());
        problem.setTitle(title);
        return problem;
    }

    private String resolveDetail(
            ProblemException exception,
            ProblemDefinition definition,
            Locale locale
    ) {
        String detailOverride = exception.getDetailOverride();
        if (detailOverride != null) {
            return detailOverride;
        }
        return Objects.requireNonNull(messageSource.getMessage(
                definition.messageCode() + DETAIL_SUFFIX,
                exception.getDetailArguments().toArray(),
                definition.defaultDetail(),
                locale
        ));
    }
}
