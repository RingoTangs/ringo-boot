package io.github.ringotangs.springcommons.web.autoconfigure;

import io.github.ringotangs.springcommons.web.ProblemExceptionHandler;
import io.github.ringotangs.springcommons.web.ProblemMessageResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            ProblemDetailsAutoConfiguration.class
                    ));

    @Test
    void configuresLocalizedProblemHandlingByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
            assertThat(context).hasSingleBean(ProblemMessageResolver.class);
            assertThat(context.getBean(ProblemMessageResolver.class))
                    .isInstanceOf(MessageSourceProblemMessageResolver.class);
            assertThat(context.getBean(ProblemDetailsProperties.class).isEnabled()).isTrue();
            assertThat(context.getBean(ProblemDetailsProperties.class)
                    .getI18n().isEnabled()).isTrue();
        });
    }

    @Test
    void usesDefaultMessagesWhenInternationalizationIsDisabled() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.problem-details.i18n.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
                    assertThat(context.getBean(ProblemMessageResolver.class))
                            .isInstanceOf(DefaultProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemDetailsProperties.class)
                            .getI18n().isEnabled()).isFalse();
                });
    }

    @Test
    void disablesProblemHandlingWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "ringotangs.spring-commons.problem-details.enabled=false"
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
                .withBean(ProblemMessageResolver.class, () -> customResolver)
                .run(context -> {
                    assertThat(context).hasSingleBean(ProblemMessageResolver.class);
                    assertThat(context.getBean(ProblemMessageResolver.class))
                            .isSameAs(customResolver);
                    assertThat(context).hasSingleBean(ProblemExceptionHandler.class);
                });
    }
}
