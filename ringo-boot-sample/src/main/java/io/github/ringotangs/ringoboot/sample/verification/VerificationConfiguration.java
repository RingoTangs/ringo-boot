package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.verification.DefaultVerificationService;
import io.github.ringotangs.ringoboot.verification.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.NumericCodeGenerator;
import io.github.ringotangs.ringoboot.verification.VerificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class VerificationConfiguration {

    @Bean
    VerificationService verificationService() {
        return new DefaultVerificationService(new NumericCodeGenerator(), new InMemoryVerificationStore());
    }

    @Bean
    InMemoryEmailCodeSender emailCodeSender() {
        return new InMemoryEmailCodeSender();
    }
}
