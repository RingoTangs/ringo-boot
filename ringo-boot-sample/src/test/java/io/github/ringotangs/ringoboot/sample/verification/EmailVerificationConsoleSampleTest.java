package io.github.ringotangs.ringoboot.sample.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class EmailVerificationConsoleSampleTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void defaultSampleConfigurationDeliversCodeToConsole(CapturedOutput output) throws Exception {
        String email = UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/verification/email/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isAccepted());

        String logs = output.getAll();
        assertThat(logs)
                .contains("DEVELOPMENT ONLY", "purpose=email-verification", email.charAt(0) + "***@example.com")
                .doesNotContain(email)
                .containsPattern("code=\\d{6}");
    }
}
