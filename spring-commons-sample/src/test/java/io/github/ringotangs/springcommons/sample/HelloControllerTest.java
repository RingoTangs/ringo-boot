package io.github.ringotangs.springcommons.sample;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(HelloControllerTest.FailingController.class)
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
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("""
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
                """, "name", "NotBlank");
    }

    @Test
    void returnsProblemWhenUserAgeIsMissing() throws Exception {
        assertValidationProblem("""
                {
                  "name": "Alice"
                }
                """, "age", "NotNull");
    }

    @Test
    void returnsProblemWhenUserAgeIsOutOfRange() throws Exception {
        assertValidationProblem("""
                {
                  "name": "Alice",
                  "age": 151
                }
                """, "age", "Max");
    }

    @Test
    void returnsProblemWithMethodParameterValidationError() throws Exception {
        mockMvc.perform(get("/validated-user").param("id", "0").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:mvc:validation-failed"))
                .andExpect(jsonPath("$.errors[0].field").value("id"))
                .andExpect(jsonPath("$.errors[0].code").value("Min"))
                .andExpect(jsonPath("$.errors[0].message").value("must be greater than or equal to 1"));
    }

    @Test
    void returnsBadRequestProblemForInvalidUserId() throws Exception {
        mockMvc.perform(get("/user").param("id", "0").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:business:user:invalid-id"))
                .andExpect(jsonPath("$.title").value("Invalid user id"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("User id must be greater than 0"))
                .andExpect(jsonPath("$.instance").value("/user"))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void returnsSpringDiagnosticForInvalidParameterType() throws Exception {
        mockMvc.perform(get("/user").param("id", "secret-invalid-value").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:mvc:invalid-parameter"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Failed to convert 'id' with value: 'secret-invalid-value'"))
                .andExpect(jsonPath("$.instance").value("/user"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("secret-invalid-value")));
    }

    @Test
    void returnsStableProblemAndAllowHeaderForUnsupportedMethod() throws Exception {
        mockMvc.perform(post("/user").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, "GET"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:mvc:method-not-allowed"))
                .andExpect(jsonPath("$.title").value("Method Not Allowed"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.detail").value("Method 'POST' is not supported."))
                .andExpect(jsonPath("$.instance").value("/user"));
    }

    @Test
    void usesSpringNativeProblemForChineseRequest() throws Exception {
        mockMvc.perform(get("/user").param("id", "invalid").header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:mvc:invalid-parameter"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Failed to convert 'id' with value: 'invalid'"));
    }

    @Test
    void returnsStableProblemForMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:mvc:malformed-request"))
                .andExpect(jsonPath("$.instance").value("/users"));
    }

    @Test
    void returnsStableProblemForUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .content("name=Alice"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value("urn:problem:mvc:unsupported-media-type"))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"));
    }

    @Test
    void returnsStableProblemWhenResourceDoesNotExist() throws Exception {
        mockMvc.perform(get("/does-not-exist").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:mvc:not-found"))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void returnsNotFoundProblemWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/user").param("id", "2").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:business:user:not-found"))
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("User 2 does not exist"))
                .andExpect(jsonPath("$.instance").value("/user"))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void returnsLocalizedProblemForChineseRequest() throws Exception {
        mockMvc.perform(get("/user").param("id", "2").header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:business:user:not-found"))
                .andExpect(jsonPath("$.title").value("未找到用户"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("用户 2 不存在"))
                .andExpect(jsonPath("$.instance").value("/user"));
    }

    @Test
    void returnsSafeProblemForUnexpectedException() throws Exception {
        mockMvc.perform(get("/test/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:fallback:internal-server-error"))
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.instance").value("/test/failure"))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("sample-secret"))));
    }

    private void assertValidationProblem(String requestBody, String field, String code) throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:mvc:validation-failed"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/users"))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].code").value(code))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @RestController
    static class FailingController {

        @GetMapping("/test/failure")
        void fail() {
            throw new IllegalStateException("sample-secret");
        }
    }
}
