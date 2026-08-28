package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.ringotangs.ringoboot.autoconfigure.problem.ProblemMessageResolver;
import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.VerificationException;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationThrottledException;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitException;
import io.github.ringotangs.ringoboot.verification.limit.MissingIssueRateLimitRuleException;
import io.github.ringotangs.ringoboot.verification.sender.CodeDeliveryRejectedException;
import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

@ExtendWith(OutputCaptureExtension.class)
class VerificationExceptionHandlerTest {

    @Test
    void returnsSafeInternalErrorForGenerationFailure(CapturedOutput output) {
        VerificationExceptionHandler handler = createDefaultHandler();

        ProblemDetail problem = handler.handleVerificationException(
                new CodeGenerationException("random source contains secret diagnostics"));

        assertProblem(
                problem,
                500,
                "urn:problem:verification:generation-failed",
                "Verification code generation failed",
                "The verification service encountered an internal error");
        assertThat(problem.getDetail()).doesNotContain("secret");
        assertLogged(output, CodeGenerationException.class);
    }

    @Test
    void returnsSameSafeUnavailableProblemForSenderStoreAndRateLimitFailures(CapturedOutput output) {
        VerificationExceptionHandler handler = createDefaultHandler();

        ProblemDetail senderProblem = handler.handleVerificationException(
                new CodeSenderException("provider token=secret", new IllegalStateException("provider details")));
        ProblemDetail storeProblem = handler.handleVerificationException(
                new VerificationStoreException("redis password=secret", new IllegalStateException("redis details")));
        ProblemDetail rateLimitProblem = handler.handleVerificationException(new IssueRateLimitException(
                "lua script secret diagnostics", new IllegalStateException("redis details")));
        ProblemDetail rejectedProblem = handler.handleVerificationException(new CodeDeliveryRejectedException());

        assertServiceUnavailable(senderProblem);
        assertServiceUnavailable(storeProblem);
        assertServiceUnavailable(rateLimitProblem);
        assertServiceUnavailable(rejectedProblem);
        assertThat(senderProblem.getDetail()).doesNotContain("provider", "secret");
        assertThat(storeProblem.getDetail()).doesNotContain("redis", "secret");
        assertThat(rateLimitProblem.getDetail()).doesNotContain("lua", "redis", "secret");
        assertLogged(output, CodeSenderException.class);
        assertLogged(output, VerificationStoreException.class);
        assertLogged(output, IssueRateLimitException.class);
        assertLogged(output, CodeDeliveryRejectedException.class);
    }

    @Test
    void resolvesEveryVerificationExceptionToTheExpectedHandler() {
        ExceptionHandlerMethodResolver resolver =
                new ExceptionHandlerMethodResolver(VerificationExceptionHandler.class);

        assertHandler(resolver, new InvalidVerificationCodeException(), "handleInvalidVerificationCode");
        assertHandler(
                resolver, new VerificationThrottledException(Duration.ofSeconds(1)), "handleVerificationThrottled");
        assertHandler(resolver, new CodeGenerationException("internal"), "handleVerificationException");
        assertHandler(resolver, new CodeSenderException("internal"), "handleVerificationException");
        assertHandler(resolver, new CodeDeliveryRejectedException(), "handleVerificationException");
        assertHandler(resolver, new VerificationStoreException("internal"), "handleVerificationException");
        assertHandler(resolver, new IssueRateLimitException("internal"), "handleVerificationException");
        assertHandler(resolver, new MissingIssueRateLimitRuleException(), "handleVerificationException");
        assertHandler(resolver, new UnknownVerificationException("internal"), "handleVerificationException");
    }

    @Test
    void returnsSafeUnavailableProblemForUnknownVerificationException(CapturedOutput output) {
        VerificationExceptionHandler handler = createDefaultHandler();

        ProblemDetail problem = handler.handleVerificationException(new UnknownVerificationException("token=secret"));

        assertServiceUnavailable(problem);
        assertThat(problem.getDetail()).doesNotContain("token", "secret");
        assertLogged(output, UnknownVerificationException.class);
    }

    @Test
    void returnsSafeInternalErrorForMissingRateLimitRule(CapturedOutput output) {
        VerificationExceptionHandler handler = createDefaultHandler();
        MissingIssueRateLimitRuleException exception =
                new MissingIssueRateLimitRuleException(new VerificationKey("account", "login", "user@example.com"));

        ProblemDetail problem = handler.handleVerificationException(exception);

        assertProblem(
                problem,
                500,
                "urn:problem:verification:configuration-error",
                "Verification configuration error",
                "The verification service is not configured for this operation");
        assertThat(problem.getDetail()).doesNotContain("account", "login", "example.com");
        assertLogged(output, MissingIssueRateLimitRuleException.class);
    }

    @Test
    void mapsExpectedBusinessFailuresWithoutErrorLogging(CapturedOutput output) {
        VerificationExceptionHandler handler = createDefaultHandler();

        ResponseEntity<ProblemDetail> throttledResponse =
                handler.handleVerificationThrottled(new VerificationThrottledException(Duration.ofMillis(1201)));
        ProblemDetail throttled = throttledResponse.getBody();
        ProblemDetail invalid = handler.handleInvalidVerificationCode(new InvalidVerificationCodeException());

        assertThat(throttled).isNotNull();
        assertProblem(
                throttled,
                429,
                "urn:problem:business:verification:throttled",
                "Too many verification code requests",
                "Please retry after approximately 2 seconds");
        assertThat(throttledResponse.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))
                .isEqualTo("2");
        assertThat(throttled.getProperties()).containsEntry("retryAfterSeconds", 2L);
        assertProblem(
                invalid,
                400,
                "urn:problem:business:verification:invalid-code",
                "Invalid verification code",
                "The verification code is invalid");
        assertThat(output).doesNotContain("Verification operation failed");
    }

    @ParameterizedTest
    @CsvSource({
        "0, 'Please retry shortly'",
        "1, 'Please retry after 1 second'",
        "89, 'Please retry after approximately 89 seconds'",
        "90, 'Please retry after approximately 2 minutes'",
        "5399, 'Please retry after approximately 90 minutes'",
        "5400, 'Please retry after approximately 2 hours'",
        "129599, 'Please retry after approximately 36 hours'",
        "129600, 'Please retry after approximately 2 days'"
    })
    void formatsRetryAfterUsingAReadableUnit(long seconds, String expectedDetail) {
        ResponseEntity<ProblemDetail> response = createDefaultHandler()
                .handleVerificationThrottled(new VerificationThrottledException(Duration.ofSeconds(seconds)));

        assertThat(response.getBody())
                .isNotNull()
                .extracting(ProblemDetail::getDetail)
                .isEqualTo(expectedDetail);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo(Long.toString(seconds));
        assertThat(response.getBody().getProperties()).containsEntry("retryAfterSeconds", seconds);
    }

    @Test
    void roundsRetryAfterUpWithoutOverflowing() {
        ResponseEntity<ProblemDetail> fractional = createDefaultHandler()
                .handleVerificationThrottled(new VerificationThrottledException(Duration.ofNanos(1L)));
        ResponseEntity<ProblemDetail> maximum = createDefaultHandler()
                .handleVerificationThrottled(
                        new VerificationThrottledException(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999L)));

        assertThat(fractional.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
        assertThat(maximum.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo(Long.toString(Long.MAX_VALUE));
    }

    private VerificationExceptionHandler createDefaultHandler() {
        ProblemMessageResolver resolver = exception -> {
            var definition = exception.getProblemType().getDefinition();
            return new ProblemMessageResolver.ProblemMessages(definition.title(), exception.getMessage());
        };
        return new VerificationExceptionHandler(resolver);
    }

    private void assertServiceUnavailable(ProblemDetail problem) {
        assertProblem(
                problem,
                503,
                "urn:problem:verification:service-unavailable",
                "Verification service unavailable",
                "The verification service is temporarily unavailable");
    }

    private void assertProblem(ProblemDetail problem, int status, String type, String title, String detail) {
        assertEquals(status, problem.getStatus());
        assertEquals(URI.create(type), problem.getType());
        assertEquals(title, problem.getTitle());
        assertEquals(detail, problem.getDetail());
    }

    private void assertLogged(CapturedOutput output, Class<?> exceptionType) {
        assertThat(output)
                .contains("ERROR")
                .contains("Verification operation failed")
                .contains(exceptionType.getName());
    }

    private void assertHandler(
            ExceptionHandlerMethodResolver resolver, Exception exception, String expectedMethodName) {
        assertThat(resolver.resolveMethod(exception))
                .isNotNull()
                .extracting(java.lang.reflect.Method::getName)
                .isEqualTo(expectedMethodName);
    }

    private static final class UnknownVerificationException extends VerificationException {

        private UnknownVerificationException(String message) {
            super(message);
        }
    }
}
