package io.github.ringotangs.springcommons.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemExceptionHandlerAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            ProblemExceptionHandlerAutoConfiguration.class
                    ));

    @Test
    void doesNotConfigureProblemHandlingByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
            assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
            assertThat(context).doesNotHaveBean(ProblemExceptionHandlerProperties.class);
        });
    }

    @Test
    void configuresDefaultMessagesWhenProblemHandlingIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.web.exception-handler.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
                    assertThat(context).hasSingleBean(ProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemMessageResolver.class))
                            .isInstanceOf(DefaultProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemExceptionHandlerProperties.class)
                            .isEnabled()).isTrue();
                    assertThat(context.getBean(ProblemExceptionHandlerProperties.class)
                            .isI18nEnabled()).isFalse();
                });
    }

    @Test
    void usesLocalizedMessagesWhenBothFeaturesAreEnabled() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.web.exception-handler.enabled=true",
                        "ringotangs.spring-commons.web.exception-handler.i18n-enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
                    assertThat(context.getBean(ProblemMessageResolver.class))
                            .isInstanceOf(MessageSourceProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemExceptionHandlerProperties.class)
                            .isI18nEnabled()).isTrue();
                });
    }

    @Test
    void internationalizationDoesNotEnableProblemHandling() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.web.exception-handler.i18n-enabled=true"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(ProblemMessageResolver.class);
                    assertThat(context).doesNotHaveBean(
                            ProblemExceptionHandlerProperties.class
                    );
                });
    }

    @Test
    void doesNotConfigureProblemHandlingWhenExplicitlyDisabled() {
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
                        "ringotangs.spring-commons.web.exception-handler.enabled=true"
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
