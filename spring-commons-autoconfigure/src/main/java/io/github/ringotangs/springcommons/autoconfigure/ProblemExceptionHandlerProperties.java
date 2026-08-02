package io.github.ringotangs.springcommons.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Commons Web 异常处理器自动配置属性。
 *
 * <p>Auto-configuration properties for the Spring Commons Web exception handler.</p>
 */
@ConfigurationProperties(ProblemExceptionHandlerProperties.PREFIX)
public class ProblemExceptionHandlerProperties {

    /** 配置属性前缀。 */
    public static final String PREFIX = "ringotangs.spring-commons.web.exception-handler";

    /**
     * 是否启用 ProblemExceptionHandler；需要显式开启。
     *
     * <p>Whether to enable ProblemExceptionHandler; explicit opt-in is required.</p>
     */
    private boolean enabled = false;

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

    /** Problem Details 消息国际化配置。 */
    public static class Internationalization {

        /**
         * 是否使用 Spring MessageSource 解析标题和详情；需要显式开启。
         *
         * <p>Whether to resolve titles and details through Spring MessageSource;
         * explicit opt-in is required.</p>
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
