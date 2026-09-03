package io.github.ringotangs.ringoboot.problem.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ringo Boot Problem Details 自动配置属性。
 *
 * <p>配置前缀为 {@value #PREFIX}。该类绑定默认关闭的总开关；各类异常处理器通过
 * {@value #HANDLERS_PREFIX} 下的独立配置显式开启。
 */
@ConfigurationProperties(ProblemProperties.PREFIX)
public class ProblemProperties {

    /**
     * Problem Details 配置属性前缀。
     */
    public static final String PREFIX = "ringo.boot.problem";

    /**
     * Problem Details 异常处理器配置属性前缀。
     */
    public static final String HANDLERS_PREFIX = PREFIX + ".handlers";

    /**
     * 是否启用整套异常处理自动配置；需要显式开启。
     */
    private boolean enabled;

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
}
