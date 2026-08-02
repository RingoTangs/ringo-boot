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

    /**
     * 是否使用 Spring MessageSource 解析标题和详情；需要显式开启。
     *
     * <p>Whether to resolve titles and details through Spring MessageSource;
     * explicit opt-in is required.</p>
     */
    private boolean i18nEnabled = false;

    /**
     * 是否启用未知异常兜底处理；需要显式开启。
     *
     * <p>Whether to enable fallback handling for unexpected exceptions;
     * explicit opt-in is required.</p>
     */
    private boolean fallbackEnabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isI18nEnabled() {
        return i18nEnabled;
    }

    public void setI18nEnabled(boolean i18nEnabled) {
        this.i18nEnabled = i18nEnabled;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }
}
