package io.github.ringotangs.ringoboot.verification.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.VerificationException;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.channel.CodeSendRejectedException;
import io.github.ringotangs.ringoboot.verification.channel.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitExceededException;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitStoreException;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitViolation;
import io.github.ringotangs.ringoboot.verification.limit.MissingIssueLimitRuleException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
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
    void returnsSameSafeUnavailableProblemForSenderStoreAndLimitFailures(CapturedOutput output) {
        VerificationExceptionHandler handler = createDefaultHandler();

        ProblemDetail senderProblem = handler.handleVerificationException(new CodeSenderException(
                VerificationChannel.EMAIL, "provider token=secret", new IllegalStateException("provider details")));
        ProblemDetail storeProblem = handler.handleVerificationException(
                new VerificationStoreException("redis password=secret", new IllegalStateException("redis details")));
        ProblemDetail limitProblem = handler.handleVerificationException(new IssueLimitStoreException(
                "lua script secret diagnostics", new IllegalStateException("redis details")));
        ProblemDetail rejectedProblem =
                handler.handleVerificationException(new CodeSendRejectedException(VerificationChannel.SMS));

        assertServiceUnavailable(senderProblem);
        assertServiceUnavailable(storeProblem);
        assertServiceUnavailable(limitProblem);
        assertServiceUnavailable(rejectedProblem);
        assertThat(senderProblem.getDetail()).doesNotContain("provider", "secret");
        assertThat(storeProblem.getDetail()).doesNotContain("redis", "secret");
        assertThat(limitProblem.getDetail()).doesNotContain("lua", "redis", "secret");
        assertLogged(output, CodeSenderException.class);
        assertLogged(output, VerificationStoreException.class);
        assertLogged(output, IssueLimitStoreException.class);
        assertLogged(output, CodeSendRejectedException.class);
        assertThat(output).contains("channel=email", "channel=sms");
    }

    @Test
    void resolvesEveryVerificationExceptionToTheExpectedHandler() {
        ExceptionHandlerMethodResolver resolver =
                new ExceptionHandlerMethodResolver(VerificationExceptionHandler.class);

        assertHandler(resolver, new InvalidVerificationCodeException(), "handleInvalidVerificationCode");
        assertHandler(resolver, exceeded(Duration.ofSeconds(1)), "handleIssueLimitExceeded");
        assertHandler(resolver, new CodeGenerationException("internal"), "handleVerificationException");
        assertHandler(
                resolver,
                new CodeSenderException(VerificationChannel.EMAIL, "internal"),
                "handleVerificationException");
        assertHandler(resolver, new CodeSendRejectedException(VerificationChannel.SMS), "handleVerificationException");
        assertHandler(resolver, new VerificationStoreException("internal"), "handleVerificationException");
        assertHandler(resolver, new IssueLimitStoreException("internal"), "handleVerificationException");
        assertHandler(resolver, new MissingIssueLimitRuleException(), "handleVerificationException");
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
    void returnsSafeInternalErrorForMissingLimitRule(CapturedOutput output) {
        VerificationExceptionHandler handler = createDefaultHandler();
        MissingIssueLimitRuleException exception =
                new MissingIssueLimitRuleException(new VerificationKey("account", "login", "user@example.com"));

        ProblemDetail problem = handler.handleVerificationException(exception);

        assertProblem(
                problem,
                500,
                "urn:problem:verification:configuration-error",
                "Verification configuration error",
                "The verification service is not configured for this operation");
        assertThat(problem.getDetail()).doesNotContain("account", "login", "example.com");
        assertLogged(output, MissingIssueLimitRuleException.class);
    }

    @Test
    void mapsExpectedBusinessFailuresWithoutErrorLogging(CapturedOutput output) {
        VerificationExceptionHandler handler = createDefaultHandler();

        ResponseEntity<ProblemDetail> throttledResponse =
                handler.handleIssueLimitExceeded(exceeded(Duration.ofMillis(1201)));
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
        assertThat(throttled.getProperties()).isNullOrEmpty();
        assertProblem(
                invalid,
                400,
                "urn:problem:business:verification:invalid-code",
                "Invalid verification code",
                "The verification code is invalid");
        assertThat(output).doesNotContain("Verification operation failed");
    }

    @Test
    void logsThrottleViolationsAtDebugWithoutExposingThemInResponse(CapturedOutput output) {
        Logger logger = (Logger) LoggerFactory.getLogger(VerificationExceptionHandler.class);
        Level previousLevel = logger.getLevel();
        IssueLimitExceededException exception = new IssueLimitExceededException(List.of(
                new IssueLimitViolation("subject-minute", Duration.ofSeconds(30)),
                new IssueLimitViolation("ip-hour", Duration.ofMinutes(10))));

        ResponseEntity<ProblemDetail> response;
        try {
            logger.setLevel(Level.DEBUG);
            response = createDefaultHandler().handleIssueLimitExceeded(exception);
        } finally {
            logger.setLevel(previousLevel);
        }

        assertThat(output)
                .contains("DEBUG", "Verification code issuance throttled", "subject-minute", "ip-hour")
                .doesNotContain("user@example.com", "203.0.113.10");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().toString()).doesNotContain("subject-minute", "ip-hour");
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("600");
    }

    @ParameterizedTest
    @CsvSource({
        "0, 'Please retry shortly'",
        "1, 'Please retry after 1 second'",
        "89, 'Please retry after approximately 89 seconds'",
        "90, 'Please retry after approximately 2 minutes'",
        "3599, 'Please retry after approximately 60 minutes'",
        "3600, 'Please try again later'",
        "3601, 'Please try again later'",
        "129600, 'Please try again later'"
    })
    void formatsRetryAfterUsingAReadableUnit(long seconds, String expectedDetail) {
        ResponseEntity<ProblemDetail> response =
                createDefaultHandler().handleIssueLimitExceeded(exceeded(Duration.ofSeconds(seconds)));

        assertThat(response.getBody())
                .isNotNull()
                .extracting(ProblemDetail::getDetail)
                .isEqualTo(expectedDetail);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo(Long.toString(seconds));
        assertThat(response.getBody().getProperties()).isNullOrEmpty();
    }

    @Test
    void roundsRetryAfterUpWithoutOverflowing() {
        ResponseEntity<ProblemDetail> fractional =
                createDefaultHandler().handleIssueLimitExceeded(exceeded(Duration.ofNanos(1L)));
        ResponseEntity<ProblemDetail> maximum = createDefaultHandler()
                .handleIssueLimitExceeded(exceeded(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999L)));

        assertThat(fractional.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
        assertThat(maximum.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo(Long.toString(Long.MAX_VALUE));
    }

    private VerificationExceptionHandler createDefaultHandler() {
        return new VerificationExceptionHandler();
    }

    private IssueLimitExceededException exceeded(Duration retryAfter) {
        return new IssueLimitExceededException(List.of(new IssueLimitViolation("subject-minute", retryAfter)));
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
        String message = CodeSenderException.class.isAssignableFrom(exceptionType)
                ? "Verification code delivery failed"
                : "Verification operation failed";
        assertThat(output).contains("ERROR").contains(message).contains(exceptionType.getName());
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
