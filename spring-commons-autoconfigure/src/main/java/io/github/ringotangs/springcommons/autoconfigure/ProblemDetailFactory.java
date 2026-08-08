package io.github.ringotangs.springcommons.autoconfigure;

import io.github.ringotangs.springcommons.core.ProblemDefinition;
import io.github.ringotangs.springcommons.core.ProblemException;
import java.util.Objects;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * 根据 ProblemException 创建 Problem Details。
 *
 * <p>Creates Problem Details from {@link ProblemException} instances.</p>
 */
final class ProblemDetailFactory {

    private final ProblemMessageResolver messageResolver;

    ProblemDetailFactory(ProblemMessageResolver messageResolver) {
        this.messageResolver = Objects.requireNonNull(messageResolver, "messageResolver must not be null");
    }

    ProblemDetail create(ProblemException exception) {
        Objects.requireNonNull(exception, "exception must not be null");
        ProblemDefinition definition = exception.getProblemType().getDefinition();
        ProblemMessageResolver.ProblemMessages messages = messageResolver.resolve(exception);
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(definition.httpStatus()), messages.detail());
        problem.setType(definition.type());
        problem.setTitle(messages.title());
        return problem;
    }
}
