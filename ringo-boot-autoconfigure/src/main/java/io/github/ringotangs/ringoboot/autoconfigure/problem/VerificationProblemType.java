package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.problem.ProblemDefinition;
import io.github.ringotangs.ringoboot.problem.ProblemType;
import io.github.ringotangs.ringoboot.problem.ProblemTypeUri;
import org.springframework.http.HttpStatus;

/** 验证码技术异常使用的稳定 Problem Type。 / Stable Problem Types for verification technical failures. */
enum VerificationProblemType implements ProblemType {
    GENERATION_FAILED(
            "generation-failed",
            "Verification code generation failed",
            "The verification service encountered an internal error",
            HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(
            "service-unavailable",
            "Verification service unavailable",
            "The verification service is temporarily unavailable",
            HttpStatus.SERVICE_UNAVAILABLE);

    private final ProblemDefinition definition;

    VerificationProblemType(String category, String title, String defaultDetail, HttpStatus status) {
        this.definition = ProblemDefinition.of(
                ProblemTypeUri.of("verification", category),
                "problem.verification." + category,
                title,
                defaultDetail,
                status.value());
    }

    /** {@inheritDoc} */
    @Override
    public ProblemDefinition getDefinition() {
        return definition;
    }
}
