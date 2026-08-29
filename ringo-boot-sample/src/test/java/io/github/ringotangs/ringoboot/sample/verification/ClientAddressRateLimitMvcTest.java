package io.github.ringotangs.ringoboot.sample.verification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ringotangs.ringoboot.autoconfigure.verification.ClientAddressIssueQuotaRule;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitManager;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
@Import(ClientAddressRateLimitMvcTest.RateLimitTestConfiguration.class)
class ClientAddressRateLimitMvcTest {

    private static final String CLIENT_ADDRESS = "198.51.100.44";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void throttlesDifferentSubjectsFromSameClientAddress() throws Exception {
        issue().andExpect(status().isAccepted());
        issue().andExpect(status().isAccepted());
        issue().andExpect(status().isTooManyRequests()).andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    private org.springframework.test.web.servlet.ResultActions issue() throws Exception {
        return mockMvc.perform(post("/verification/email/code")
                .with(request -> {
                    request.setRemoteAddr(CLIENT_ADDRESS);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s@example.com"}
                        """.formatted(UUID.randomUUID())));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RateLimitTestConfiguration {

        @Bean
        ClientAddressIssueQuotaRule mvcClientAddressRule() {
            return ClientAddressIssueQuotaRule.builder()
                    .id("mvc-client-address-limit")
                    .namespace("account")
                    .purpose("email-verification")
                    .channel(VerificationChannel.EMAIL)
                    .maxIssues(2)
                    .window(Duration.ofMinutes(10))
                    .build();
        }

        @Bean
        IssueRateLimiter mvcIssueRateLimiter(
                @Qualifier("mvcClientAddressRule") ClientAddressIssueQuotaRule mvcClientAddressRule) {
            return new IssueRateLimitManager(List.of(mvcClientAddressRule), new InMemoryIssueRateLimitStore());
        }
    }
}
