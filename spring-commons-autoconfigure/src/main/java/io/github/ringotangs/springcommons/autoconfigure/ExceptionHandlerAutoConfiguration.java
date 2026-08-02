package io.github.ringotangs.springcommons.autoconfigure;

import io.github.ringotangs.springcommons.core.ProblemException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

/**
 * 自动配置 Spring Commons Web 异常处理体系。
 *
 * <p>{@code enabled} 是整个自动配置的总开关；总开关开启后，
 * {@code problem-enabled} 和 {@code fallback-enabled} 分别控制业务问题异常处理与
 * 未知异常兜底处理。{@code i18n-enabled} 不会单独启用处理器，只决定已启用的处理器
 * 是否通过 Spring {@code MessageSource} 解析错误文案。</p>
 *
 * <p>Auto-configures the Spring Commons Web exception-handling system.
 * {@code enabled} is the master switch. Once it is enabled,
 * {@code problem-enabled} and {@code fallback-enabled} independently control
 * problem-exception handling and fallback handling. {@code i18n-enabled} does
 * not enable a handler by itself; it only selects MessageSource-based message
 * resolution for enabled handlers.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass({ProblemDetail.class, ProblemException.class})
@ConditionalOnProperty(
        prefix = ExceptionHandlerProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
@EnableConfigurationProperties(ExceptionHandlerProperties.class)
public class ExceptionHandlerAutoConfiguration {

    /**
     * 在 Problem 功能开关开启时装配 ProblemExceptionHandler。
     *
     * <p>Configures ProblemExceptionHandler when problem handling is enabled.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = ExceptionHandlerProperties.PREFIX,
            name = "problem-enabled",
            havingValue = "true"
    )
    static class ProblemConfiguration {

        /**
         * 用户提供自定义 ProblemExceptionHandler 时不创建默认实现。
         *
         * <p>Backs off when a custom ProblemExceptionHandler is available.</p>
         */
        @Bean
        @ConditionalOnMissingBean
        ProblemExceptionHandler problemExceptionHandler(
                ProblemMessageResolver messageResolver
        ) {
            return new ProblemExceptionHandler(messageResolver);
        }
    }

    /**
     * 在兜底功能开关开启时独立装配 FallbackExceptionHandler。
     *
     * <p>Independently configures FallbackExceptionHandler when fallback handling
     * is enabled.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = ExceptionHandlerProperties.PREFIX,
            name = "fallback-enabled",
            havingValue = "true"
    )
    static class FallbackConfiguration {

        /**
         * 用户提供自定义 FallbackExceptionHandler 时不创建默认实现。
         *
         * <p>Backs off when a custom FallbackExceptionHandler is available.</p>
         */
        @Bean
        @ConditionalOnMissingBean
        FallbackExceptionHandler fallbackExceptionHandler(
                ProblemMessageResolver messageResolver,
                ApplicationContext applicationContext,
                ExceptionHandlerProperties properties
        ) {
            return new FallbackExceptionHandler(
                    messageResolver,
                    applicationContext,
                    properties
            );
        }
    }

    /**
     * 装配两个异常处理器共享的消息解析器。
     * 只有至少一个处理器开启时才需要该解析器。
     *
     * <p>Configures the message resolver shared by both handlers. The resolver is
     * needed only when at least one handler is enabled.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @Conditional(AnyHandlerEnabledCondition.class)
    static class ProblemMessageResolverConfiguration {

        /**
         * 根据 i18n 开关选择国际化或默认消息解析器，并允许用户覆盖默认 Bean。
         *
         * <p>Selects the localized or default resolver and backs off for a custom
         * resolver.</p>
         */
        @Bean
        @ConditionalOnMissingBean
        ProblemMessageResolver problemMessageResolver(
                ApplicationContext applicationContext,
                ExceptionHandlerProperties properties
        ) {
            return properties.isI18nEnabled()
                    ? new MessageSourceProblemMessageResolver(applicationContext)
                    : new DefaultProblemMessageResolver();
        }
    }

    /**
     * 当 Problem 或 Fallback 任意一个功能开启时匹配。
     *
     * <p>该条件表达的是逻辑 OR：</p>
     * <pre>{@code problem-enabled || fallback-enabled}</pre>
     *
     * <p>Matches when either problem handling or fallback handling is enabled.</p>
     */
    static final class AnyHandlerEnabledCondition extends AnyNestedCondition {

        /**
         * 在解析配置类阶段计算属性条件，决定是否注册消息解析器配置。
         *
         * <p>Evaluates the property conditions while configuration classes are
         * being parsed.</p>
         */
        AnyHandlerEnabledCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        /**
         * 仅描述 Problem 功能开关条件，不会注册 Bean。
         *
         * <p>Describes the problem-handler condition without registering a bean.</p>
         */
        @ConditionalOnProperty(
                prefix = ExceptionHandlerProperties.PREFIX,
                name = "problem-enabled",
                havingValue = "true"
        )
        static class ProblemEnabled {
        }

        /**
         * 仅描述 Fallback 功能开关条件，不会注册 Bean。
         *
         * <p>Describes the fallback-handler condition without registering a bean.</p>
         */
        @ConditionalOnProperty(
                prefix = ExceptionHandlerProperties.PREFIX,
                name = "fallback-enabled",
                havingValue = "true"
        )
        static class FallbackEnabled {
        }
    }
}
