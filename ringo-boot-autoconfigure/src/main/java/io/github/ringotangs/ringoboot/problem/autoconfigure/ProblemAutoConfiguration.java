package io.github.ringotangs.ringoboot.problem.autoconfigure;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.problem.web.FallbackExceptionHandler;
import io.github.ringotangs.ringoboot.problem.web.ProblemExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

/**
 * 自动配置 Ringo Boot Problem Details 异常处理体系。
 *
 * <p>{@code spring.mvc.problemdetails.enabled} 是总开关。业务问题和未知异常处理均默认关闭，需要通过
 * {@code handlers} 分组分别开启。</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ProblemDetail.class, ProblemException.class})
@ConditionalOnBooleanProperty("spring.mvc.problemdetails.enabled")
public class ProblemAutoConfiguration {

    /**
     * 在业务问题异常处理开关开启时装配处理器。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = ProblemPropertyNames.HANDLERS_PREFIX, name = "application", havingValue = "true")
    static class ProblemExceptionHandlerConfiguration {

        /**
         * 用户提供自定义处理器时不创建默认实现。
         */
        @Bean
        @ConditionalOnMissingBean
        ProblemExceptionHandler problemExceptionHandler() {
            return new ProblemExceptionHandler();
        }
    }

    /**
     * 在未知异常兜底开关开启时装配处理器。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = ProblemPropertyNames.HANDLERS_PREFIX, name = "fallback", havingValue = "true")
    static class FallbackConfiguration {

        /**
         * 用户提供自定义处理器时不创建默认实现。
         */
        @Bean
        @ConditionalOnMissingBean
        FallbackExceptionHandler fallbackExceptionHandler() {
            return new FallbackExceptionHandler();
        }
    }
}
