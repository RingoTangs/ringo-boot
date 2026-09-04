package io.github.ringotangs.ringoboot.problem.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.ringotangs.ringoboot.problem.ProblemDescriptor;
import java.net.URI;
import org.junit.jupiter.api.Test;

class ProblemDetailsTest {

    private static final ProblemDescriptor DESCRIPTOR = ProblemDescriptor.of(
            URI.create("https://example.com/problems/invalid-value"),
            "problem.invalid-value",
            "Invalid value",
            "Value {0} is invalid",
            400);

    @Test
    void createsProblemWithDefaultDetail() {
        var problem = ProblemDetails.create(DESCRIPTOR);

        assertThat(problem.getType()).isEqualTo(DESCRIPTOR.type());
        assertThat(problem.getTitle()).isEqualTo(DESCRIPTOR.title());
        assertThat(problem.getStatus()).isEqualTo(DESCRIPTOR.status());
        assertThat(problem.getDetail()).isEqualTo(DESCRIPTOR.detail());
    }

    @Test
    void acceptsNullFormatterAsDefault() {
        assertThat(ProblemDetails.create(DESCRIPTOR, null).getDetail()).isEqualTo(DESCRIPTOR.detail());
    }

    @Test
    void formatsDescriptorDetail() {
        assertThat(ProblemDetails.create(DESCRIPTOR, detail -> detail.replace("{0}", "email"))
                        .getDetail())
                .isEqualTo("Value email is invalid");
    }

    @Test
    void rejectsNullFormattedDetail() {
        assertThatNullPointerException()
                .isThrownBy(() -> ProblemDetails.create(DESCRIPTOR, ignored -> null))
                .withMessage("formatted detail must not be null");
    }
}
