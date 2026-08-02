package io.github.ringotangs.springcommons.sample;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsUserWhenUserExists() throws Exception {
        mockMvc.perform(get("/user").param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("zs"))
                .andExpect(jsonPath("$.age").value(18));
    }

    @Test
    void returnsBadRequestProblemForInvalidUserId() throws Exception {
        mockMvc.perform(get("/user").param("id", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:spring-commons:user:invalid-id"))
                .andExpect(jsonPath("$.title").value("Invalid user id"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("User id must be greater than 0"))
                .andExpect(jsonPath("$.instance").value("/user"))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void returnsNotFoundProblemWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/user").param("id", "2"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:spring-commons:user:not-found"))
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("The requested user does not exist"))
                .andExpect(jsonPath("$.instance").value("/user"))
                .andExpect(jsonPath("$.code").doesNotExist());
    }
}
