package io.github.ringotangs.ringoboot.sample.verification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ringotangs.ringoboot.verification.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.NamespaceIssueQuotaRule;
import io.github.ringotangs.ringoboot.verification.limit.PurposeIssueQuotaRule;
import io.github.ringotangs.ringoboot.verification.limit.SubjectIssueQuotaRule;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "ringo.boot.verification.store=memory")
@AutoConfigureMockMvc
@Import(EmailVerificationControllerTest.SenderTestConfiguration.class)
class EmailVerificationControllerTest {

    private static final String VALIDATION_FAILED_TYPE = "urn:problem:mvc:validation-failed";
    private static final String INVALID_CODE_TYPE = "urn:problem:business:verification:invalid-code";
    private static final String THROTTLED_TYPE = "urn:problem:business:verification:throttled";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CapturingEmailCodeSender sender;

    @Autowired
    private List<IssueRateLimitRule> issueRateLimitRules;

    @Test
    void issuesDeliversVerifiesAndConsumesCode() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/verification/email/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issueRequest(email)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.expiresAt").isString())
                .andExpect(jsonPath("$.code").doesNotExist());

        CapturedDelivery message = sender.latest(email);
        org.assertj.core.api.Assertions.assertThat(message.code()).matches("\\d{6}");

        mockMvc.perform(post("/verification/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyRequest("  " + email.toUpperCase(Locale.ROOT) + "  ", message.code())))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertInvalidCode(email, message.code());
    }

    @Test
    void throttlesRepeatedIssuanceAndSeparatesSubjects() throws Exception {
        String email = uniqueEmail();
        issue(email);
        for (int issued = 1; issued < 5; issued++) {
            issue(email);
        }
        CapturedDelivery latestMessage = sender.latest(email);

        mockMvc.perform(post("/verification/email/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issueRequest(email)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, org.hamcrest.Matchers.matchesPattern("\\d+")))
                .andExpect(jsonPath("$.type").value(THROTTLED_TYPE))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());

        org.assertj.core.api.Assertions.assertThat(sender.latest(email)).isSameAs(latestMessage);

        issue(uniqueEmail());
    }

    @Test
    void registersLayeredIssueRateLimitRules() {
        org.assertj.core.api.Assertions.assertThat(issueRateLimitRules)
                .containsExactlyInAnyOrder(
                        new NamespaceIssueQuotaRule(
                                "account-email-hourly-quota",
                                "account",
                                VerificationChannel.EMAIL,
                                100,
                                Duration.ofHours(1L)),
                        new PurposeIssueQuotaRule(
                                "email-verification-hourly-quota",
                                "account",
                                "email-verification",
                                VerificationChannel.EMAIL,
                                10,
                                Duration.ofHours(1L)),
                        new SubjectIssueQuotaRule(
                                "email-verification-resend-cooldown",
                                "account",
                                "email-verification",
                                VerificationChannel.EMAIL,
                                5,
                                Duration.ofMinutes(1L)));
    }

    @Test
    void hidesInternalReasonForWrongAndUnknownCodes() throws Exception {
        String issuedEmail = uniqueEmail();
        issue(issuedEmail);
        String issuedCode = sender.latest(issuedEmail).code();
        String wrongCode = issuedCode.startsWith("0") ? "1" + issuedCode.substring(1) : "0" + issuedCode.substring(1);

        assertInvalidCode(issuedEmail, wrongCode);
        assertInvalidCode(uniqueEmail(), "123456");
    }

    @Test
    void localizesInvalidCodeForChineseRequest() throws Exception {
        mockMvc.perform(post("/verification/email/verify")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyRequest(uniqueEmail(), "123456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(INVALID_CODE_TYPE))
                .andExpect(jsonPath("$.title").value("验证码无效"))
                .andExpect(jsonPath("$.detail").value("验证码无效"));
    }

    @Test
    void validatesIssueRequestEmail() throws Exception {
        mockMvc.perform(post("/verification/email/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issueRequest("not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(VALIDATION_FAILED_TYPE));
    }

    @Test
    void validatesVerificationCodeFormat() throws Exception {
        for (String code : new String[] {"", "12345", "1234567", "12a456"}) {
            mockMvc.perform(post("/verification/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(verifyRequest(uniqueEmail(), code)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(VALIDATION_FAILED_TYPE));
        }
    }

    private void issue(String email) throws Exception {
        mockMvc.perform(post("/verification/email/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issueRequest(email)))
                .andExpect(status().isAccepted());
    }

    private void assertInvalidCode(String email, String code) throws Exception {
        mockMvc.perform(post("/verification/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyRequest(email, code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(INVALID_CODE_TYPE))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid verification code"))
                .andExpect(jsonPath("$.detail").value("The verification code is invalid"));
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    private String issueRequest(String email) {
        return """
                {"email":"%s"}
                """.formatted(email);
    }

    private String verifyRequest(String email, String code) {
        return """
                {"email":"%s","code":"%s"}
                """.formatted(email, code);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SenderTestConfiguration {

        @Bean
        CapturingEmailCodeSender capturingEmailCodeSender() {
            return new CapturingEmailCodeSender();
        }
    }

    static final class CapturingEmailCodeSender implements EmailCodeSender {

        private final Map<String, CapturedDelivery> messages = new ConcurrentHashMap<>();

        @Override
        public CodeSendResult send(IssueContext context, String code, Instant expiresAt) {
            messages.put(context.key().subject(), new CapturedDelivery(context, code, expiresAt));
            return CodeSendResult.ACCEPTED;
        }

        CapturedDelivery latest(String email) {
            return Objects.requireNonNull(
                    messages.get(email.strip().toLowerCase(Locale.ROOT)), "No message found for " + email);
        }
    }

    private record CapturedDelivery(IssueContext context, String code, Instant expiresAt) {}
}
