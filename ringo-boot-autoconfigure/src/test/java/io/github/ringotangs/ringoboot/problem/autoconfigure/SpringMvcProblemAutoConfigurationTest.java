package io.github.ringotangs.ringoboot.problem.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ringotangs.ringoboot.problem.web.SpringMvcExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

class SpringMvcProblemAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringMvcProblemAutoConfiguration.class));

    @Test
    void doesNotConfigureInNonWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SpringMvcProblemAutoConfiguration.class))
                .withPropertyValues("spring.mvc.problemdetails.enabled=true", "ringo.boot.problem.handlers.mvc=true")
                .run(context -> assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class));
    }

    @Test
    void configuresSpringMvcHandlingWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("spring.mvc.problemdetails.enabled=true", "ringo.boot.problem.handlers.mvc=true")
                .run(context -> assertThat(context).hasSingleBean(SpringMvcExceptionHandler.class));
    }

    @Test
    void leavesSpringBootProblemDetailsHandlerInPlaceByDefault() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
                .withPropertyValues("spring.mvc.problemdetails.enabled=true")
                .run(context -> {
                    assertThat(context).hasBean("problemDetailsExceptionHandler");
                    assertThat(context).doesNotHaveBean(SpringMvcExceptionHandler.class);
                });
    }

    @Test
    void replacesSpringBootProblemDetailsHandlerWhenMvcHandlingIsEnabled() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
                .withPropertyValues("spring.mvc.problemdetails.enabled=true", "ringo.boot.problem.handlers.mvc=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(SpringMvcExceptionHandler.class);
                    assertThat(context).doesNotHaveBean("problemDetailsExceptionHandler");
                });
    }

    @Test
    void doesNotConfigureWhenSpringMvcIsAbsent() {
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

    private static final class CustomSpringMvcExceptionHandler extends ResponseEntityExceptionHandler {}
}
