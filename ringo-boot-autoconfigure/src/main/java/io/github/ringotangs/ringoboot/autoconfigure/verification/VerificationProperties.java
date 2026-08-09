package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ringo Boot 验证码自动配置属性。
 *
 * <p>Auto-configuration properties for Ringo Boot verification services.</p>
 */
@ConfigurationProperties(VerificationProperties.PREFIX)
public class VerificationProperties {

    /** 配置属性前缀。 / Configuration property prefix. */
    public static final String PREFIX = "ringo.boot.verification";

    /** 是否启用验证码自动配置。 / Whether verification auto-configuration is enabled. */
    private boolean enabled;

    /** 默认验证码长度。 / Default verification code length. */
    private int length = 6;

    /** 默认验证码有效期。 / Default verification code time to live. */
    private Duration ttl = Duration.ofMinutes(5);

    /** 默认最大校验尝试次数。 / Default maximum number of verification attempts. */
    private int maxAttempts = 5;

    /** 默认重发间隔。 / Default resend interval. */
    private Duration resendInterval = Duration.ofSeconds(60);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getResendInterval() {
        return resendInterval;
    }

    public void setResendInterval(Duration resendInterval) {
        this.resendInterval = resendInterval;
    }

    VerificationPolicy toPolicy() {
        return new VerificationPolicy(length, ttl, maxAttempts, resendInterval);
    }
}
