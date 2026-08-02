package io.github.ringotangs.springcommons.autoconfigure;

import io.github.ringotangs.springcommons.core.ProblemException;
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

/** 自动配置 Spring Commons Web ProblemExceptionHandler。 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass({ProblemDetail.class, ProblemException.class})
@ConditionalOnProperty(
        prefix = ProblemExceptionHandlerProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(ProblemExceptionHandlerProperties.class)
public class ProblemExceptionHandlerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ProblemExceptionHandler problemExceptionHandler(ProblemMessageResolver messageResolver) {
        return new ProblemExceptionHandler(messageResolver);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = ProblemExceptionHandlerProperties.PREFIX + ".i18n",
            name = "enabled",
            havingValue = "true"
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
            prefix = ProblemExceptionHandlerProperties.PREFIX + ".i18n",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    static class DefaultMessagesConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ProblemMessageResolver problemMessageResolver() {
            return new DefaultProblemMessageResolver();
        }
    }
}
