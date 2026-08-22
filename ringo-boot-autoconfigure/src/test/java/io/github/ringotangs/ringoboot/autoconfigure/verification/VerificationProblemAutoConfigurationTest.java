package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ringotangs.ringoboot.autoconfigure.problem.ProblemAutoConfiguration;
import io.github.ringotangs.ringoboot.autoconfigure.problem.ProblemMessageResolver;
import io.github.ringotangs.ringoboot.autoconfigure.problem.ProblemProperties;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ProblemDetail;

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
                        "ringo.boot.problem.verification-enabled=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(VerificationExceptionHandler.class);
                    assertThat(context).hasSingleBean(ProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemProperties.class).isVerificationEnabled())
                            .isTrue();
                });
    }

    @Test
    void doesNotConfigureInNonWebApplication() {
        nonWebContextRunner
                .withPropertyValues(
                        "ringo.boot.problem.enabled=true",
                        "ringo.boot.problem.verification-enabled=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(VerificationExceptionHandler.class));
    }

    @Test
    void doesNotConfigureWhenVerificationClassesAreAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("io.github.ringotangs.ringoboot.verification"))
                .withPropertyValues(
                        "ringo.boot.problem.enabled=true",
                        "ringo.boot.problem.verification-enabled=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("verificationExceptionHandler");
                });
    }

    @Test
    void requiresBothFeatureSwitches() {
        contextRunner
                .withPropertyValues("ringo.boot.problem.enabled=true", "ringo.boot.verification.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(VerificationExceptionHandler.class));

        contextRunner
                .withPropertyValues("ringo.boot.problem.enabled=true", "ringo.boot.problem.verification-enabled=true")
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
                        "ringo.boot.problem.verification-enabled=true",
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
                        "ringo.boot.problem.verification-enabled=true",
                        "ringo.boot.problem.i18n-enabled=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> {
                    VerificationExceptionHandler handler = context.getBean(VerificationExceptionHandler.class);
                    ProblemDetail problem =
                            handler.handleVerificationException(new VerificationStoreException("internal"));
                    assertThat(problem.getTitle()).isEqualTo("验证码服务不可用");
                    assertThat(problem.getDetail()).isEqualTo("验证码服务暂时不可用");
                });
    }
}
