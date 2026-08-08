package io.github.ringotangs.springcommons.sample;

import io.github.ringotangs.springcommons.core.ProblemDefinition;
import io.github.ringotangs.springcommons.core.ProblemType;
import io.github.ringotangs.springcommons.core.ProblemTypeUri;
import org.springframework.http.HttpStatus;

public enum UserProblemType implements ProblemType {
    INVALID_USER_ID("invalid-id", "Invalid user id", "User id must be greater than 0", HttpStatus.BAD_REQUEST),

    USER_NOT_FOUND("not-found", "User not found", "User {0} does not exist", HttpStatus.NOT_FOUND);

    private final ProblemDefinition definition;

    UserProblemType(String category, String title, String defaultDetail, HttpStatus httpStatus) {
        this.definition = ProblemDefinition.of(
                ProblemTypeUri.of("business", "user", category),
                "problem.user." + category,
                title,
                defaultDetail,
                httpStatus.value());
    }

    @Override
    public ProblemDefinition getDefinition() {
        return definition;
    }
}
