package io.github.ringotangs.springcommons.web.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Commons Problem Details 自动配置属性。
 *
 * <p>Auto-configuration properties for Spring Commons Problem Details.</p>
 */
@ConfigurationProperties(ProblemDetailsProperties.PREFIX)
public class ProblemDetailsProperties {

    /** 配置属性前缀。 */
    public static final String PREFIX = "ringotangs.spring-commons.problem-details";

    /** 是否启用 Problem Details 异常处理。 */
    private boolean enabled = true;

    /** 国际化配置。 */
    private final Internationalization internationalization = new Internationalization();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Internationalization getI18n() {
        return internationalization;
    }

    /** Problem Details 国际化配置。 */
    public static class Internationalization {

        /** 是否使用 Spring MessageSource 解析标题和详情。 */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
