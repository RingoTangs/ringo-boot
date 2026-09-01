package io.github.ringotangs.ringoboot.problem.autoconfigure;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.problem.message.DefaultProblemMessageResolver;
import io.github.ringotangs.ringoboot.problem.message.MessageSourceProblemMessageResolver;
import io.github.ringotangs.ringoboot.problem.message.ProblemMessageResolver;
import io.github.ringotangs.ringoboot.problem.web.FallbackExceptionHandler;
import io.github.ringotangs.ringoboot.problem.web.ProblemExceptionHandler;
import io.github.ringotangs.ringoboot.problem.web.SpringMvcExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 自动配置 Ringo Boot Problem Details 异常处理体系。
 *
 * <p>{@code enabled} 是总开关。业务问题、Spring MVC、验证码和未知异常处理均默认关闭，
 * 需要通过 {@code handlers} 分组分别开启。{@code i18n} 决定是否通过 Spring {@code MessageSource} 解析错误文案。</p>
 */
@AutoConfiguration(before = WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ProblemDetail.class, ProblemException.class})
@ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ProblemProperties.class)
public class ProblemAutoConfiguration {

    /**
     * 配置 Spring MVC 内置异常的稳定 Problem Details 映射。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ResponseEntityExceptionHandler.class)
    @ConditionalOnProperty(prefix = ProblemProperties.HANDLERS_PREFIX, name = "mvc", havingValue = "true")
    static class SpringMvcConfiguration {

        /**
         * 用户提供 ResponseEntityExceptionHandler 时不创建默认 MVC 处理器。
         */
        @Bean
        @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
        SpringMvcExceptionHandler springMvcExceptionHandler(ApplicationContext applicationContext) {
            return new SpringMvcExceptionHandler(applicationContext);
        }
    }

    /**
     * 在业务问题异常处理开关开启时装配处理器。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = ProblemProperties.HANDLERS_PREFIX, name = "application", havingValue = "true")
    static class ProblemExceptionHandlerConfiguration {

        /**
         * 用户提供自定义处理器时不创建默认实现。
         */
        @Bean
        @ConditionalOnMissingBean
        ProblemExceptionHandler problemExceptionHandler(ProblemMessageResolver messageResolver) {
            return new ProblemExceptionHandler(messageResolver);
        }
    }

    /**
     * 在未知异常兜底开关开启时装配处理器。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = ProblemProperties.HANDLERS_PREFIX, name = "fallback", havingValue = "true")
    static class FallbackConfiguration {

        /**
         * 用户提供自定义处理器时不创建默认实现。
         */
        @Bean
        @ConditionalOnMissingBean
        FallbackExceptionHandler fallbackExceptionHandler(
                ProblemMessageResolver messageResolver,
                ApplicationContext applicationContext,
                ProblemProperties properties) {
            return new FallbackExceptionHandler(messageResolver, applicationContext, properties.isI18n());
        }
    }

    /**
     * 装配应用、验证码和兜底异常处理器共享的消息解析器。
     */
    @Configuration(proxyBeanMethods = false)
    static class ProblemMessageResolverConfiguration {

        /**
         * 根据国际化开关选择消息解析器，用户可以提供自定义 Bean。
         */
        @Bean
        @ConditionalOnMissingBean
        ProblemMessageResolver problemMessageResolver(
                ApplicationContext applicationContext, ProblemProperties properties) {
            return properties.isI18n()
                    ? new MessageSourceProblemMessageResolver(applicationContext)
                    : new DefaultProblemMessageResolver();
        }
    }
}
