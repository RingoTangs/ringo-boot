package io.github.ringotangs.springcommons.sample;

import io.github.ringotangs.springcommons.core.ProblemDefinition;
import io.github.ringotangs.springcommons.core.ProblemType;
import org.springframework.http.HttpStatus;

public enum UserProblemType implements ProblemType {

    INVALID_USER_ID(ProblemDefinition.of(
            "urn:problem:business:user:invalid-id",
            "problem.user.invalid-id",
            "Invalid user id",
            "User id must be greater than 0",
            HttpStatus.BAD_REQUEST.value()
    )),

    USER_NOT_FOUND(ProblemDefinition.of(
            "urn:problem:business:user:not-found",
            "problem.user.not-found",
            "User not found",
            "User {0} does not exist",
            HttpStatus.NOT_FOUND.value()
    ));

    private final ProblemDefinition definition;

    UserProblemType(ProblemDefinition definition) {
        this.definition = definition;
    }

    @Override
    public ProblemDefinition getDefinition() {
        return definition;
    }
}
