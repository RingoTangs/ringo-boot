package io.github.ringotangs.springcommons.autoconfigure;

import io.github.ringotangs.springcommons.core.ProblemException;
import org.springframework.http.ProblemDetail;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将 {@link ProblemException} 转换为 RFC 9457 Problem Details 响应。
 *
 * <p>Converts {@link ProblemException} instances into RFC 9457 Problem Details
 * responses.</p>
 */
@RestControllerAdvice
@Order(0)
public class ProblemExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    /**
     * 使用问题消息解析器创建异常处理器。
     *
     * <p>Creates the exception handler with a problem message resolver.</p>
     *
     * @param messageResolver 问题消息解析器 / the problem message resolver
     */
    public ProblemExceptionHandler(ProblemMessageResolver messageResolver) {
        this.problemDetailFactory = new ProblemDetailFactory(messageResolver);
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
        return problemDetailFactory.create(exception);
    }
}
