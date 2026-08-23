package io.github.ringotangs.ringoboot.autoconfigure.problem;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Ringo Boot Problem Details 自动配置属性。 */
@ConfigurationProperties(ProblemProperties.PREFIX)
public class ProblemProperties {

    /** 配置属性前缀。 */
    public static final String PREFIX = "ringo.boot.problem";

    /** 异常处理器配置属性前缀。 */
    public static final String HANDLERS_PREFIX = PREFIX + ".handlers";

    /**
     * 是否启用整套异常处理自动配置；需要显式开启。
     */
    private boolean enabled;

    /**
     * 是否使用 Spring MessageSource 解析业务问题、验证码和兜底异常的标题与详情。
     */
    private boolean i18n = true;

    /** 各类异常处理器的开关。 */
    private final Handlers handlers = new Handlers();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isI18n() {
        return i18n;
    }

    public void setI18n(boolean i18n) {
        this.i18n = i18n;
    }

    public Handlers getHandlers() {
        return handlers;
    }

    /** 各类异常处理器的配置。 */
    public static class Handlers {

        /** 是否处理应用抛出的 ProblemException。 */
        private boolean application = true;

        /** 是否处理 Spring MVC 内置异常。 */
        private boolean mvc = true;

        /** 是否处理验证码业务和技术异常。 */
        private boolean verification = true;

        /** 是否兜底处理其他未知异常。 */
        private boolean fallback;

        public boolean isApplication() {
            return application;
        }

        public void setApplication(boolean application) {
            this.application = application;
        }

        public boolean isMvc() {
            return mvc;
        }

        public void setMvc(boolean mvc) {
            this.mvc = mvc;
        }

        public boolean isVerification() {
            return verification;
        }

        public void setVerification(boolean verification) {
            this.verification = verification;
        }

        public boolean isFallback() {
            return fallback;
        }

        public void setFallback(boolean fallback) {
            this.fallback = fallback;
        }
    }
}
