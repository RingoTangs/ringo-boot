package io.github.ringotangs.ringoboot.autoconfigure.verification;

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

    /** 邮件验证码渠道配置。 / Email verification channel settings. */
    private final Channel email = new Channel();

    /** 短信验证码渠道配置。 / SMS verification channel settings. */
    private final Channel sms = new Channel();

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

    public Channel getEmail() {
        return email;
    }

    public Channel getSms() {
        return sms;
    }

    /**
     * 验证码渠道的开发辅助配置。
     *
     * <p>Development support settings for a verification channel.</p>
     */
    public static class Channel {

        /** 是否启用会输出明文验证码的控制台发送器。 / Whether to enable the console sender that outputs plaintext codes. */
        private boolean consoleEnabled;

        public boolean isConsoleEnabled() {
            return consoleEnabled;
        }

        public void setConsoleEnabled(boolean consoleEnabled) {
            this.consoleEnabled = consoleEnabled;
        }
    }
}
