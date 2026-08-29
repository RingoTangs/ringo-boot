package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ringotangs.ringoboot.verification.IssueContextContributor;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class ClientAddressIssueRateLimitAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ClientAddressIssueRateLimitAutoConfiguration.class))
            .withPropertyValues("ringo.boot.verification.enabled=true");

    @Test
    void doesNotConfigureWithoutClientAddressRule() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ClientAddressResolver.class);
            assertThat(context).doesNotHaveBean(ClientAddressIssueContextContributor.class);
        });
    }

    @Test
    void configuresResolverAndContributorForClientAddressRule() {
        contextRunner
                .withBean(ClientAddressIssueQuotaRule.class, ClientAddressIssueRateLimitAutoConfigurationTest::rule)
                .run(context -> {
                    assertThat(context).hasSingleBean(ClientAddressResolver.class);
                    assertThat(context.getBean(ClientAddressResolver.class))
                            .isInstanceOf(ServletClientAddressResolver.class);
                    assertThat(context).hasSingleBean(ClientAddressIssueContextContributor.class);
                    assertThat(context).hasSingleBean(IssueContextContributor.class);
                });
    }

    @Test
    void backsOffForCustomResolver() {
        ClientAddressResolver custom = () -> "198.51.100.10";

        contextRunner
                .withBean(ClientAddressIssueQuotaRule.class, ClientAddressIssueRateLimitAutoConfigurationTest::rule)
                .withBean(ClientAddressResolver.class, () -> custom)
                .run(context ->
                        assertThat(context.getBean(ClientAddressResolver.class)).isSameAs(custom));
    }

    @Test
    void doesNotConfigureInNonWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ClientAddressIssueRateLimitAutoConfiguration.class))
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(ClientAddressIssueQuotaRule.class, ClientAddressIssueRateLimitAutoConfigurationTest::rule)
                .run(context -> assertThat(context).doesNotHaveBean(ClientAddressResolver.class));
    }

    private static ClientAddressIssueQuotaRule rule() {
        return ClientAddressIssueQuotaRule.builder()
                .id("email-client-address-minute")
                .namespace("account")
                .purpose("email-verification")
                .channel(io.github.ringotangs.ringoboot.verification.VerificationChannel.EMAIL)
                .maxIssues(10)
                .window(Duration.ofMinutes(1))
                .build();
    }
}
