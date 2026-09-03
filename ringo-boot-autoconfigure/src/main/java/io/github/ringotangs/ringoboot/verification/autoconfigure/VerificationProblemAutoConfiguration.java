package io.github.ringotangs.ringoboot.verification.autoconfigure;

import io.github.ringotangs.ringoboot.problem.ProblemDescriptor;
import io.github.ringotangs.ringoboot.problem.autoconfigure.ProblemAutoConfiguration;
import io.github.ringotangs.ringoboot.problem.autoconfigure.ProblemProperties;
import io.github.ringotangs.ringoboot.verification.VerificationException;
import io.github.ringotangs.ringoboot.verification.web.VerificationExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ProblemDetail;

/**
 * 在 Problem Details 和验证码功能均启用时自动配置验证码异常处理。
 */
@AutoConfiguration(after = ProblemAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ProblemDetail.class, ProblemDescriptor.class, VerificationException.class})
@ConditionalOnBooleanProperty(ProblemProperties.ENABLED_PROPERTY)
@ConditionalOnProperty(prefix = ProblemProperties.HANDLERS_PREFIX, name = "verification", havingValue = "true")
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
public class VerificationProblemAutoConfiguration {

    /**
     * 在用户未提供自定义实现时创建验证码异常处理器。
     *
     * @return 验证码异常处理器
     */
    @Bean
    @ConditionalOnMissingBean
    VerificationExceptionHandler verificationExceptionHandler() {
        return new VerificationExceptionHandler();
    }
}
