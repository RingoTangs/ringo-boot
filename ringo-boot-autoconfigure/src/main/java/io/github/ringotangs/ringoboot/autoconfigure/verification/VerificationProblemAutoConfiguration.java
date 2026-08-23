package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.autoconfigure.problem.ProblemAutoConfiguration;
import io.github.ringotangs.ringoboot.autoconfigure.problem.ProblemMessageResolver;
import io.github.ringotangs.ringoboot.autoconfigure.problem.ProblemProperties;
import io.github.ringotangs.ringoboot.verification.VerificationException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ProblemDetail;

/** 在 Problem Details 和验证码功能均启用时自动配置验证码异常处理。 */
@AutoConfiguration(after = ProblemAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ProblemDetail.class, VerificationException.class})
@ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = ProblemProperties.PREFIX, name = "verification-enabled", havingValue = "true")
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
public class VerificationProblemAutoConfiguration {

    /**
     * 在共享消息解析器可用且用户未提供自定义实现时创建验证码异常处理器。
     *
     * @param messageResolver 问题消息解析器
     * @return 验证码异常处理器
     */
    @Bean
    @ConditionalOnBean(ProblemMessageResolver.class)
    @ConditionalOnMissingBean
    VerificationExceptionHandler verificationExceptionHandler(ProblemMessageResolver messageResolver) {
        return new VerificationExceptionHandler(messageResolver);
    }
}
