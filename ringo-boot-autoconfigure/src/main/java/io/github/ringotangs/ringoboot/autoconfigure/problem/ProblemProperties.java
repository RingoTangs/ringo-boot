package io.github.ringotangs.ringoboot.autoconfigure.problem;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Ringo Boot Problem Details 自动配置属性。 */
@ConfigurationProperties(ProblemProperties.PREFIX)
public class ProblemProperties {

    /** 配置属性前缀。 */
    public static final String PREFIX = "ringo.boot.problem";

    /**
     * 是否启用整套异常处理自动配置；需要显式开启。
     */
    private boolean enabled = false;

    /**
     * 是否启用 ProblemException 异常处理；仅在总开关开启后生效。
     */
    private boolean applicationEnabled = false;

    /**
     * 是否启用 Spring MVC 内置异常处理；仅在总开关开启后生效。
     */
    private boolean mvcEnabled = false;

    /**
     * 是否启用验证码技术异常处理；仅在总开关和验证码功能均开启后生效。
     */
    private boolean verificationEnabled = false;

    /**
     * 是否使用 Spring MessageSource 解析业务异常和兜底异常的标题与详情；
     * Spring MVC 内置异常始终使用 Spring 原生的消息解析机制。
     */
    private boolean i18nEnabled = false;

    /**
     * 是否启用未知异常兜底处理；仅在总开关开启后生效。
     */
    private boolean fallbackEnabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isApplicationEnabled() {
        return applicationEnabled;
    }

    public void setApplicationEnabled(boolean applicationEnabled) {
        this.applicationEnabled = applicationEnabled;
    }

    public boolean isMvcEnabled() {
        return mvcEnabled;
    }

    public void setMvcEnabled(boolean mvcEnabled) {
        this.mvcEnabled = mvcEnabled;
    }

    public boolean isVerificationEnabled() {
        return verificationEnabled;
    }

    public void setVerificationEnabled(boolean verificationEnabled) {
        this.verificationEnabled = verificationEnabled;
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
