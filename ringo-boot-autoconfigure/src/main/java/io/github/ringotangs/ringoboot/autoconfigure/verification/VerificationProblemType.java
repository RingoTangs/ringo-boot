package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.problem.ProblemType;
import io.github.ringotangs.ringoboot.problem.ProblemTypeDefinition;
import io.github.ringotangs.ringoboot.problem.ProblemTypeUri;
import org.springframework.http.HttpStatus;

/**
 * 验证码异常使用的稳定 Problem Type。
 */
enum VerificationProblemType implements ProblemType {
    THROTTLED(
            ProblemTypeUri.of("business", "verification", "throttled"),
            "throttled",
            "Too many verification code requests",
            "{0,choice,0#Please retry shortly|1#Please retry after 1 second|2#Please retry after approximately {1} seconds|90#Please retry after approximately {2} minutes|5400#Please retry after approximately {3} hours|129600#Please retry after approximately {4} days}",
            HttpStatus.TOO_MANY_REQUESTS),
    INVALID_CODE(
            ProblemTypeUri.of("business", "verification", "invalid-code"),
            "invalid-code",
            "Invalid verification code",
            "The verification code is invalid",
            HttpStatus.BAD_REQUEST),
    GENERATION_FAILED(
            ProblemTypeUri.of("verification", "generation-failed"),
            "generation-failed",
            "Verification code generation failed",
            "The verification service encountered an internal error",
            HttpStatus.INTERNAL_SERVER_ERROR),
    CONFIGURATION_ERROR(
            ProblemTypeUri.of("verification", "configuration-error"),
            "configuration-error",
            "Verification configuration error",
            "The verification service is not configured for this operation",
            HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(
            ProblemTypeUri.of("verification", "service-unavailable"),
            "service-unavailable",
            "Verification service unavailable",
            "The verification service is temporarily unavailable",
            HttpStatus.SERVICE_UNAVAILABLE);

    private final ProblemTypeDefinition definition;

    VerificationProblemType(java.net.URI type, String category, String title, String defaultDetail, HttpStatus status) {
        this.definition = ProblemTypeDefinition.of(
                type, "problem.verification." + category, title, defaultDetail, status.value());
    }

    /**
     * 返回当前异常类型的定义。
     */
    @Override
    public ProblemTypeDefinition getDefinition() {
        return definition;
    }
}
