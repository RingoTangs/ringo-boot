package io.github.ringotangs.ringoboot.problem.autoconfigure;

/**
 * Problem Details 配置属性名称。
 */
public final class ProblemProperties {

    /**
     * Spring MVC Problem Details 总开关。
     */
    public static final String ENABLED_PROPERTY = "spring.mvc.problemdetails.enabled";

    /**
     * Problem Details 配置属性前缀。
     */
    public static final String PREFIX = "ringo.boot.problem";

    /**
     * Problem Details 异常处理器配置属性前缀。
     */
    public static final String HANDLERS_PREFIX = PREFIX + ".handlers";

    private ProblemProperties() {}
}
