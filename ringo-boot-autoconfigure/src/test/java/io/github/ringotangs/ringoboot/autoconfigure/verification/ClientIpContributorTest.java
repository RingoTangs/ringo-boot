package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClientIpContributorTest {

    private static final VerificationKey KEY = new VerificationKey("account", "login", "user@example.com");
    private static final VerificationPolicy POLICY = VerificationPolicy.defaults();

    @ParameterizedTest
    @ValueSource(strings = {"203.0.113.10", "2001:db8::1"})
    void contributesClientIpAndPreservesContext(String clientIp) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(" " + clientIp + " ");
        IssueContext context =
                IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY).with("device-id", "device-123");

        IssueContext contributed = new ClientIpContributor(request).contribute(context);

        assertThat(contributed.key()).isEqualTo(KEY);
        assertThat(contributed.channel()).isEqualTo(VerificationChannel.EMAIL);
        assertThat(contributed.policy()).isEqualTo(POLICY);
        assertThat(contributed.attributes())
                .containsEntry("device-id", "device-123")
                .containsEntry(ClientIpContributor.ATTRIBUTE_NAME, clientIp);
        assertThat(context.attribute(ClientIpContributor.ATTRIBUTE_NAME)).isEmpty();
        verify(request).getRemoteAddr();
        verifyNoMoreInteractions(request);
    }

    @Test
    void usesOnlyServletRemoteAddress() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.0.2.10");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10");

        IssueContext contributed =
                new ClientIpContributor(request).contribute(IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY));

        assertThat(contributed.attribute(ClientIpContributor.ATTRIBUTE_NAME)).contains("192.0.2.10");
        verify(request).getRemoteAddr();
        verifyNoMoreInteractions(request);
    }

    @Test
    void rejectsNullRequestAndContext() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ClientIpContributor(null))
                .withMessage("request must not be null");

        ClientIpContributor contributor = new ClientIpContributor(mock(HttpServletRequest.class));
        assertThatNullPointerException()
                .isThrownBy(() -> contributor.contribute(null))
                .withMessage("context must not be null");
    }

    @Test
    void rejectsMissingOrBlankRemoteAddress() {
        HttpServletRequest missingAddress = mock(HttpServletRequest.class);
        when(missingAddress.getRemoteAddr()).thenReturn(null);
        HttpServletRequest blankAddress = mock(HttpServletRequest.class);
        when(blankAddress.getRemoteAddr()).thenReturn("  ");
        IssueContext context = IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY);

        assertThatNullPointerException()
                .isThrownBy(() -> new ClientIpContributor(missingAddress).contribute(context))
                .withMessage("request remote address must not be null");
        assertThatIllegalStateException()
                .isThrownBy(() -> new ClientIpContributor(blankAddress).contribute(context))
                .withMessage("request remote address must not be blank");
    }

    @Test
    void refusesToOverwriteExistingClientIp() {
        IssueContext context = IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY)
                .with(ClientIpContributor.ATTRIBUTE_NAME, "203.0.113.10");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ClientIpContributor(mock(HttpServletRequest.class)).contribute(context))
                .withMessage("issue context attribute already exists: client-ip");
    }
}
