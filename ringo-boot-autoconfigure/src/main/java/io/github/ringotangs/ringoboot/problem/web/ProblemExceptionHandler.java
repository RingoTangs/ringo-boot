package io.github.ringotangs.ringoboot.problem.web;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将 {@link ProblemException} 转换为 RFC 9457 Problem Details 响应。
 */
@RestControllerAdvice
@Order(0)
public class ProblemExceptionHandler {

    /**
     * 构建 Problem Details 响应。
     *
     * @param exception 问题异常
     * @return Problem Details 响应
     */
    @ExceptionHandler(ProblemException.class)
    public ProblemDetail handleProblemException(ProblemException exception) {
        return ProblemDetailFactory.create(exception.getProblemType(), exception.getMessage());
    }
}
