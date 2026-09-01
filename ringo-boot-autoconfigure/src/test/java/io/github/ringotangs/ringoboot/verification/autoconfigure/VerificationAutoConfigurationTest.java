package io.github.ringotangs.ringoboot.verification.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.ringotangs.ringoboot.verification.DefaultIssueContextManager;
import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.IssueContextManager;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerifyResult;
import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.email.EmailVerificationService;
import io.github.ringotangs.ringoboot.verification.email.StdoutEmailCodeSender;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimiter;
import io.github.ringotangs.ringoboot.verification.redis.RedisVerificationStore;
import io.github.ringotangs.ringoboot.verification.redis.VerificationHmacKey;
import io.github.ringotangs.ringoboot.verification.servlet.ClientIpContributor;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeSender;
import io.github.ringotangs.ringoboot.verification.sms.SmsVerificationService;
import io.github.ringotangs.ringoboot.verification.sms.StdoutSmsCodeSender;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.store.StoreResult;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class VerificationAutoConfigurationTest {

    private static final String SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(VerificationAutoConfiguration.class, IssueLimitAutoConfiguration.class))
            .withPropertyValues("spring.application.name=test-application");

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VerificationAutoConfiguration.class))
            .withPropertyValues("ringo.boot.verification.enabled=true");

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
            assertThat(context).doesNotHaveBean(ClientIpContributor.class);
        });
    }

    @Test
    void doesNotConfigureClientIpContributorByDefault() {
        webContextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ClientIpContributor.class);
        });
    }

    @Test
    void clientIpContributorSwitchDoesNotEnableVerification() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VerificationAutoConfiguration.class))
                .withPropertyValues("ringo.boot.verification.contributor.client-ip=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ClientIpContributor.class);
                });
    }

    @Test
    void doesNotConfigureClientIpContributorWhenExplicitlyDisabled() {
        webContextRunner
                .withPropertyValues("ringo.boot.verification.contributor.client-ip=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ClientIpContributor.class);
                });
    }

    @Test
    void configuresClientIpContributorInServletApplication() {
        webContextRunner
                .withPropertyValues("ringo.boot.verification.contributor.client-ip=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ClientIpContributor.class);

                    MockHttpServletRequest request = new MockHttpServletRequest(
                            context.getSourceApplicationContext().getServletContext());
                    request.setRemoteAddr("203.0.113.10");
                    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
                    try {
                        IssueContext issueContext = IssueContext.of(
                                new VerificationKey("account", "login", "user@example.com"),
                                VerificationChannel.EMAIL,
                                VerificationPolicy.defaults());

                        IssueContext enriched =
                                context.getBean(IssueContextManager.class).enrich(issueContext);

                        assertThat(enriched.attribute(ClientIpContributor.ATTRIBUTE_NAME))
                                .contains("203.0.113.10");
                    } finally {
                        RequestContextHolder.resetRequestAttributes();
                    }
                });
    }

    @Test
    void customClientIpContributorOverridesDefault() {
        ClientIpContributor contributor = new ClientIpContributor(requestProvider());

        webContextRunner
                .withPropertyValues("ringo.boot.verification.contributor.client-ip=true")
                .withBean(ClientIpContributor.class, () -> contributor)
                .run(context -> {
                    assertThat(context).hasSingleBean(ClientIpContributor.class);
                    assertThat(context.getBean(ClientIpContributor.class)).isSameAs(contributor);
                });
    }

    @Test
    void verificationConfigurationDoesNotOwnLimitInfrastructure() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VerificationAutoConfiguration.class))
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CodeGenerator.class);
                    assertThat(context).hasSingleBean(VerificationStore.class);
                    assertThat(context).hasSingleBean(EmailCodeSender.class);
                    assertThat(context).hasSingleBean(SmsCodeSender.class);
                    assertThat(context).doesNotHaveBean(IssueLimiter.class);
                    assertThat(context).doesNotHaveBean(EmailVerificationService.class);
                    assertThat(context).doesNotHaveBean(SmsVerificationService.class);
                });
    }

    @Test
    void configuresInMemoryStoreWhenEnabled() {
        contextRunner.withPropertyValues("ringo.boot.verification.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(CodeGenerator.class);
            assertThat(context).hasSingleBean(VerificationStore.class);
            assertThat(context.getBean(VerificationStore.class)).isInstanceOf(InMemoryVerificationStore.class);
            assertThat(context.getBean(EmailCodeSender.class)).isInstanceOf(StdoutEmailCodeSender.class);
            assertThat(context.getBean(SmsCodeSender.class)).isInstanceOf(StdoutSmsCodeSender.class);
            assertThat(context).hasSingleBean(IssueContextManager.class);
            assertThat(context.getBean(IssueContextManager.class)).isInstanceOf(DefaultIssueContextManager.class);
            assertThat(context).doesNotHaveBean(ClientIpContributor.class);
            assertThat(context).doesNotHaveBean(EmailVerificationService.class);
            assertThat(context).doesNotHaveBean(SmsVerificationService.class);
        });
    }

    @Test
    void customIssueContextManagerOverridesDefault() {
        IssueContextManager manager = context -> context.with("tenant", "tenant-1");

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(IssueContextManager.class, () -> manager)
                .run(context -> {
                    assertThat(context).hasSingleBean(IssueContextManager.class);
                    assertThat(context.getBean(IssueContextManager.class)).isSameAs(manager);
                    assertThat(context).doesNotHaveBean(DefaultIssueContextManager.class);
                });
    }

    @Test
    void configuresVerificationWithoutSpringWeb() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("org.springframework.web"))
                .withPropertyValues(
                        "ringo.boot.verification.enabled=true", "ringo.boot.verification.contributor.client-ip=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(VerificationStore.class);
                    assertThat(context).doesNotHaveBean(CodeGenerator.class);
                    assertThat(context).doesNotHaveBean(EmailVerificationService.class);
                    assertThat(context).doesNotHaveBean(SmsVerificationService.class);
                    assertThat(context).doesNotHaveBean(ClientIpContributor.class);
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
                    assertThat(context).hasSingleBean(IssueLimiter.class);
                    assertThat(context).doesNotHaveBean(IssueLimitStore.class);
                    assertThat(context.getBeansOfType(InMemoryVerificationStore.class))
                            .isEmpty();
                });
    }

    @Test
    void configuresRedisStoreAfterSpringBootCreatesStringRedisTemplate() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        VerificationAutoConfiguration.class,
                        IssueLimitAutoConfiguration.class,
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
                    assertThat(context).doesNotHaveBean(CodeGenerator.class);
                    assertThat(context).doesNotHaveBean(EmailVerificationService.class);
                    assertThat(context).doesNotHaveBean(SmsVerificationService.class);
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
                        "ringo.boot.verification.max-attempts=3")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(CodeGenerator.class);
                    assertThat(context).doesNotHaveBean(VerificationPolicy.class);
                    assertThat(context).hasSingleBean(VerificationStore.class);
                    assertThat(context.getBean(VerificationStore.class)).isInstanceOf(InMemoryVerificationStore.class);
                    assertThat(context).hasSingleBean(IssueLimiter.class);
                    assertThat(context).doesNotHaveBean(IssueLimitStore.class);
                    assertThat(context).doesNotHaveBean(EmailVerificationService.class);
                    assertThat(context).doesNotHaveBean(SmsVerificationService.class);

                    VerificationPolicy policy =
                            context.getBean(VerificationProperties.class).toPolicy();
                    assertThat(policy.length()).isEqualTo(8);
                    assertThat(policy.ttl()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(policy.maxAttempts()).isEqualTo(3);
                });
    }

    @Test
    void backsOffForCustomStore() {
        VerificationStore store = new TestVerificationStore();

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(VerificationStore.class, () -> store)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(CodeGenerator.class);
                    assertThat(context).doesNotHaveBean(VerificationPolicy.class);
                    assertThat(context.getBean(VerificationStore.class)).isSameAs(store);
                    assertThat(context.getBeansOfType(InMemoryVerificationStore.class))
                            .isEmpty();
                    assertThat(context).doesNotHaveBean(EmailVerificationService.class);
                    assertThat(context).doesNotHaveBean(SmsVerificationService.class);
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
                    assertThat(context).doesNotHaveBean(EmailVerificationService.class);
                    assertThat(context).doesNotHaveBean(SmsVerificationService.class);
                    assertThat(context.getBean(VerificationProperties.class)
                                    .toPolicy()
                                    .length())
                            .isEqualTo(6);
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
                    assertThat(context).doesNotHaveBean(EmailVerificationService.class);
                    assertThat(context).doesNotHaveBean(SmsVerificationService.class);
                });
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ringo.boot.verification.length=0",
                "ringo.boot.verification.ttl=0s",
                "ringo.boot.verification.max-attempts=0"
            })
    void rejectsInvalidPolicyWhenConverted(String property) {
        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true", property)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThatThrownBy(() -> context.getBean(VerificationProperties.class)
                                    .toPolicy())
                            .isInstanceOf(IllegalArgumentException.class);
                });
    }

    private static VerificationHmacKey hmacKey() {
        return VerificationHmacKey.fromBase64(SECRET);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<HttpServletRequest> requestProvider() {
        return mock(ObjectProvider.class);
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
