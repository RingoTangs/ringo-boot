package io.github.ringotangs.ringoboot.sample;

import io.github.ringotangs.ringoboot.problem.ProblemDescriptor;
import io.github.ringotangs.ringoboot.problem.ProblemTypeUri;
import org.springframework.http.HttpStatus;

public final class UserProblems {

    public static final ProblemDescriptor INVALID_USER_ID =
            descriptor("invalid-id", "Invalid user id", "User id must be greater than 0", HttpStatus.BAD_REQUEST);

    public static final ProblemDescriptor USER_NOT_FOUND =
            descriptor("not-found", "User not found", "User {0} does not exist", HttpStatus.NOT_FOUND);

    private UserProblems() {}

    private static ProblemDescriptor descriptor(String category, String title, String detail, HttpStatus status) {
        return ProblemDescriptor.of(
                ProblemTypeUri.of("business", "user", category),
                "problem.user." + category,
                title,
                detail,
                status.value());
    }
}
