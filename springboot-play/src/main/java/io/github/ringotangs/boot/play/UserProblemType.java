package io.github.ringotangs.boot.play;

import io.github.ringotangs.commons.core.ProblemDefinition;
import io.github.ringotangs.commons.core.ProblemType;
import org.springframework.http.HttpStatus;

public enum UserProblemType implements ProblemType {

    INVALID_USER_ID(ProblemDefinition.of(
            "urn:problem:spring-commons:user:invalid-id",
            "Invalid user id",
            "User id must be greater than 0",
            HttpStatus.BAD_REQUEST.value()
    )),

    USER_NOT_FOUND(ProblemDefinition.of(
            "urn:problem:spring-commons:user:not-found",
            "User not found",
            "The requested user does not exist",
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
