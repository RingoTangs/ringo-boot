package io.github.ringotangs.ringoboot.autoconfigure.problem;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ringo Boot Problem Details 自动配置属性。
 *
 * <p>配置前缀为 {@value ProblemConfigurationConstants#PREFIX}。该类绑定总开关和国际化开关，二者默认关闭；
 * 各类异常处理器通过 {@value ProblemConfigurationConstants#HANDLERS_PREFIX} 下的独立配置显式开启。
 */
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

    /**
     * 创建默认关闭的 Problem Details 自动配置属性。
     */
    public ProblemProperties() {}

    /**
     * 返回是否启用 Problem Details 异常处理自动配置。
     *
     * @return 启用时为 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用 Problem Details 异常处理自动配置。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回是否使用 Spring MessageSource 解析异常消息。
     *
     * @return 启用国际化消息解析时为 {@code true}
     */
    public boolean isI18n() {
        return i18n;
    }

    /**
     * 设置是否使用 Spring MessageSource 解析异常消息。
     *
     * @param i18n 是否启用国际化消息解析
     */
    public void setI18n(boolean i18n) {
        this.i18n = i18n;
    }
}
