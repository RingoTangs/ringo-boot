package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.ringotangs.ringoboot.autoconfigure.verification.redis.RedisIssueRateLimitAutoConfiguration;
import io.github.ringotangs.ringoboot.autoconfigure.verification.redis.RedisIssueRateLimitStore;
import io.github.ringotangs.ringoboot.autoconfigure.verification.redis.RedisVerificationStore;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerificationService;
import io.github.ringotangs.ringoboot.verification.VerifyResult;
import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.email.EmailVerificationService;
import io.github.ringotangs.ringoboot.verification.email.StdoutEmailCodeSender;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueContext;
import io.github.ringotangs.ringoboot.verification.limit.IssueContextResolver;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitManager;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.sender.CodeSender;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeSender;
import io.github.ringotangs.ringoboot.verification.sms.SmsVerificationService;
import io.github.ringotangs.ringoboot.verification.sms.StdoutSmsCodeSender;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.store.StoreResult;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

class VerificationAutoConfigurationTest {

    private static final String SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RedisIssueRateLimitAutoConfiguration.class,
                    VerificationAutoConfiguration.class,
                    IssueRateLimitAutoConfiguration.class))
            .withPropertyValues("spring.application.name=test-application");

    @Test
    void failsWhenRedisApplicationNameIsMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VerificationAutoConfiguration.class))
                .withPropertyValues("ringo.boot.verification.enabled=true", "ringo.boot.verification.store=redis")
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withBean(VerificationHmacKey.class, VerificationAutoConfigurationTest::hmacKey)
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseMessage("Required key 'spring.application.name' not found"));
    }

    @Test
    void usesSpringApplicationName() {
        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true", "ringo.boot.verification.store=redis")
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withBean(VerificationHmacKey.class, VerificationAutoConfigurationTest::hmacKey)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RedisVerificationStore.class);
                });
    }

    @Test
    void failsWhenRedisApplicationNameIsInvalid() {
        contextRunner
                .withPropertyValues(
                        "spring.application.name=invalid application",
                        "ringo.boot.verification.enabled=true",
                        "ringo.boot.verification.store=redis")
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withBean(VerificationHmacKey.class, VerificationAutoConfigurationTest::hmacKey)
                .run(
                        context -> assertThat(context.getStartupFailure())
                                .hasRootCauseMessage(
                                        "applicationName must start with an alphanumeric character and contain only letters, digits, '.', '_', or '-': invalid application"));
    }

    @Test
    void doesNotConfigureVerificationByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(VerificationProperties.class);
            assertThat(context).doesNotHaveBean(CodeGenerator.class);
            assertThat(context).doesNotHaveBean(VerificationPolicy.class);
            assertThat(context).doesNotHaveBean(VerificationStore.class);
            assertThat(context).doesNotHaveBean(EmailCodeSender.class);
            assertThat(context).doesNotHaveBean(SmsCodeSender.class);
        });
    }

    @Test
    void verificationConfigurationDoesNotOwnRateLimitInfrastructure() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VerificationAutoConfiguration.class))
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(CodeGenerator.class);
                    assertThat(context).hasSingleBean(VerificationStore.class);
                    assertThat(context).hasSingleBean(EmailCodeSender.class);
                    assertThat(context).hasSingleBean(SmsCodeSender.class);
                    assertThat(context).doesNotHaveBean(IssueRateLimiter.class);
                    assertThat(context).doesNotHaveBean(VerificationService.class);
                });
    }

    @Test
    void configuresInMemoryStoreWhenEnabled() {
        contextRunner.withPropertyValues("ringo.boot.verification.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("numericCodeGenerator");
            assertThat(context).doesNotHaveBean("verificationCodeGenerator");
            assertThat(context).hasSingleBean(VerificationStore.class);
            assertThat(context.getBean(VerificationStore.class)).isInstanceOf(InMemoryVerificationStore.class);
            assertThat(context.getBean(EmailCodeSender.class)).isInstanceOf(StdoutEmailCodeSender.class);
            assertThat(context.getBean(SmsCodeSender.class)).isInstanceOf(StdoutSmsCodeSender.class);
            assertThat(context).getBeans(CodeSender.class).hasSize(2);
            assertThat(context).hasSingleBean(EmailVerificationService.class);
            assertThat(context).hasSingleBean(SmsVerificationService.class);
            assertThat(context).hasSingleBean(IssueContextResolver.class);
            VerificationKey key = new VerificationKey("account", "login", "user@example.com");
            assertThat(context.getBean(IssueContextResolver.class).resolve(key)).isEqualTo(IssueContext.of(key));
        });
    }

    @Test
    void configuresVerificationWithoutSpringWeb() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("org.springframework.web"))
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(VerificationStore.class);
                    assertThat(context).hasSingleBean(EmailVerificationService.class);
                    assertThat(context).hasSingleBean(SmsVerificationService.class);
                });
    }

    @Test
    void configuresRedisStoreWhenExplicitlySelected() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true", "ringo.boot.verification.store=redis")
                .withBean(StringRedisTemplate.class, () -> redisTemplate)
                .withBean(VerificationHmacKey.class, VerificationAutoConfigurationTest::hmacKey)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(VerificationStore.class);
                    assertThat(context.getBean(VerificationStore.class)).isInstanceOf(RedisVerificationStore.class);
                    assertThat(context).hasSingleBean(IssueRateLimiter.class);
                    assertThat(context.getBean(IssueRateLimiter.class)).isInstanceOf(IssueRateLimitManager.class);
                    assertThat(context).hasSingleBean(IssueRateLimitStore.class);
                    assertThat(context.getBean(IssueRateLimitStore.class)).isInstanceOf(RedisIssueRateLimitStore.class);
                    assertThat(context.getBeansOfType(InMemoryVerificationStore.class))
                            .isEmpty();
                });
    }

    @Test
    void configuresRedisStoreAfterSpringBootCreatesStringRedisTemplate() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        RedisIssueRateLimitAutoConfiguration.class,
                        VerificationAutoConfiguration.class,
                        IssueRateLimitAutoConfiguration.class,
                        RedisAutoConfiguration.class))
                .withPropertyValues(
                        "spring.application.name=test-application",
                        "ringo.boot.verification.enabled=true",
                        "ringo.boot.verification.store=redis")
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .withBean(VerificationHmacKey.class, VerificationAutoConfigurationTest::hmacKey)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(StringRedisTemplate.class);
                    assertThat(context).hasSingleBean(VerificationStore.class);
                    assertThat(context.getBean(VerificationStore.class)).isInstanceOf(RedisVerificationStore.class);
                    assertThat(context).hasSingleBean(EmailVerificationService.class);
                    assertThat(context).hasSingleBean(SmsVerificationService.class);
                });
    }

    @Test
    void failsWhenRedisIsSelectedWithoutTemplate() {
        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true", "ringo.boot.verification.store=redis")
                .withBean(VerificationHmacKey.class, VerificationAutoConfigurationTest::hmacKey)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "Redis verification storage requires Spring Data Redis and a StringRedisTemplate");
                });
    }

    @Test
    void failsWhenRedisIsSelectedWithoutSpringDataRedis() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(StringRedisTemplate.class))
                .withPropertyValues("ringo.boot.verification.enabled=true", "ringo.boot.verification.store=redis")
                .withBean(VerificationHmacKey.class, VerificationAutoConfigurationTest::hmacKey)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class);
                });
    }

    @Test
    void failsWhenRedisHmacKeyIsMissing() {
        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true", "ringo.boot.verification.store=redis")
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
                });
    }

    @Test
    void customStoreOverridesExplicitRedisSelection() {
        VerificationStore store = new TestVerificationStore();

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true", "ringo.boot.verification.store=redis")
                .withBean(VerificationStore.class, () -> store)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(VerificationStore.class)).isSameAs(store);
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
                        "ringo.boot.verification.issue-rate-limit.interval=30s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CodeGenerator.class);
                    assertThat(context).doesNotHaveBean(VerificationPolicy.class);
                    assertThat(context).hasSingleBean(VerificationStore.class);
                    assertThat(context.getBean(VerificationStore.class)).isInstanceOf(InMemoryVerificationStore.class);
                    assertThat(context.getBean(IssueRateLimiter.class)).isInstanceOf(IssueRateLimitManager.class);
                    assertThat(context.getBean(IssueRateLimitStore.class))
                            .isInstanceOf(InMemoryIssueRateLimitStore.class);
                    assertThat(context).getBeans(VerificationService.class).hasSize(2);

                    VerificationPolicy policy =
                            context.getBean(VerificationProperties.class).toPolicy();
                    assertThat(policy.length()).isEqualTo(8);
                    assertThat(policy.ttl()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(policy.maxAttempts()).isEqualTo(3);
                    assertThat(context.getBean(IssueRateLimitProperties.class).getInterval())
                            .isEqualTo(Duration.ofSeconds(30));
                });
    }

    @Test
    void backsOffForCustomGeneratorAndStore() {
        CodeGenerator generator = length -> "1".repeat(length);
        VerificationStore store = new TestVerificationStore();

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(CodeGenerator.class, () -> generator)
                .withBean(VerificationStore.class, () -> store)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CodeGenerator.class)).isSameAs(generator);
                    assertThat(context).doesNotHaveBean(VerificationPolicy.class);
                    assertThat(context.getBean(VerificationStore.class)).isSameAs(store);
                    assertThat(context.getBeansOfType(InMemoryVerificationStore.class))
                            .isEmpty();
                    assertThat(context).getBeans(VerificationService.class).hasSize(2);
                });
    }

    @Test
    void ignoresBusinessPolicyBeansWhenConfiguringChannelDefaults() {
        VerificationPolicy loginPolicy = new VerificationPolicy(4, Duration.ofMinutes(1), 2);
        VerificationPolicy registrationPolicy = new VerificationPolicy(8, Duration.ofMinutes(10), 3);

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true", "ringo.boot.verification.length=6")
                .withBean("loginPolicy", VerificationPolicy.class, () -> loginPolicy)
                .withBean("registrationPolicy", VerificationPolicy.class, () -> registrationPolicy)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBeans(VerificationPolicy.class).hasSize(2);
                    assertThat(context).getBeans(VerificationService.class).hasSize(2);
                    assertThat(context.getBean(VerificationProperties.class)
                                    .toPolicy()
                                    .length())
                            .isEqualTo(6);
                });
    }

    @Test
    void configuresEmailServiceWhenSenderIsAvailable() {
        EmailCodeSender sender = delivery -> CodeSendResult.ACCEPTED;

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(EmailCodeSender.class, () -> sender)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(EmailVerificationService.class);
                    assertThat(context).hasSingleBean(SmsVerificationService.class);
                    assertThat(context.getBean(EmailCodeSender.class)).isSameAs(sender);
                    assertThat(context.getBean(SmsCodeSender.class)).isInstanceOf(StdoutSmsCodeSender.class);
                });
    }

    @Test
    void configuresSmsServiceWhenSenderIsAvailable() {
        SmsCodeSender sender = delivery -> CodeSendResult.ACCEPTED;

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(SmsCodeSender.class, () -> sender)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SmsVerificationService.class);
                    assertThat(context).hasSingleBean(EmailVerificationService.class);
                    assertThat(context.getBean(SmsCodeSender.class)).isSameAs(sender);
                    assertThat(context.getBean(EmailCodeSender.class)).isInstanceOf(StdoutEmailCodeSender.class);
                });
    }

    @Test
    void configuresBothChannelServicesWhenBothSendersAreAvailable() {
        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(EmailCodeSender.class, () -> delivery -> CodeSendResult.ACCEPTED)
                .withBean(SmsCodeSender.class, () -> delivery -> CodeSendResult.ACCEPTED)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(EmailVerificationService.class);
                    assertThat(context).hasSingleBean(SmsVerificationService.class);
                    assertThat(context).getBeans(VerificationService.class).hasSize(2);
                });
    }

    @Test
    void backsOffForCustomEmailService() {
        EmailCodeSender sender = delivery -> CodeSendResult.ACCEPTED;
        EmailVerificationService service = new EmailVerificationService(
                length -> "1".repeat(length),
                new TestVerificationStore(),
                IssueRateLimiter.permitAll(),
                VerificationPolicy.defaults(),
                sender);

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(EmailCodeSender.class, () -> sender)
                .withBean(EmailVerificationService.class, () -> service)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(EmailVerificationService.class);
                    assertThat(context.getBean(EmailVerificationService.class)).isSameAs(service);
                });
    }

    @Test
    void allowsMultipleCustomStoresWithoutSelectingOne() {
        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean("firstStore", VerificationStore.class, TestVerificationStore::new)
                .withBean("secondStore", VerificationStore.class, TestVerificationStore::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBeans(VerificationStore.class).hasSize(2);
                    assertThat(context.getBeansOfType(InMemoryVerificationStore.class))
                            .isEmpty();
                    assertThat(context).doesNotHaveBean(VerificationService.class);
                });
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ringo.boot.verification.length=0",
                "ringo.boot.verification.ttl=0s",
                "ringo.boot.verification.max-attempts=0",
                "ringo.boot.verification.issue-rate-limit.interval=-1s"
            })
    void failsForInvalidPolicy(String property) {
        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true", property)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                });
    }

    private static VerificationHmacKey hmacKey() {
        return VerificationHmacKey.fromBase64(SECRET);
    }

    private static final class TestVerificationStore implements VerificationStore {

        @Override
        public StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt) {
            return new StoreResult(issuedAt.plus(policy.ttl()));
        }

        @Override
        public VerifyResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt) {
            return VerifyResult.NOT_FOUND;
        }

        @Override
        public boolean invalidate(VerificationKey key, String code) {
            return false;
        }
    }
}
