package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.autoconfigure.verification.redis.RedisIssueRateLimitAutoConfiguration;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitManager;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 自动配置验证码签发限流上下文、规则、状态存储和统一管理器。
 *
 * <p>应用提供自定义解析器、存储或限流器时，对应默认组件会自动回退。容器内的规则 Bean 会按照 Spring 顺序统一收集。</p>
 */
@AutoConfiguration(after = RedisIssueRateLimitAutoConfiguration.class)
@ConditionalOnClass(IssueRateLimiter.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
public class IssueRateLimitAutoConfiguration {

    private static final Duration DEFAULT_KEY_COOLDOWN = Duration.ofSeconds(60);

    /**
     * 在内存模式且用户未提供存储时创建进程内签发限流状态存储。
     *
     * @return 进程内验证码签发限流状态存储
     */
    @Bean
    @ConditionalOnMissingBean({IssueRateLimiter.class, IssueRateLimitStore.class})
    @ConditionalOnProperty(
            prefix = VerificationProperties.PREFIX,
            name = "store",
            havingValue = "memory",
            matchIfMissing = true)
    IssueRateLimitStore inMemoryIssueRateLimitStore() {
        return new InMemoryIssueRateLimitStore();
    }

    /**
     * 在应用没有提供规则时创建完整验证码键默认冷却规则。
     *
     * @return 默认完整验证码键冷却规则
     */
    @Bean
    @ConditionalOnMissingBean({IssueRateLimiter.class, IssueRateLimitRule.class})
    IssueRateLimitRule defaultKeyCooldownIssueRateLimitRule() {
        return IssueRateLimitRule.of(
                "default-key-cooldown",
                context -> IssueLimitBucket.of(
                        context.key().namespace(),
                        context.key().purpose(),
                        context.key().subject()),
                1,
                DEFAULT_KEY_COOLDOWN);
    }

    /**
     * 收集容器内全部签发规则并创建统一限流管理器。
     *
     * @param rules 容器内的签发限流规则
     * @param store 签发限流状态存储
     * @return 统一签发限流入口
     */
    @Bean
    @ConditionalOnBean(IssueRateLimitStore.class)
    @ConditionalOnMissingBean(IssueRateLimiter.class)
    IssueRateLimiter issueRateLimiter(ObjectProvider<IssueRateLimitRule> rules, IssueRateLimitStore store) {
        List<IssueRateLimitRule> ruleBeans = rules.orderedStream().toList();
        return new IssueRateLimitManager(ruleBeans, store);
    }
}
