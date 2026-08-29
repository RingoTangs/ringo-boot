package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ringotangs.ringoboot.autoconfigure.problem.ProblemAutoConfiguration;
import io.github.ringotangs.ringoboot.autoconfigure.problem.ProblemMessageResolver;
import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.VerificationThrottledException;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitViolation;
import io.github.ringotangs.ringoboot.verification.limit.MissingIssueRateLimitRuleException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class VerificationProblemAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(ProblemAutoConfiguration.class, VerificationProblemAutoConfiguration.class));

    private final ApplicationContextRunner nonWebContextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(ProblemAutoConfiguration.class, VerificationProblemAutoConfiguration.class));

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void configuresVerificationHandlingIndependently() {
        contextRunner
                .withPropertyValues(
                        "ringo.boot.problem.enabled=true",
                        "ringo.boot.problem.handlers.verification=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(VerificationExceptionHandler.class);
                    assertThat(context).hasSingleBean(ProblemMessageResolver.class);
                });
    }

    @Test
    void doesNotConfigureInNonWebApplication() {
        nonWebContextRunner
                .withPropertyValues(
                        "ringo.boot.problem.enabled=true",
                        "ringo.boot.problem.handlers.verification=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(VerificationExceptionHandler.class));
    }

    @Test
    void doesNotConfigureWhenVerificationClassesAreAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("io.github.ringotangs.ringoboot.verification"))
                .withPropertyValues(
                        "ringo.boot.problem.enabled=true",
                        "ringo.boot.problem.handlers.verification=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("verificationExceptionHandler");
                });
    }

    @Test
    void requiresBothFeatureSwitchesAndHandlerSwitch() {
        contextRunner
                .withPropertyValues("ringo.boot.problem.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(VerificationExceptionHandler.class));

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(VerificationExceptionHandler.class));

        contextRunner
                .withPropertyValues("ringo.boot.problem.enabled=true", "ringo.boot.verification.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(VerificationExceptionHandler.class));
    }

    @Test
    void backsOffForCustomVerificationExceptionHandler() {
        ProblemMessageResolver resolver =
                exception -> new ProblemMessageResolver.ProblemMessages("Custom title", "Custom detail");
        VerificationExceptionHandler customHandler = new VerificationExceptionHandler(resolver);

        contextRunner
                .withPropertyValues(
                        "ringo.boot.problem.enabled=true",
                        "ringo.boot.problem.handlers.verification=true",
                        "ringo.boot.verification.enabled=true")
                .withBean(VerificationExceptionHandler.class, () -> customHandler)
                .run(context -> assertThat(context.getBean(VerificationExceptionHandler.class))
                        .isSameAs(customHandler));
    }

    @Test
    void usesBuiltInLocalizedVerificationMessages() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);

        contextRunner
                .withPropertyValues(
                        "ringo.boot.problem.enabled=true",
                        "ringo.boot.problem.i18n=true",
                        "ringo.boot.problem.handlers.verification=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> {
                    VerificationExceptionHandler handler = context.getBean(VerificationExceptionHandler.class);
                    ProblemDetail problem =
                            handler.handleVerificationException(new VerificationStoreException("internal"));
                    assertThat(problem.getTitle()).isEqualTo("验证码服务不可用");
                    assertThat(problem.getDetail()).isEqualTo("验证码服务暂时不可用");

                    ResponseEntity<ProblemDetail> throttled =
                            handler.handleVerificationThrottled(new VerificationThrottledException(
                                    List.of(new IssueLimitViolation("subject-minute", Duration.ofSeconds(3_478L)))));
                    assertThat(throttled.getBody())
                            .isNotNull()
                            .extracting(ProblemDetail::getDetail)
                            .isEqualTo("请在约 58 分钟后重试");
                });
    }

    @Test
    void usesCodeDefaultsForEnglishVerificationMessages() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        contextRunner
                .withPropertyValues(
                        "ringo.boot.problem.enabled=true",
                        "ringo.boot.problem.i18n=true",
                        "ringo.boot.problem.handlers.verification=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> {
                    VerificationExceptionHandler handler = context.getBean(VerificationExceptionHandler.class);

                    assertThat(handler.handleVerificationThrottled(new VerificationThrottledException(
                                            List.of(new IssueLimitViolation("subject-minute", Duration.ofSeconds(2)))))
                                    .getBody())
                            .extracting(ProblemDetail::getTitle, ProblemDetail::getDetail)
                            .containsExactly(
                                    "Too many verification code requests",
                                    "Please retry after approximately 2 seconds");
                    assertThat(handler.handleInvalidVerificationCode(new InvalidVerificationCodeException()))
                            .extracting(ProblemDetail::getTitle, ProblemDetail::getDetail)
                            .containsExactly("Invalid verification code", "The verification code is invalid");
                    assertThat(handler.handleVerificationException(new CodeGenerationException("internal")))
                            .extracting(ProblemDetail::getTitle, ProblemDetail::getDetail)
                            .containsExactly(
                                    "Verification code generation failed",
                                    "The verification service encountered an internal error");
                    assertThat(handler.handleVerificationException(new MissingIssueRateLimitRuleException()))
                            .extracting(ProblemDetail::getTitle, ProblemDetail::getDetail)
                            .containsExactly(
                                    "Verification configuration error",
                                    "The verification service is not configured for this operation");
                    assertThat(handler.handleVerificationException(new VerificationStoreException("internal")))
                            .extracting(ProblemDetail::getTitle, ProblemDetail::getDetail)
                            .containsExactly(
                                    "Verification service unavailable",
                                    "The verification service is temporarily unavailable");
                });
    }
}
