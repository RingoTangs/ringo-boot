package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.problem.ProblemDefinition;
import io.github.ringotangs.ringoboot.problem.ProblemException;

final class DefaultProblemMessageResolver implements ProblemMessageResolver {

    @Override
    public ProblemMessages resolve(ProblemException exception) {
        ProblemDefinition definition = exception.getProblemType().getDefinition();
        return new ProblemMessages(definition.title(), exception.getMessage());
    }
}
