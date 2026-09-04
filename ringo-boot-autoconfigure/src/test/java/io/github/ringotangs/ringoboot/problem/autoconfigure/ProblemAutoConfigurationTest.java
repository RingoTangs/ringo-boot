package io.github.ringotangs.ringoboot.problem.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ringotangs.ringoboot.problem.web.FallbackExceptionHandler;
import io.github.ringotangs.ringoboot.problem.web.ProblemExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class ProblemAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(ProblemAutoConfiguration.class));

    private final ApplicationContextRunner nonWebContextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ProblemAutoConfiguration.class));

    @Test
    void doesNotConfigureHandlersInNonWebApplication() {
        nonWebContextRunner
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true", "ringo.boot.problem.handlers.fallback=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                });
    }

    @Test
    void doesNotConfigureHandlersWhenSpringWebIsAbsent() {
        nonWebContextRunner
                .withClassLoader(new FilteredClassLoader("org.springframework.web"))
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true", "ringo.boot.problem.handlers.fallback=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                });
    }

    @Test
    void configuresProblemHandlingWhenVerificationModuleIsAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("io.github.ringotangs.ringoboot.verification"))
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true",
                        "ringo.boot.problem.handlers.application=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean("verificationExceptionHandler");
                });
    }

    @Test
    void doesNotConfigureExceptionHandlingByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
            assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
        });
    }

    @Test
    void standardSwitchLeavesCustomHandlersDisabledByDefault() {
        contextRunner
                .withPropertyValues("spring.mvc.problemdetails.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                });
    }

    @Test
    void configuresProblemHandlingWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true", "ringo.boot.problem.handlers.application=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                });
    }

    @Test
    void configuresFallbackHandlingIndependently() {
        contextRunner
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true", "ringo.boot.problem.handlers.fallback=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).hasSingleBean(FallbackExceptionHandler.class);
                });
    }

    @Test
    void fallbackDoesNotEnableExceptionHandling() {
        contextRunner
                .withPropertyValues("ringo.boot.problem.handlers.fallback=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                });
    }

    @Test
    void problemHandlingDoesNotEnableExceptionHandling() {
        contextRunner
                .withPropertyValues("ringo.boot.problem.handlers.application=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                });
    }

    @Test
    void doesNotConfigureExceptionHandlingWhenExplicitlyDisabled() {
        contextRunner
                .withPropertyValues("spring.mvc.problemdetails.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                });
    }
}
