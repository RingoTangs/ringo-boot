package io.github.ringotangs.springcommons.web.autoconfigure;

import io.github.ringotangs.springcommons.core.ProblemException;
import io.github.ringotangs.springcommons.web.ProblemExceptionHandler;
import io.github.ringotangs.springcommons.web.ProblemMessageResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

/** 自动配置 Spring Commons Problem Details 异常处理。 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass({ProblemDetail.class, ProblemException.class})
@ConditionalOnProperty(
        prefix = ProblemDetailsProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(ProblemDetailsProperties.class)
public class ProblemDetailsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ProblemExceptionHandler problemExceptionHandler(ProblemMessageResolver messageResolver) {
        return new ProblemExceptionHandler(messageResolver);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = ProblemDetailsProperties.PREFIX + ".i18n",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    static class InternationalizationConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ProblemMessageResolver problemMessageResolver(ApplicationContext applicationContext) {
            return new MessageSourceProblemMessageResolver(applicationContext);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = ProblemDetailsProperties.PREFIX + ".i18n",
            name = "enabled",
            havingValue = "false"
    )
    static class DefaultMessagesConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ProblemMessageResolver problemMessageResolver() {
            return new DefaultProblemMessageResolver();
        }
    }
}
