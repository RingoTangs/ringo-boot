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

/** 自动配置 Spring Commons Web 异常处理器。 */
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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = ExceptionHandlerProperties.PREFIX,
            name = "problem-enabled",
            havingValue = "true"
    )
    static class ProblemConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ProblemExceptionHandler problemExceptionHandler(
                ProblemDetailFactory problemDetailFactory
        ) {
            return new ProblemExceptionHandler(problemDetailFactory);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = ExceptionHandlerProperties.PREFIX,
            name = "fallback-enabled",
            havingValue = "true"
    )
    static class FallbackConfiguration {

        @Bean
        @ConditionalOnMissingBean
        FallbackExceptionHandler fallbackExceptionHandler(
                ProblemDetailFactory problemDetailFactory,
                ApplicationContext applicationContext,
                ExceptionHandlerProperties properties
        ) {
            return new FallbackExceptionHandler(
                    problemDetailFactory,
                    applicationContext,
                    properties
            );
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Conditional(AnyHandlerEnabledCondition.class)
    static class ProblemDetailsInfrastructureConfiguration {

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

        @Bean
        @ConditionalOnMissingBean
        ProblemDetailFactory problemDetailFactory(
                ProblemMessageResolver messageResolver
        ) {
            return new ProblemDetailFactory(messageResolver);
        }
    }

    static final class AnyHandlerEnabledCondition extends AnyNestedCondition {

        AnyHandlerEnabledCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(
                prefix = ExceptionHandlerProperties.PREFIX,
                name = "problem-enabled",
                havingValue = "true"
        )
        static class ProblemEnabled {
        }

        @ConditionalOnProperty(
                prefix = ExceptionHandlerProperties.PREFIX,
                name = "fallback-enabled",
                havingValue = "true"
        )
        static class FallbackEnabled {
        }
    }
}
