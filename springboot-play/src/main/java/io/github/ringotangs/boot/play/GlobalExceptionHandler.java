package io.github.ringotangs.boot.play;

import io.github.ringotangs.commons.core.ProblemException;
import io.github.ringotangs.commons.core.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProblemException.class)
    public ProblemDetail handleProblemException(
            ProblemException exception,
            HttpServletRequest request
    ) {
        ProblemType problemType = exception.getProblemType();
        HttpStatus status = HttpStatus.valueOf(problemType.getHttpStatus());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                exception.getMessage()
        );
        problem.setType(problemType.getType());
        problem.setTitle(problemType.getTitle());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
