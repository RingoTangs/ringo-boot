package io.github.ringotangs.ringoboot.verification.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import io.github.ringotangs.ringoboot.verification.limit.ClientIpQuotaRule;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;

class ClientIpContributorTest {

    private static final VerificationKey KEY = new VerificationKey("account", "login", "user@example.com");
    private static final VerificationPolicy POLICY = VerificationPolicy.defaults();

    @Test
    void usesClientIpRuleAttributeName() {
        assertThat(ClientIpContributor.ATTRIBUTE_NAME).isEqualTo(ClientIpQuotaRule.ATTRIBUTE_NAME);
    }

    @ParameterizedTest
    @ValueSource(strings = {"203.0.113.10", "2001:db8::1"})
    void contributesClientIpAndPreservesContext(String clientIp) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(clientIp);
        IssueContext context =
                IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY).with("device-id", "device-123");
        ObjectProvider<HttpServletRequest> requestProvider = requestProvider(request);

        IssueContext contributed = new ClientIpContributor(requestProvider).contribute(context);

        assertThat(contributed.key()).isEqualTo(KEY);
        assertThat(contributed.channel()).isEqualTo(VerificationChannel.EMAIL);
        assertThat(contributed.policy()).isEqualTo(POLICY);
        assertThat(contributed.attributes())
                .containsEntry("device-id", "device-123")
                .containsEntry(ClientIpContributor.ATTRIBUTE_NAME, clientIp);
        assertThat(context.attribute(ClientIpContributor.ATTRIBUTE_NAME)).isEmpty();
        verify(requestProvider).getObject();
        verify(request).getRemoteAddr();
        verifyNoMoreInteractions(request);
    }

    @Test
    void usesOnlyServletRemoteAddress() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.0.2.10");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10");

        IssueContext contributed = new ClientIpContributor(requestProvider(request))
                .contribute(IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY));

        assertThat(contributed.attribute(ClientIpContributor.ATTRIBUTE_NAME)).contains("192.0.2.10");
        verify(request).getRemoteAddr();
        verifyNoMoreInteractions(request);
    }

    @Test
    void resolvesCurrentRequestForEveryContribution() {
        HttpServletRequest firstRequest = mock(HttpServletRequest.class);
        HttpServletRequest secondRequest = mock(HttpServletRequest.class);
        when(firstRequest.getRemoteAddr()).thenReturn("192.0.2.10");
        when(secondRequest.getRemoteAddr()).thenReturn("198.51.100.20");
        ObjectProvider<HttpServletRequest> requestProvider = requestProvider(firstRequest);
        when(requestProvider.getObject()).thenReturn(firstRequest, secondRequest);
        ClientIpContributor contributor = new ClientIpContributor(requestProvider);
        IssueContext context = IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY);

        IssueContext first = contributor.contribute(context);
        IssueContext second = contributor.contribute(context);

        assertThat(first.attribute(ClientIpContributor.ATTRIBUTE_NAME)).contains("192.0.2.10");
        assertThat(second.attribute(ClientIpContributor.ATTRIBUTE_NAME)).contains("198.51.100.20");
        verify(requestProvider, times(2)).getObject();
    }

    @Test
    void propagatesFailureWhenCurrentRequestIsUnavailable() {
        ObjectProvider<HttpServletRequest> requestProvider = requestProvider(null);
        IllegalStateException failure = new IllegalStateException("no current request");
        when(requestProvider.getObject()).thenThrow(failure);

        assertThatThrownBy(() -> new ClientIpContributor(requestProvider)
                        .contribute(IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY)))
                .isSameAs(failure);
    }

    @Test
    void rejectsNullRequestProvider() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ClientIpContributor(null))
                .withMessage("requestProvider must not be null");
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<HttpServletRequest> requestProvider(HttpServletRequest request) {
        ObjectProvider<HttpServletRequest> requestProvider = mock(ObjectProvider.class);
        when(requestProvider.getObject()).thenReturn(request);
        return requestProvider;
    }
}
