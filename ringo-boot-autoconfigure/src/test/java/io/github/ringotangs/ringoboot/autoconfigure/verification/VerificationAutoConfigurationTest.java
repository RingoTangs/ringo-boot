package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.ringotangs.ringoboot.verification.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.StoreResult;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerificationResult;
import io.github.ringotangs.ringoboot.verification.VerificationService;
import io.github.ringotangs.ringoboot.verification.VerificationStore;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class VerificationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VerificationAutoConfiguration.class));

    @Test
    void doesNotConfigureVerificationByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(VerificationProperties.class);
            assertThat(context).doesNotHaveBean(CodeGenerator.class);
            assertThat(context).doesNotHaveBean(VerificationPolicy.class);
            assertThat(context).doesNotHaveBean(VerificationStore.class);
            assertThat(context).doesNotHaveBean(VerificationService.class);
        });
    }

    @Test
    void configuresInMemoryStoreWhenEnabled() {
        contextRunner.withPropertyValues("ringo.boot.verification.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(VerificationStore.class);
            assertThat(context.getBean(VerificationStore.class)).isInstanceOf(InMemoryVerificationStore.class);
            assertThat(context).hasSingleBean(VerificationService.class);
        });
    }

    @Test
    void configuresInMemoryVerificationWithConfiguredPolicy() {
        contextRunner
                .withPropertyValues(
                        "ringo.boot.verification.enabled=true",
                        "ringo.boot.verification.length=8",
                        "ringo.boot.verification.ttl=10m",
                        "ringo.boot.verification.max-attempts=3",
                        "ringo.boot.verification.resend-interval=30s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CodeGenerator.class);
                    assertThat(context).hasSingleBean(VerificationPolicy.class);
                    assertThat(context).hasSingleBean(VerificationStore.class);
                    assertThat(context.getBean(VerificationStore.class)).isInstanceOf(InMemoryVerificationStore.class);
                    assertThat(context).hasSingleBean(VerificationService.class);

                    VerificationPolicy policy = context.getBean(VerificationPolicy.class);
                    assertThat(policy.length()).isEqualTo(8);
                    assertThat(policy.ttl()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(policy.maxAttempts()).isEqualTo(3);
                    assertThat(policy.resendInterval()).isEqualTo(Duration.ofSeconds(30));
                });
    }

    @Test
    void backsOffForCustomGeneratorPolicyAndStore() {
        CodeGenerator generator = length -> "1".repeat(length);
        VerificationPolicy policy = new VerificationPolicy(4, Duration.ofMinutes(1), 2, Duration.ZERO);
        VerificationStore store = new TestVerificationStore();

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(CodeGenerator.class, () -> generator)
                .withBean(VerificationPolicy.class, () -> policy)
                .withBean(VerificationStore.class, () -> store)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CodeGenerator.class)).isSameAs(generator);
                    assertThat(context.getBean(VerificationPolicy.class)).isSameAs(policy);
                    assertThat(context.getBean(VerificationStore.class)).isSameAs(store);
                    assertThat(context.getBeansOfType(InMemoryVerificationStore.class))
                            .isEmpty();
                    assertThat(context).hasSingleBean(VerificationService.class);
                });
    }

    @Test
    void backsOffForCustomServiceWithoutRequiringStore() {
        VerificationService service = mock(VerificationService.class);

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(VerificationService.class, () -> service)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(VerificationService.class);
                    assertThat(context.getBean(VerificationService.class)).isSameAs(service);
                    assertThat(context).doesNotHaveBean(VerificationStore.class);
                });
    }

    @Test
    void failsWhenMultipleStoresHaveNoUniqueCandidate() {
        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean("firstStore", VerificationStore.class, TestVerificationStore::new)
                .withBean("secondStore", VerificationStore.class, TestVerificationStore::new)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("Verification requires a unique VerificationStore bean. "
                                    + "Mark one store as @Primary or provide a custom VerificationService");
                });
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ringo.boot.verification.length=0",
                "ringo.boot.verification.ttl=0s",
                "ringo.boot.verification.max-attempts=0",
                "ringo.boot.verification.resend-interval=-1s"
            })
    void failsForInvalidPolicy(String property) {
        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true", property)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                });
    }

    private static final class TestVerificationStore implements VerificationStore {

        @Override
        public StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt) {
            return new StoreResult.Stored(issuedAt.plus(policy.ttl()));
        }

        @Override
        public VerificationResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt) {
            return VerificationResult.NOT_FOUND;
        }

        @Override
        public boolean invalidate(VerificationKey key, String code) {
            return false;
        }
    }
}
