package io.github.ringotangs.springcommons.web;

import io.github.ringotangs.springcommons.core.ProblemDefinition;
import io.github.ringotangs.springcommons.core.ProblemException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * 将 {@link ProblemException} 转换为 RFC 9457 Problem Details 响应。
 *
 * <p>Converts {@link ProblemException} instances into RFC 9457 Problem Details
 * responses.</p>
 */
@RestControllerAdvice
public class ProblemExceptionHandler {

    private final ProblemMessageResolver messageResolver;

    /**
     * 使用问题消息解析器创建异常处理器。
     *
     * <p>Creates the exception handler with a problem message resolver.</p>
     *
     * @param messageResolver 问题消息解析器 / the problem message resolver
     */
    public ProblemExceptionHandler(ProblemMessageResolver messageResolver) {
        this.messageResolver = Objects.requireNonNull(
                messageResolver,
                "messageResolver must not be null"
        );
    }

    /**
     * 构建 Problem Details 响应。
     *
     * <p>Builds a Problem Details response.</p>
     *
     * @param exception 问题异常 / the problem exception
     * @return Problem Details 响应 / the Problem Details response
     */
    @ExceptionHandler(ProblemException.class)
    public ProblemDetail handleProblemException(ProblemException exception) {
        ProblemDefinition definition = exception.getProblemType().getDefinition();
        ProblemMessageResolver.ProblemMessages messages = messageResolver.resolve(exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(definition.httpStatus()),
                messages.detail()
        );
        problem.setType(definition.type());
        problem.setTitle(messages.title());
        return problem;
    }
}
