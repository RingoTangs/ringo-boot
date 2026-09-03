package io.github.ringotangs.ringoboot.sample;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "ringo.boot.verification.store=memory")
@AutoConfigureMockMvc
class ProblemExceptionHandlerDefaultMessageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsDefaultMessagesForChineseRequest() throws Exception {
        mockMvc.perform(get("/user").param("id", "2").header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.detail").value("User 2 does not exist"));
    }

    @Test
    void springMvcUsesItsNativeMessages() throws Exception {
        mockMvc.perform(get("/user").param("id", "invalid").header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:mvc:invalid-parameter"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Failed to convert 'id' with value: 'invalid'"));
    }
}
