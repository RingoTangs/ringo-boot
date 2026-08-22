package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.problem.ProblemTypeDefinition;

final class DefaultProblemMessageResolver implements ProblemMessageResolver {

    @Override
    public ProblemMessages resolve(ProblemException exception) {
        ProblemTypeDefinition definition = exception.getProblemType().getDefinition();
        return new ProblemMessages(definition.title(), exception.getMessage());
    }
}
