package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationProperties;
import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.verification.VerificationException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 自动配置 Ringo Boot Problem Details 异常处理体系。
 *
 * <p>{@code enabled} 是整个自动配置的总开关；总开关开启后，
 * {@code application-enabled}、{@code mvc-enabled} 和 {@code fallback-enabled} 分别控制
 * 业务问题异常、Spring MVC 内置异常与未知异常处理。{@code i18n-enabled} 不会单独
 * 启用处理器，只决定业务问题与兜底处理器是否通过 Spring
 * {@code MessageSource} 解析错误文案。Spring MVC 响应继续使用 Spring 原生消息解析。</p>
 *
 * <p>Auto-configures the Ringo Boot Problem Details exception-handling system.
 * {@code enabled} is the master switch. Once it is enabled,
 * {@code application-enabled}, {@code mvc-enabled}, and {@code fallback-enabled}
 * independently control problem exceptions, built-in Spring MVC exceptions, and
 * fallback handling. {@code i18n-enabled} does not enable a handler by itself; it
 * selects MessageSource-based message resolution for problem and fallback handlers.
 * Spring MVC responses continue to use Spring's native message resolution.</p>
 */
@AutoConfiguration(before = WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ProblemDetail.class, ProblemException.class})
@ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ProblemProperties.class)
public class ProblemAutoConfiguration {

    /** 配置 Spring MVC 内置异常的稳定 Problem Details 映射。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ResponseEntityExceptionHandler.class)
    @ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "mvc-enabled", havingValue = "true")
    static class SpringMvcConfiguration {

        /** 用户提供 ResponseEntityExceptionHandler 时不创建默认 MVC 处理器。 */
        @Bean
        @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
        SpringMvcExceptionHandler springMvcExceptionHandler(
                ApplicationContext applicationContext, ProblemProperties properties) {
            return new SpringMvcExceptionHandler(applicationContext, properties);
        }
    }

    /**
     * 在应用问题异常功能开关开启时装配 ProblemExceptionHandler。
     *
     * <p>Configures ProblemExceptionHandler when application problem handling is enabled.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "application-enabled", havingValue = "true")
    static class ApplicationConfiguration {

        /**
         * 用户提供自定义 ProblemExceptionHandler 时不创建默认实现。
         *
         * <p>Backs off when a custom ProblemExceptionHandler is available.</p>
         */
        @Bean
        @ConditionalOnMissingBean
        ProblemExceptionHandler problemExceptionHandler(ProblemMessageResolver messageResolver) {
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
    @ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "fallback-enabled", havingValue = "true")
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
                ProblemProperties properties) {
            return new FallbackExceptionHandler(messageResolver, applicationContext, properties);
        }
    }

    /**
     * 在验证码功能和应用或兜底异常处理开启时装配验证码技术异常处理器。
     *
     * <p>Configures verification technical exception handling when verification and either
     * application or fallback exception handling are enabled.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(VerificationException.class)
    @Conditional(AnyHandlerEnabledCondition.class)
    @ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
    static class VerificationConfiguration {

        /**
         * 用户提供自定义 VerificationExceptionHandler 时不创建默认实现。
         *
         * <p>Backs off when a custom VerificationExceptionHandler is available.</p>
         */
        @Bean
        @ConditionalOnMissingBean
        VerificationExceptionHandler verificationExceptionHandler(ProblemMessageResolver messageResolver) {
            return new VerificationExceptionHandler(messageResolver);
        }
    }

    /**
     * 装配应用、验证码和兜底异常处理器共享的消息解析器。
     * 只有至少一个处理器开启时才需要该解析器。
     *
     * <p>Configures the message resolver shared by application, verification, and fallback handlers.
     * The resolver is needed only when application or fallback handling is enabled.</p>
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
                ApplicationContext applicationContext, ProblemProperties properties) {
            return properties.isI18nEnabled()
                    ? new MessageSourceProblemMessageResolver(applicationContext)
                    : new DefaultProblemMessageResolver();
        }
    }

    /**
     * 当 Application 或 Fallback 任意一个功能开启时匹配。
     *
     * <p>该条件表达的是逻辑 OR：</p>
     * <pre>{@code application-enabled || fallback-enabled}</pre>
     *
     * <p>Matches when either application problem handling or fallback handling is enabled.</p>
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
         * 仅描述 Application 功能开关条件，不会注册 Bean。
         *
         * <p>Describes the application problem-handler condition without registering a bean.</p>
         */
        @ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "application-enabled", havingValue = "true")
        static class ApplicationEnabled {}

        /**
         * 仅描述 Fallback 功能开关条件，不会注册 Bean。
         *
         * <p>Describes the fallback-handler condition without registering a bean.</p>
         */
        @ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "fallback-enabled", havingValue = "true")
        static class FallbackEnabled {}
    }
}
