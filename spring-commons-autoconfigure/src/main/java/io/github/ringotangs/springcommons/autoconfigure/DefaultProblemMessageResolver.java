package io.github.ringotangs.springcommons.autoconfigure;

import io.github.ringotangs.springcommons.core.ProblemDefinition;
import io.github.ringotangs.springcommons.core.ProblemException;

final class DefaultProblemMessageResolver implements ProblemMessageResolver {

    @Override
    public ProblemMessages resolve(ProblemException exception) {
        ProblemDefinition definition = exception.getProblemType().getDefinition();
        return new ProblemMessages(definition.title(), exception.getMessage());
    }
}
