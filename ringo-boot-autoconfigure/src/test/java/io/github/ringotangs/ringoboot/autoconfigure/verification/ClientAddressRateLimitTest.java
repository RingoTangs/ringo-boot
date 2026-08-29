package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.IssueContextAttributes;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class ClientAddressRateLimitTest {

    private static final IssueContext EMAIL = IssueContext.of(
            new VerificationKey("account", "email-verification", "first@example.com"),
            VerificationChannel.EMAIL,
            VerificationPolicy.defaults());

    @Test
    void aggregatesSubjectsByClientAddressAndSeparatesAddresses() {
        ClientAddressIssueQuotaRule rule = rule();
        IssueContext otherSubject = IssueContext.of(
                        new VerificationKey("account", "email-verification", "second@example.com"),
                        VerificationChannel.EMAIL,
                        VerificationPolicy.defaults())
                .with(IssueContextAttributes.CLIENT_ADDRESS, "203.0.113.10");
        IssueContext sameAddress = EMAIL.with(IssueContextAttributes.CLIENT_ADDRESS, "203.0.113.10");
        IssueContext differentAddress = EMAIL.with(IssueContextAttributes.CLIENT_ADDRESS, "203.0.113.11");

        assertThat(rule.bucket(otherSubject)).isEqualTo(rule.bucket(sameAddress));
        assertThat(rule.bucket(differentAddress)).isNotEqualTo(rule.bucket(sameAddress));
        assertThat(rule.matches(EMAIL)).isTrue();
        assertThat(rule.matches(IssueContext.of(EMAIL.key(), VerificationChannel.SMS, VerificationPolicy.defaults())))
                .isFalse();
    }

    @Test
    void failsClosedWhenMatchingContextHasNoClientAddress() {
        assertThatThrownBy(() -> rule().bucket(EMAIL))
                .isInstanceOf(ClientAddressResolutionException.class)
                .hasMessageContaining(IssueContextAttributes.CLIENT_ADDRESS);
    }

    @Test
    void contributorResolvesOnlyForMatchingRules() {
        ClientAddressResolver resolver = mock(ClientAddressResolver.class);
        when(resolver.resolve()).thenReturn("203.0.113.10");
        ClientAddressIssueContextContributor contributor =
                new ClientAddressIssueContextContributor(List.of(rule()), resolver);

        IssueContext contributed = contributor.contribute(EMAIL);
        IssueContext sms = IssueContext.of(EMAIL.key(), VerificationChannel.SMS, EMAIL.policy());

        assertThat(contributed.attribute(IssueContextAttributes.CLIENT_ADDRESS)).contains("203.0.113.10");
        assertThat(contributor.contribute(sms)).isSameAs(sms);
    }

    @Test
    void servletResolverNormalizesIpLiteralsAndIgnoresForwardedHeader() {
        @SuppressWarnings("unchecked")
        ObjectProvider<HttpServletRequest> provider = mock(ObjectProvider.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(provider.getIfAvailable()).thenReturn(request);
        when(request.getRemoteAddr()).thenReturn(" 203.000.113.010 ");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.9");

        assertThat(new ServletClientAddressResolver(provider).resolve()).isEqualTo("203.0.113.10");
        verify(request, never()).getHeader("X-Forwarded-For");
        assertThat(ServletClientAddressResolver.normalize("2001:0db8:0:0:0:0:0:1"))
                .isEqualTo("2001:db8:0:0:0:0:0:1");
    }

    @Test
    void servletResolverRejectsMissingRequestAndNonIpValues() {
        @SuppressWarnings("unchecked")
        ObjectProvider<HttpServletRequest> provider = mock(ObjectProvider.class);

        assertThatThrownBy(() -> new ServletClientAddressResolver(provider).resolve())
                .isInstanceOf(ClientAddressResolutionException.class)
                .hasMessage("current servlet request is unavailable");
        assertThatThrownBy(() -> ServletClientAddressResolver.normalize("example.com"))
                .isInstanceOf(ClientAddressResolutionException.class);
        assertThatThrownBy(() -> ServletClientAddressResolver.normalize("fe80::1%eth0"))
                .isInstanceOf(ClientAddressResolutionException.class);
    }

    private static ClientAddressIssueQuotaRule rule() {
        return ClientAddressIssueQuotaRule.builder()
                .id("email-client-address-minute")
                .namespace("account")
                .purpose("email-verification")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(10)
                .window(Duration.ofMinutes(1))
                .build();
    }
}
