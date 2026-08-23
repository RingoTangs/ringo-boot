package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.problem.ProblemException;
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
 * {@code application-enabled}、{@code mvc-enabled}、{@code verification-enabled} 和
 * {@code fallback-enabled} 分别控制业务问题异常、Spring MVC 内置异常、验证码技术异常与未知异常处理。{@code i18n-enabled} 不会单独
 * 启用处理器，只决定业务问题与兜底处理器是否通过 Spring
 * {@code MessageSource} 解析错误文案。Spring MVC 响应继续使用 Spring 原生消息解析。</p>
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

    /** 在业务问题异常处理开关开启时装配处理器。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "application-enabled", havingValue = "true")
    static class ApplicationConfiguration {

        /** 用户提供自定义处理器时不创建默认实现。 */
        @Bean
        @ConditionalOnMissingBean
        ProblemExceptionHandler problemExceptionHandler(ProblemMessageResolver messageResolver) {
            return new ProblemExceptionHandler(messageResolver);
        }
    }

    /** 在未知异常兜底开关开启时装配处理器。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "fallback-enabled", havingValue = "true")
    static class FallbackConfiguration {

        /** 用户提供自定义处理器时不创建默认实现。 */
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
     * 装配应用、验证码和兜底异常处理器共享的消息解析器。
     * 只有至少一个处理器开启时才需要该解析器。
     */
    @Configuration(proxyBeanMethods = false)
    @Conditional(AnyHandlerEnabledCondition.class)
    static class ProblemMessageResolverConfiguration {

        /** 根据国际化开关选择消息解析器，用户可以提供自定义 Bean。 */
        @Bean
        @ConditionalOnMissingBean
        ProblemMessageResolver problemMessageResolver(
                ApplicationContext applicationContext, ProblemProperties properties) {
            return properties.isI18nEnabled()
                    ? new MessageSourceProblemMessageResolver(applicationContext)
                    : new DefaultProblemMessageResolver();
        }
    }

    /** 当业务问题、验证码异常或未知异常处理任意一个开关开启时匹配。 */
    static final class AnyHandlerEnabledCondition extends AnyNestedCondition {

        /** 在解析配置类时判断是否需要注册消息解析器。 */
        AnyHandlerEnabledCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        /** 业务问题异常处理开关条件。 */
        @ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "application-enabled", havingValue = "true")
        static class ApplicationEnabled {}

        /** 验证码异常处理开关条件。 */
        @ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "verification-enabled", havingValue = "true")
        static class VerificationEnabled {}

        /** 未知异常兜底处理开关条件。 */
        @ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "fallback-enabled", havingValue = "true")
        static class FallbackEnabled {}
    }
}
