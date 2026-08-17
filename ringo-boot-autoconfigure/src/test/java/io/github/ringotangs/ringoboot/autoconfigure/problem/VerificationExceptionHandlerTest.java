package io.github.ringotangs.ringoboot.autoconfigure.problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.VerificationThrottledException;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.sender.CodeDeliveryRejectedException;
import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.ProblemDetail;

@ExtendWith(OutputCaptureExtension.class)
class VerificationExceptionHandlerTest {

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

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
    void returnsSameSafeUnavailableProblemForSenderAndStoreFailures(CapturedOutput output) {
        VerificationExceptionHandler handler = createDefaultHandler();

        ProblemDetail senderProblem = handler.handleVerificationException(
                new CodeSenderException("provider token=secret", new IllegalStateException("provider details")));
        ProblemDetail storeProblem = handler.handleVerificationException(
                new VerificationStoreException("redis password=secret", new IllegalStateException("redis details")));
        ProblemDetail rejectedProblem = handler.handleVerificationException(new CodeDeliveryRejectedException());

        assertServiceUnavailable(senderProblem);
        assertServiceUnavailable(storeProblem);
        assertServiceUnavailable(rejectedProblem);
        assertThat(senderProblem.getDetail()).doesNotContain("provider", "secret");
        assertThat(storeProblem.getDetail()).doesNotContain("redis", "secret");
        assertLogged(output, CodeSenderException.class);
        assertLogged(output, VerificationStoreException.class);
        assertLogged(output, CodeDeliveryRejectedException.class);
    }

    @Test
    void usesBuiltInLocalizedMessages() {
        StaticMessageSource messageSource = new StaticMessageSource();
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        VerificationExceptionHandler handler =
                new VerificationExceptionHandler(new MessageSourceProblemMessageResolver(messageSource));

        ProblemDetail generation = handler.handleVerificationException(new CodeGenerationException("internal"));
        ProblemDetail unavailable = handler.handleVerificationException(new VerificationStoreException("internal"));

        assertEquals("验证码生成失败", generation.getTitle());
        assertEquals("验证码服务发生内部错误", generation.getDetail());
        assertEquals("验证码服务不可用", unavailable.getTitle());
        assertEquals("验证码服务暂时不可用", unavailable.getDetail());
    }

    @Test
    void usesBuiltInEnglishMessages() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        VerificationExceptionHandler handler =
                new VerificationExceptionHandler(new MessageSourceProblemMessageResolver(new StaticMessageSource()));

        ProblemDetail problem = handler.handleVerificationException(new VerificationStoreException("internal"));

        assertEquals("Verification service unavailable", problem.getTitle());
        assertEquals("The verification service is temporarily unavailable", problem.getDetail());
    }

    @Test
    void applicationMessagesOverrideBuiltInMessagesPerKey() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage(
                "problem.verification.generation-failed.title", Locale.SIMPLIFIED_CHINESE, "自定义验证码生成失败");
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        VerificationExceptionHandler handler =
                new VerificationExceptionHandler(new MessageSourceProblemMessageResolver(messageSource));

        ProblemDetail problem = handler.handleVerificationException(new CodeGenerationException("internal"));

        assertEquals("自定义验证码生成失败", problem.getTitle());
        assertEquals("验证码服务发生内部错误", problem.getDetail());
    }

    @Test
    void mapsExpectedBusinessFailuresWithoutErrorLogging(CapturedOutput output) {
        VerificationExceptionHandler handler = createDefaultHandler();

        ProblemDetail throttled =
                handler.handleVerificationThrottled(new VerificationThrottledException(Duration.ofMillis(1201)));
        ProblemDetail invalid = handler.handleInvalidVerificationCode(new InvalidVerificationCodeException());

        assertProblem(
                throttled,
                429,
                "urn:problem:business:verification:throttled",
                "Too many verification code requests",
                "Please retry after 2 seconds");
        assertProblem(
                invalid,
                400,
                "urn:problem:business:verification:invalid-code",
                "Invalid verification code",
                "The verification code is invalid");
        assertThat(output).doesNotContain("Verification operation failed");
    }

    @Test
    void localizesExpectedBusinessFailures() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        VerificationExceptionHandler handler =
                new VerificationExceptionHandler(new MessageSourceProblemMessageResolver(new StaticMessageSource()));

        ProblemDetail throttled =
                handler.handleVerificationThrottled(new VerificationThrottledException(Duration.ofSeconds(3)));
        ProblemDetail invalid = handler.handleInvalidVerificationCode(new InvalidVerificationCodeException());

        assertEquals("验证码请求过于频繁", throttled.getTitle());
        assertEquals("请在 3 秒后重试", throttled.getDetail());
        assertEquals("验证码无效", invalid.getTitle());
        assertEquals("验证码无效", invalid.getDetail());
    }

    private VerificationExceptionHandler createDefaultHandler() {
        return new VerificationExceptionHandler(new DefaultProblemMessageResolver());
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
}
