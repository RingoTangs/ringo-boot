package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.problem.ProblemTypeDefinition;

/**
 * 使用问题类型中的默认文案解析响应消息。
 */
final class DefaultProblemMessageResolver implements ProblemMessageResolver {

    @Override
    public ProblemMessages resolve(ProblemException exception) {
        ProblemTypeDefinition definition = exception.getProblemType().getDefinition();
        return new ProblemMessages(definition.title(), exception.getMessage());
    }
}
