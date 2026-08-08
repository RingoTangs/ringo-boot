package io.github.ringotangs.ringoboot.sample.verification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EmailVerificationControllerTest {

    private static final String VALIDATION_FAILED_TYPE = "urn:problem:mvc:validation-failed";
    private static final String INVALID_CODE_TYPE = "urn:problem:business:verification:invalid-code";

    @Autowired
    private MockMvc mockMvc;

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

        String inboxBody = mockMvc.perform(get("/verification/email/test-inbox").param("email", email.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("\\d{6}")))
                .andExpect(jsonPath("$.expiresAt").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String code = JsonPath.read(inboxBody, "$.code");

        mockMvc.perform(post("/verification/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyRequest("  " + email.toUpperCase() + "  ", code)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertInvalidCode(email, code);
    }

    @Test
    void throttlesRepeatedIssuanceWithoutReplacingDeliveredCode() throws Exception {
        String email = uniqueEmail();
        issue(email);
        String originalCode = latestCode(email);

        mockMvc.perform(post("/verification/email/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issueRequest(email)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("urn:problem:business:verification:throttled"))
                .andExpect(jsonPath("$.status").value(429));

        mockMvc.perform(get("/verification/email/test-inbox").param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(originalCode));
    }

    @Test
    void hidesInternalReasonForWrongAndUnknownCodes() throws Exception {
        String issuedEmail = uniqueEmail();
        issue(issuedEmail);
        String issuedCode = latestCode(issuedEmail);
        String wrongCode = issuedCode.startsWith("0") ? "1" + issuedCode.substring(1) : "0" + issuedCode.substring(1);

        assertInvalidCode(issuedEmail, wrongCode);
        assertInvalidCode(uniqueEmail(), "123456");
    }

    @Test
    void returnsNotFoundForEmptyTestInbox() throws Exception {
        mockMvc.perform(get("/verification/email/test-inbox").param("email", uniqueEmail()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:business:verification:test-message-not-found"))
                .andExpect(jsonPath("$.status").value(404));
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

    private String latestCode(String email) throws Exception {
        String response = mockMvc.perform(get("/verification/email/test-inbox").param("email", email))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.code");
    }

    private void assertInvalidCode(String email, String code) throws Exception {
        mockMvc.perform(post("/verification/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyRequest(email, code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(INVALID_CODE_TYPE))
                .andExpect(jsonPath("$.status").value(400));
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
}
