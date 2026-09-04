package io.github.ringotangs.ringoboot.problem.autoconfigure;

import io.github.ringotangs.ringoboot.problem.ProblemTypeUri;
import io.github.ringotangs.ringoboot.problem.web.SpringMvcExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 自动配置 Spring MVC 内置异常的稳定 Problem Details 映射。
 */
@AutoConfiguration(before = WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ProblemDetail.class, ProblemTypeUri.class, ResponseEntityExceptionHandler.class})
@ConditionalOnBooleanProperty("spring.mvc.problemdetails.enabled")
@ConditionalOnProperty(prefix = ProblemProperties.HANDLERS_PREFIX, name = "mvc", havingValue = "true")
public class SpringMvcProblemAutoConfiguration {

    /**
     * 用户提供 ResponseEntityExceptionHandler 时不创建默认 MVC 处理器。
     *
     * @param applicationContext Spring 应用上下文
     * @return Spring MVC 异常处理器
     */
    @Bean
    @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
    SpringMvcExceptionHandler springMvcExceptionHandler(ApplicationContext applicationContext) {
        return new SpringMvcExceptionHandler(applicationContext);
    }
}
