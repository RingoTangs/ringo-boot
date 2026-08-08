package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.problem.ProblemDefinition;
import io.github.ringotangs.ringoboot.problem.ProblemType;
import io.github.ringotangs.ringoboot.problem.ProblemTypeUri;
import org.springframework.http.HttpStatus;

enum VerificationProblemType implements ProblemType {
    THROTTLED(
            "throttled",
            "Too many verification code requests",
            "Please retry after {0} seconds",
            HttpStatus.TOO_MANY_REQUESTS),
    INVALID_CODE(
            "invalid-code", "Invalid verification code", "The verification code is invalid", HttpStatus.BAD_REQUEST),
    TEST_MESSAGE_NOT_FOUND(
            "test-message-not-found",
            "Test email not found",
            "No test email was found for the requested address",
            HttpStatus.NOT_FOUND);

    private final ProblemDefinition definition;

    VerificationProblemType(String category, String title, String defaultDetail, HttpStatus status) {
        this.definition = ProblemDefinition.of(
                ProblemTypeUri.of("business", "verification", category),
                "problem.verification." + category,
                title,
                defaultDetail,
                status.value());
    }

    @Override
    public ProblemDefinition getDefinition() {
        return definition;
    }
}
