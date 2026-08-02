package io.github.ringotangs.springcommons.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Commons Web 异常处理器自动配置属性。
 *
 * <p>Auto-configuration properties for the Spring Commons Web exception handler.</p>
 */
@ConfigurationProperties(ExceptionHandlerProperties.PREFIX)
public class ExceptionHandlerProperties {

    /** 配置属性前缀。 */
    public static final String PREFIX = "ringotangs.spring-commons.web.exception-handler";

    /**
     * 是否启用整套异常处理自动配置；需要显式开启。
     *
     * <p>Whether to enable the complete exception-handling auto-configuration;
     * explicit opt-in is required.</p>
     */
    private boolean enabled = false;

    /**
     * 是否启用 ProblemException 异常处理；仅在总开关开启后生效。
     *
     * <p>Whether to enable ProblemException handling; effective only when the
     * main switch is enabled.</p>
     */
    private boolean problemEnabled = false;

    /**
     * 是否启用 Spring MVC 内置异常处理；仅在总开关开启后生效。
     *
     * <p>Whether to handle built-in Spring MVC exceptions; effective only when the
     * main switch is enabled.</p>
     */
    private boolean mvcEnabled = false;

    /**
     * 是否使用 Spring MessageSource 解析标题和详情；仅在总开关开启后生效。
     *
     * <p>Whether to resolve titles and details through Spring MessageSource;
     * effective only when the main switch is enabled.</p>
     */
    private boolean i18nEnabled = false;

    /**
     * 是否启用未知异常兜底处理；仅在总开关开启后生效。
     *
     * <p>Whether to enable fallback handling for unexpected exceptions;
     * effective only when the main switch is enabled.</p>
     */
    private boolean fallbackEnabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isProblemEnabled() {
        return problemEnabled;
    }

    public void setProblemEnabled(boolean problemEnabled) {
        this.problemEnabled = problemEnabled;
    }

    public boolean isMvcEnabled() {
        return mvcEnabled;
    }

    public void setMvcEnabled(boolean mvcEnabled) {
        this.mvcEnabled = mvcEnabled;
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
