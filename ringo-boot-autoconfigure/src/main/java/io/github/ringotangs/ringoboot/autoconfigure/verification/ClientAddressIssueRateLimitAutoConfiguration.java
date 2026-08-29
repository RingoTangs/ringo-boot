package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.IssueContextContributor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/** 为基于当前 Servlet 请求的客户端地址签发限流提供适配组件。 */
@AutoConfiguration(before = IssueRateLimitAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({IssueContextContributor.class, HttpServletRequest.class})
@ConditionalOnBean(ClientAddressIssueQuotaRule.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
public class ClientAddressIssueRateLimitAutoConfiguration {

    /** 在应用没有提供解析器时创建基于当前 Servlet 请求的默认实现。 */
    @Bean
    @ConditionalOnMissingBean(ClientAddressResolver.class)
    ClientAddressResolver clientAddressResolver(ObjectProvider<HttpServletRequest> requestProvider) {
        return new ServletClientAddressResolver(requestProvider);
    }

    /** 收集全部客户端地址规则并创建上下文 Contributor。 */
    @Bean
    @ConditionalOnMissingBean(ClientAddressIssueContextContributor.class)
    ClientAddressIssueContextContributor clientAddressIssueContextContributor(
            List<ClientAddressIssueQuotaRule> rules, ClientAddressResolver resolver) {
        return new ClientAddressIssueContextContributor(rules, resolver);
    }
}
