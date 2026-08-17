package io.github.ringotangs.ringoboot.autoconfigure.verification;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 验证码签发频率限制的自动配置属性。 */
@ConfigurationProperties(IssueRateLimitProperties.PREFIX)
public class IssueRateLimitProperties {

    /** 配置属性前缀。 */
    public static final String PREFIX = VerificationProperties.PREFIX + ".issue-rate-limit";

    /** 同一验证码键的最小签发间隔，默认为 60 秒；设置为零可显式关闭限制。 */
    private Duration interval = Duration.ofSeconds(60);

    /**
     * 返回同一验证码键的最小签发间隔。
     *
     * @return 最小签发间隔
     */
    public Duration getInterval() {
        return interval;
    }

    /**
     * 设置同一验证码键的最小签发间隔。
     *
     * @param interval 最小签发间隔，不得为负数
     */
    public void setInterval(Duration interval) {
        this.interval = interval;
    }
}
