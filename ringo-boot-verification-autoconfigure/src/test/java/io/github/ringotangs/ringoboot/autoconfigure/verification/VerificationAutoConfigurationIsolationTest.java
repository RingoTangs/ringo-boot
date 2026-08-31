package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

class VerificationAutoConfigurationIsolationTest {

    @Test
    void doesNotDependOnProblemArtifacts() {
        ClassLoader classLoader = getClass().getClassLoader();

        assertThat(ClassUtils.isPresent("io.github.ringotangs.ringoboot.problem.ProblemException", classLoader))
                .isFalse();
        assertThat(ClassUtils.isPresent(
                        "io.github.ringotangs.ringoboot.autoconfigure.problem.ProblemAutoConfiguration", classLoader))
                .isFalse();
    }
}
