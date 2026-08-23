package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将 {@link ProblemException} 转换为 RFC 9457 Problem Details 响应。 */
@RestControllerAdvice
@Order(0)
public class ProblemExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    /**
     * 使用问题消息解析器创建异常处理器。
     *
     * @param messageResolver 问题消息解析器
     */
    public ProblemExceptionHandler(ProblemMessageResolver messageResolver) {
        this.problemDetailFactory = new ProblemDetailFactory(messageResolver);
    }

    /**
     * 构建 Problem Details 响应。
     *
     * @param exception 问题异常
     * @return Problem Details 响应
     */
    @ExceptionHandler(ProblemException.class)
    public ProblemDetail handleProblemException(ProblemException exception) {
        return problemDetailFactory.create(exception);
    }
}
