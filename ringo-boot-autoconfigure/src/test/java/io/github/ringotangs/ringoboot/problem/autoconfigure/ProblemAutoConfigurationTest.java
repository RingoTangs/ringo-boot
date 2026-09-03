package io.github.ringotangs.ringoboot.problem.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ringotangs.ringoboot.problem.web.FallbackExceptionHandler;
import io.github.ringotangs.ringoboot.problem.web.ProblemExceptionHandler;
import io.github.ringotangs.ringoboot.problem.web.SpringMvcExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

class ProblemAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(ProblemAutoConfiguration.class));

    private final ApplicationContextRunner nonWebContextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ProblemAutoConfiguration.class));

    private final WebApplicationContextRunner springMvcContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ProblemAutoConfiguration.class, WebMvcAutoConfiguration.class));

    @Test
    void doesNotConfigureHandlersInNonWebApplication() {
        nonWebContextRunner
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true", "ringo.boot.problem.handlers.fallback=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class);
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
                    assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class);
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
            assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class);
            assertThat(context).doesNotHaveBean(ProblemProperties.class);
        });
    }

    @Test
    void configuresSpringMvcHandlingIndependently() {
        contextRunner
                .withPropertyValues("spring.mvc.problemdetails.enabled=true", "ringo.boot.problem.handlers.mvc=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(SpringMvcExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                });
    }

    @Test
    void leavesSpringBootProblemDetailsHandlerInPlaceByDefault() {
        springMvcContextRunner
                .withPropertyValues("spring.mvc.problemdetails.enabled=true")
                .run(context -> {
                    assertThat(context).hasBean("problemDetailsExceptionHandler");
                    assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class);
                });
    }

    @Test
    void replacesSpringBootProblemDetailsHandlerWhenMvcHandlingIsEnabled() {
        springMvcContextRunner
                .withPropertyValues("spring.mvc.problemdetails.enabled=true", "ringo.boot.problem.handlers.mvc=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(SpringMvcExceptionHandler.class);
                    assertThat(context).doesNotHaveBean("problemDetailsExceptionHandler");
                });
    }

    @Test
    void doesNotConfigureSpringMvcHandlerWhenSpringMvcIsAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(ResponseEntityExceptionHandler.class))
                .withPropertyValues("spring.mvc.problemdetails.enabled=true", "ringo.boot.problem.handlers.mvc=true")
                .run(context -> assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class));
    }

    @Test
    void backsOffForCustomSpringMvcExceptionHandler() {
        contextRunner
                .withPropertyValues("spring.mvc.problemdetails.enabled=true", "ringo.boot.problem.handlers.mvc=true")
                .withBean(ResponseEntityExceptionHandler.class, CustomSpringMvcExceptionHandler::new)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class);
                    assertThat(context).hasSingleBean(ResponseEntityExceptionHandler.class);
                });
    }

    @Test
    void standardSwitchLeavesCustomHandlersDisabledByDefault() {
        contextRunner
                .withPropertyValues("spring.mvc.problemdetails.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(FallbackExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(ProblemProperties.class);
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
                    assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class);
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

    private static final class CustomSpringMvcExceptionHandler extends ResponseEntityExceptionHandler {}
}
