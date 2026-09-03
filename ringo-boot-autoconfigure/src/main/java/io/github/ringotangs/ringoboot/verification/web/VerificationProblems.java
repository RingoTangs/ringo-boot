package io.github.ringotangs.ringoboot.verification.web;

import io.github.ringotangs.ringoboot.problem.ProblemDescriptor;
import io.github.ringotangs.ringoboot.problem.ProblemTypeUri;
import java.net.URI;
import org.springframework.http.HttpStatus;

/**
 * 验证码异常使用的稳定 Problem 描述。
 */
final class VerificationProblems {

    static final ProblemDescriptor THROTTLED = descriptor(
            ProblemTypeUri.of("business", "verification", "throttled"),
            "throttled",
            "Too many verification code requests",
            "Please retry later",
            HttpStatus.TOO_MANY_REQUESTS);

    static final ProblemDescriptor INVALID_CODE = descriptor(
            ProblemTypeUri.of("business", "verification", "invalid-code"),
            "invalid-code",
            "Invalid verification code",
            "The verification code is invalid",
            HttpStatus.BAD_REQUEST);

    static final ProblemDescriptor GENERATION_FAILED = descriptor(
            ProblemTypeUri.of("verification", "generation-failed"),
            "generation-failed",
            "Verification code generation failed",
            "The verification service encountered an internal error",
            HttpStatus.INTERNAL_SERVER_ERROR);

    static final ProblemDescriptor CONFIGURATION_ERROR = descriptor(
            ProblemTypeUri.of("verification", "configuration-error"),
            "configuration-error",
            "Verification configuration error",
            "The verification service is not configured for this operation",
            HttpStatus.INTERNAL_SERVER_ERROR);

    static final ProblemDescriptor SERVICE_UNAVAILABLE = descriptor(
            ProblemTypeUri.of("verification", "service-unavailable"),
            "service-unavailable",
            "Verification service unavailable",
            "The verification service is temporarily unavailable",
            HttpStatus.SERVICE_UNAVAILABLE);

    private VerificationProblems() {}

    private static ProblemDescriptor descriptor(
            URI type, String category, String title, String detail, HttpStatus status) {
        return ProblemDescriptor.of(type, "problem.verification." + category, title, detail, status.value());
    }
}
