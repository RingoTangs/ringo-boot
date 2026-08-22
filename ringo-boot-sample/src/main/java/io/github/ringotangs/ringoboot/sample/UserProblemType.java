package io.github.ringotangs.ringoboot.sample;

import io.github.ringotangs.ringoboot.problem.ProblemType;
import io.github.ringotangs.ringoboot.problem.ProblemTypeDefinition;
import io.github.ringotangs.ringoboot.problem.ProblemTypeUri;
import org.springframework.http.HttpStatus;

public enum UserProblemType implements ProblemType {
    INVALID_USER_ID("invalid-id", "Invalid user id", "User id must be greater than 0", HttpStatus.BAD_REQUEST),

    USER_NOT_FOUND("not-found", "User not found", "User {0} does not exist", HttpStatus.NOT_FOUND);

    private final ProblemTypeDefinition definition;

    UserProblemType(String category, String title, String defaultDetail, HttpStatus httpStatus) {
        this.definition = ProblemTypeDefinition.of(
                ProblemTypeUri.of("business", "user", category),
                "problem.user." + category,
                title,
                defaultDetail,
                httpStatus.value());
    }

    @Override
    public ProblemTypeDefinition getDefinition() {
        return definition;
    }
}
