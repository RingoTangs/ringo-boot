package io.github.ringotangs.springcommons.sample;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ringotangs.spring-commons.web.exception-handler.i18n.enabled=false"
})
@AutoConfigureMockMvc
class ProblemExceptionHandlerI18nDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsDefaultMessagesForChineseRequest() throws Exception {
        mockMvc.perform(get("/user")
                        .param("id", "2")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.detail").value("User 2 does not exist"));
    }
}
