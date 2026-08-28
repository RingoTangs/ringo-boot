package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationHmacKey;
import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 为示例应用配置验证码 HMAC 密钥。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "store", havingValue = "redis")
class VerificationHmacKeyConfiguration {

    /**
     * 从环境变量读取 Base64 编码的 HMAC 密钥。
     */
    @Bean
    VerificationHmacKey verificationHmacKey(Environment environment) {
        String encoded = environment.getProperty("VERIFICATION_HMAC_KEY");
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("VERIFICATION_HMAC_KEY must be configured");
        }
        return VerificationHmacKey.fromBase64(encoded);
    }
}
