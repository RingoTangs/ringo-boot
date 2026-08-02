package io.github.ringotangs.springcommons.sample;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void createsUserWhenRequestIsValid() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice",
                                  "age": 20
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.age").value(20));
    }

    @Test
    void returnsProblemWhenUserNameIsBlank() throws Exception {
        assertValidationProblem("""
                {
                  "name": " ",
                  "age": 20
                }
                """);
    }

    @Test
    void returnsProblemWhenUserAgeIsMissing() throws Exception {
        assertValidationProblem("""
                {
                  "name": "Alice"
                }
                """);
    }

    @Test
    void returnsProblemWhenUserAgeIsOutOfRange() throws Exception {
        assertValidationProblem("""
                {
                  "name": "Alice",
                  "age": 151
                }
                """);
    }

    @Test
    void returnsBadRequestProblemForInvalidUserId() throws Exception {
        mockMvc.perform(get("/user")
                        .param("id", "0")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
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
        mockMvc.perform(get("/user")
                        .param("id", "2")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:spring-commons:user:not-found"))
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("User 2 does not exist"))
                .andExpect(jsonPath("$.instance").value("/user"))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void returnsLocalizedProblemForChineseRequest() throws Exception {
        mockMvc.perform(get("/user")
                        .param("id", "2")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:problem:spring-commons:user:not-found"))
                .andExpect(jsonPath("$.title").value("未找到用户"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("用户 2 不存在"))
                .andExpect(jsonPath("$.instance").value("/user"));
    }

    private void assertValidationProblem(String requestBody) throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/users"));
    }
}
