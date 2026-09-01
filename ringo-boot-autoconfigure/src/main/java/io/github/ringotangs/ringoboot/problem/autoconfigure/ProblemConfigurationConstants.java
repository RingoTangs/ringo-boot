package io.github.ringotangs.ringoboot.problem.autoconfigure;

/**
 * 定义 Problem Details 自动配置共享的配置属性前缀。
 *
 * <p>自动配置类通过这些常量引用配置路径，避免在不同功能中重复声明字符串。
 */
public final class ProblemConfigurationConstants {

    /**
     * Problem Details 配置属性前缀：{@value}。
     */
    public static final String PREFIX = "ringo.boot.problem";

    /**
     * Problem Details 异常处理器配置属性前缀：{@value}。
     */
    public static final String HANDLERS_PREFIX = PREFIX + ".handlers";

    /**
     * 防止实例化常量类。
     */
    private ProblemConfigurationConstants() {}
}
