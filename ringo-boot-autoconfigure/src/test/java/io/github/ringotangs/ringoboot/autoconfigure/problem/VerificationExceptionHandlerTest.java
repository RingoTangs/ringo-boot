package io.github.ringotangs.ringoboot.autoconfigure.problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import java.net.URI;
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

        assertServiceUnavailable(senderProblem);
        assertServiceUnavailable(storeProblem);
        assertThat(senderProblem.getDetail()).doesNotContain("provider", "secret");
        assertThat(storeProblem.getDetail()).doesNotContain("redis", "secret");
        assertLogged(output, CodeSenderException.class);
        assertLogged(output, VerificationStoreException.class);
    }

    @Test
    void localizesGenerationAndUnavailableProblems() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("problem.verification.generation-failed.title", Locale.SIMPLIFIED_CHINESE, "验证码生成失败");
        messageSource.addMessage(
                "problem.verification.generation-failed.detail", Locale.SIMPLIFIED_CHINESE, "验证码服务发生内部错误");
        messageSource.addMessage(
                "problem.verification.service-unavailable.title", Locale.SIMPLIFIED_CHINESE, "验证码服务不可用");
        messageSource.addMessage(
                "problem.verification.service-unavailable.detail", Locale.SIMPLIFIED_CHINESE, "验证码服务暂时不可用");
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
