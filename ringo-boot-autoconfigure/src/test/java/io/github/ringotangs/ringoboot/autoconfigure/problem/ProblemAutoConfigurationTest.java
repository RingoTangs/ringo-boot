package io.github.ringotangs.ringoboot.autoconfigure.problem;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

class ProblemAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(ProblemAutoConfiguration.class));

    private final ApplicationContextRunner nonWebContextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ProblemAutoConfiguration.class));

    @Test
    void doesNotConfigureHandlersInNonWebApplication() {
        nonWebContextRunner
                .withPropertyValues(
                        "ringo.boot.problem.enabled=true",
                        "ringo.boot.problem.application-enabled=true",
                        "ringo.boot.problem.fallback-enabled=true",
                        "ringo.boot.problem.i18n-enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
                });
    }

    @Test
    void doesNotConfigureHandlersWhenSpringWebIsAbsent() {
        nonWebContextRunner
                .withClassLoader(new FilteredClassLoader("org.springframework.web"))
                .withPropertyValues(
                        "ringo.boot.problem.enabled=true",
                        "ringo.boot.problem.application-enabled=true",
                        "ringo.boot.problem.mvc-enabled=true",
                        "ringo.boot.problem.fallback-enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class);
                });
    }

    @Test
    void configuresProblemHandlingWhenVerificationModuleIsAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("io.github.ringotangs.ringoboot.verification"))
                .withPropertyValues(
                        "ringo.boot.problem.enabled=true",
                        "ringo.boot.problem.application-enabled=true",
                        "ringo.boot.problem.verification-enabled=true",
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
            assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
            assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
            assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class);
            assertThat(context).doesNotHaveBean(ProblemProperties.class);
        });
    }

    @Test
    void configuresSpringMvcHandlingIndependently() {
        contextRunner
                .withPropertyValues("ringo.boot.problem.enabled=true", "ringo.boot.problem.mvc-enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(SpringMvcExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemProperties.class).isMvcEnabled())
                            .isTrue();
                });
    }

    @Test
    void doesNotConfigureSpringMvcHandlerWhenSpringMvcIsAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(ResponseEntityExceptionHandler.class))
                .withPropertyValues("ringo.boot.problem.enabled=true", "ringo.boot.problem.mvc-enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class));
    }

    @Test
    void backsOffForCustomSpringMvcExceptionHandler() {
        contextRunner
                .withPropertyValues("ringo.boot.problem.enabled=true", "ringo.boot.problem.mvc-enabled=true")
                .withBean(ResponseEntityExceptionHandler.class, CustomSpringMvcExceptionHandler::new)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class);
                    assertThat(context).hasSingleBean(ResponseEntityExceptionHandler.class);
                });
    }

    @Test
    void masterSwitchAloneDoesNotConfigureHandlers() {
        contextRunner.withPropertyValues("ringo.boot.problem.enabled=true").run(context -> {
            assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
            assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
            assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
            assertThat(context.getBean(ProblemProperties.class).isEnabled()).isTrue();
            assertThat(context.getBean(ProblemProperties.class).isApplicationEnabled())
                    .isFalse();
            assertThat(context.getBean(ProblemProperties.class).isMvcEnabled()).isFalse();
            assertThat(context.getBean(ProblemProperties.class).isVerificationEnabled())
                    .isFalse();
            assertThat(context.getBean(ProblemProperties.class).isI18nEnabled()).isFalse();
        });
    }

    @Test
    void configuresProblemHandlingWithDefaultMessagesWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("ringo.boot.problem.enabled=true", "ringo.boot.problem.application-enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
                    assertThat(context).hasSingleBean(ProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemMessageResolver.class))
                            .isInstanceOf(DefaultProblemMessageResolver.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                    assertThat(context.getBean(ProblemProperties.class).isApplicationEnabled())
                            .isTrue();
                });
    }

    @Test
    void configuresFallbackHandlingIndependently() {
        contextRunner
                .withPropertyValues("ringo.boot.problem.enabled=true", "ringo.boot.problem.fallback-enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).hasSingleBean(FallbackExceptionHandler.class);
                    assertThat(context).hasSingleBean(ProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemProperties.class).isFallbackEnabled())
                            .isTrue();
                });
    }

    @Test
    void fallbackDoesNotEnableExceptionHandling() {
        contextRunner
                .withPropertyValues("ringo.boot.problem.fallback-enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                });
    }

    @Test
    void problemHandlingDoesNotEnableExceptionHandling() {
        contextRunner
                .withPropertyValues("ringo.boot.problem.application-enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
                });
    }

    @Test
    void configuresBothHandlersWithLocalizedMessages() {
        contextRunner
                .withPropertyValues(
                        "ringo.boot.problem.enabled=true",
                        "ringo.boot.problem.application-enabled=true",
                        "ringo.boot.problem.fallback-enabled=true",
                        "ringo.boot.problem.i18n-enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
                    assertThat(context).hasSingleBean(FallbackExceptionHandler.class);
                    assertThat(context.getBean(ProblemMessageResolver.class))
                            .isInstanceOf(MessageSourceProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemProperties.class).isI18nEnabled())
                            .isTrue();
                });
    }

    @Test
    void internationalizationDoesNotEnableExceptionHandling() {
        contextRunner.withPropertyValues("ringo.boot.problem.i18n-enabled=true").run(context -> {
            assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
            assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
            assertThat(context).doesNotHaveBean(ProblemProperties.class);
        });
    }

    @Test
    void doesNotConfigureExceptionHandlingWhenExplicitlyDisabled() {
        contextRunner.withPropertyValues("ringo.boot.problem.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
            assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
        });
    }

    @Test
    void backsOffForCustomMessageResolver() {
        ProblemMessageResolver customResolver =
                exception -> new ProblemMessageResolver.ProblemMessages("Custom title", "Custom detail");

        contextRunner
                .withPropertyValues("ringo.boot.problem.enabled=true", "ringo.boot.problem.application-enabled=true")
                .withBean(ProblemMessageResolver.class, () -> customResolver)
                .run(context -> {
                    assertThat(context).hasSingleBean(ProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemMessageResolver.class)).isSameAs(customResolver);
                    assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
                });
    }

    private static final class CustomSpringMvcExceptionHandler extends ResponseEntityExceptionHandler {}
}
