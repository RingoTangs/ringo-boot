package io.github.ringotangs.ringoboot.autoconfigure.problem;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Ringo Boot Problem Details 自动配置属性。 */
@ConfigurationProperties(ProblemConfigurationConstants.PREFIX)
public class ProblemProperties {

    /**
     * 是否启用整套异常处理自动配置；需要显式开启。
     */
    private boolean enabled;

    /**
     * 是否使用 Spring MessageSource 解析业务问题、验证码和兜底异常的标题与详情；默认关闭。
     */
    private boolean i18n;

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
}
