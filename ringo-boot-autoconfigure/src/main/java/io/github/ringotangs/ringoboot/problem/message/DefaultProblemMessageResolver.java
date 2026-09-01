package io.github.ringotangs.ringoboot.problem.message;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.problem.ProblemTypeDefinition;

/**
 * 使用问题类型中的默认文案解析响应消息。
 */
public final class DefaultProblemMessageResolver implements ProblemMessageResolver {

    /**
     * 创建使用问题类型默认文案的消息解析器。
     */
    public DefaultProblemMessageResolver() {}

    @Override
    public ProblemMessages resolve(ProblemException exception) {
        ProblemTypeDefinition definition = exception.getProblemType().getDefinition();
        return new ProblemMessages(definition.title(), exception.getMessage());
    }
}
