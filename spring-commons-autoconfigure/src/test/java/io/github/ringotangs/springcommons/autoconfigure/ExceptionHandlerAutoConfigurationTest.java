package io.github.ringotangs.springcommons.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionHandlerAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            ExceptionHandlerAutoConfiguration.class
                    ));

    @Test
    void doesNotConfigureExceptionHandlingByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
            assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
            assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
            assertThat(context).doesNotHaveBean(ExceptionHandlerProperties.class);
        });
    }

    @Test
    void masterSwitchAloneDoesNotConfigureHandlers() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.web.exception-handler.enabled=true"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                    assertThat(context.getBean(ExceptionHandlerProperties.class)
                            .isEnabled()).isTrue();
                    assertThat(context.getBean(ExceptionHandlerProperties.class)
                            .isProblemEnabled()).isFalse();
                    assertThat(context.getBean(ExceptionHandlerProperties.class)
                            .isI18nEnabled()).isFalse();
                });
    }

    @Test
    void configuresProblemHandlingWithDefaultMessagesWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.web.exception-handler.enabled=true",
                        "ringotangs.spring-commons.web.exception-handler.problem-enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
                    assertThat(context).hasSingleBean(ProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemMessageResolver.class))
                            .isInstanceOf(DefaultProblemMessageResolver.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                    assertThat(context.getBean(ExceptionHandlerProperties.class)
                            .isProblemEnabled()).isTrue();
                });
    }

    @Test
    void configuresFallbackHandlingIndependently() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.web.exception-handler.enabled=true",
                        "ringotangs.spring-commons.web.exception-handler.fallback-enabled=true"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).hasSingleBean(FallbackExceptionHandler.class);
                    assertThat(context).hasSingleBean(ProblemMessageResolver.class);
                    assertThat(context.getBean(ExceptionHandlerProperties.class)
                            .isFallbackEnabled()).isTrue();
                });
    }

    @Test
    void fallbackDoesNotEnableExceptionHandling() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.web.exception-handler.fallback-enabled=true"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                });
    }

    @Test
    void problemHandlingDoesNotEnableExceptionHandling() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.web.exception-handler.problem-enabled=true"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
                });
    }

    @Test
    void configuresBothHandlersWithLocalizedMessages() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.web.exception-handler.enabled=true",
                        "ringotangs.spring-commons.web.exception-handler.problem-enabled=true",
                        "ringotangs.spring-commons.web.exception-handler.fallback-enabled=true",
                        "ringotangs.spring-commons.web.exception-handler.i18n-enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
                    assertThat(context).hasSingleBean(FallbackExceptionHandler.class);
                    assertThat(context.getBean(ProblemMessageResolver.class))
                            .isInstanceOf(MessageSourceProblemMessageResolver.class);
                    assertThat(context.getBean(ExceptionHandlerProperties.class)
                            .isI18nEnabled()).isTrue();
                });
    }

    @Test
    void internationalizationDoesNotEnableExceptionHandling() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.web.exception-handler.i18n-enabled=true"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
                    assertThat(context).doesNotHaveBean(
                            ExceptionHandlerProperties.class
                    );
                });
    }

    @Test
    void doesNotConfigureExceptionHandlingWhenExplicitlyDisabled() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.web.exception-handler.enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
                });
    }

    @Test
    void backsOffForCustomMessageResolver() {
        ProblemMessageResolver customResolver = exception ->
                new ProblemMessageResolver.ProblemMessages("Custom title", "Custom detail");

        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.web.exception-handler.enabled=true",
                        "ringotangs.spring-commons.web.exception-handler.problem-enabled=true"
                )
                .withBean(ProblemMessageResolver.class, () -> customResolver)
                .run(context -> {
                    assertThat(context).hasSingleBean(ProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemMessageResolver.class))
                            .isSameAs(customResolver);
                    assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
                });
    }
}
