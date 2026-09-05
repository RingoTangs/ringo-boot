package io.github.ringotangs.ringoboot.verification.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ringotangs.ringoboot.problem.ProblemDescriptor;
import io.github.ringotangs.ringoboot.verification.VerificationRejectedException;
import io.github.ringotangs.ringoboot.verification.VerifyResult;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitExceededException;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitViolation;
import io.github.ringotangs.ringoboot.verification.limit.MissingIssueLimitRuleException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import io.github.ringotangs.ringoboot.verification.web.VerificationExceptionHandler;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.ProblemDetail;

class VerificationProblemAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VerificationProblemAutoConfiguration.class));

    private final ApplicationContextRunner nonWebContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VerificationProblemAutoConfiguration.class));

    @Test
    void configuresVerificationHandlingIndependently() {
        contextRunner
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true",
                        "ringo.boot.verification.exception-handler=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(VerificationExceptionHandler.class);
                });
    }

    @Test
    void doesNotConfigureInNonWebApplication() {
        nonWebContextRunner
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true",
                        "ringo.boot.verification.exception-handler=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(VerificationExceptionHandler.class));
    }

    @Test
    void doesNotConfigureWhenVerificationClassesAreAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("io.github.ringotangs.ringoboot.verification"))
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true",
                        "ringo.boot.verification.exception-handler=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("verificationExceptionHandler");
                });
    }

    @Test
    void doesNotConfigureWhenProblemClassesAreAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(ProblemDescriptor.class))
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true",
                        "ringo.boot.verification.exception-handler=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("verificationExceptionHandler");
                });
    }

    @Test
    void requiresProblemDetailsVerificationAndHandlerSwitches() {
        contextRunner
                .withPropertyValues("spring.mvc.problemdetails.enabled=true", "ringo.boot.verification.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(VerificationExceptionHandler.class));

        contextRunner
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true", "ringo.boot.verification.exception-handler=true")
                .run(context -> assertThat(context).doesNotHaveBean(VerificationExceptionHandler.class));

        contextRunner
                .withPropertyValues(
                        "ringo.boot.verification.enabled=true", "ringo.boot.verification.exception-handler=true")
                .run(context -> assertThat(context).doesNotHaveBean(VerificationExceptionHandler.class));
    }

    @Test
    void backsOffForCustomVerificationExceptionHandler() {
        VerificationExceptionHandler customHandler = new VerificationExceptionHandler();

        contextRunner
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true",
                        "ringo.boot.verification.exception-handler=true",
                        "ringo.boot.verification.enabled=true")
                .withBean(VerificationExceptionHandler.class, () -> customHandler)
                .run(context -> assertThat(context.getBean(VerificationExceptionHandler.class))
                        .isSameAs(customHandler));
    }

    @Test
    void usesDefaultVerificationMessages() {
        contextRunner
                .withPropertyValues(
                        "spring.mvc.problemdetails.enabled=true",
                        "ringo.boot.verification.exception-handler=true",
                        "ringo.boot.verification.enabled=true")
                .run(context -> {
                    VerificationExceptionHandler handler = context.getBean(VerificationExceptionHandler.class);

                    assertThat(handler.handleIssueLimitExceeded(new IssueLimitExceededException(
                                            List.of(new IssueLimitViolation("subject-minute", Duration.ofSeconds(2)))))
                                    .getBody())
                            .extracting(ProblemDetail::getTitle, ProblemDetail::getDetail)
                            .containsExactly(
                                    "Too many verification code requests",
                                    "Please retry after approximately 2 seconds");
                    assertThat(handler.handleVerificationRejected(
                                    new VerificationRejectedException(VerifyResult.MISMATCH)))
                            .extracting(ProblemDetail::getTitle, ProblemDetail::getDetail)
                            .containsExactly("Invalid verification code", "The verification code is invalid");
                    assertThat(handler.handleVerificationException(new CodeGenerationException("internal")))
                            .extracting(ProblemDetail::getTitle, ProblemDetail::getDetail)
                            .containsExactly(
                                    "Verification code generation failed",
                                    "The verification service encountered an internal error");
                    assertThat(handler.handleVerificationException(new MissingIssueLimitRuleException()))
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
