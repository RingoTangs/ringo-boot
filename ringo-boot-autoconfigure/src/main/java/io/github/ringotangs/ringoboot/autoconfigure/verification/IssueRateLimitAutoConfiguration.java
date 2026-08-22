package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.autoconfigure.verification.redis.RedisIssueRateLimitAutoConfiguration;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueContext;
import io.github.ringotangs.ringoboot.verification.limit.IssueContextResolver;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitManager;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * 自动配置验证码签发限流上下文、规则、状态存储和统一管理器。
 *
 * <p>应用提供自定义 Resolver、Store 或 Limiter 时，对应默认组件会自动回退。容器内的 Rule Bean 会按照 Spring 顺序统一收集。
 *
 * <p>Auto-configures verification issue rate-limit contexts, rules, state storage, and the central
 * manager. Default components back off when the application supplies the corresponding Resolver,
 * Store, or Limiter, and Rule beans are collected in Spring order.</p>
 */
@AutoConfiguration(after = {VerificationAutoConfiguration.class, RedisIssueRateLimitAutoConfiguration.class})
@ConditionalOnClass(IssueRateLimiter.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(IssueRateLimitProperties.class)
public class IssueRateLimitAutoConfiguration {

    /**
     * 在应用未提供上下文解析器时，仅使用验证码键创建签发上下文。
     *
     * @return 不包含 IP、设备等环境属性的默认上下文解析器
     */
    @Bean
    @ConditionalOnMissingBean
    IssueContextResolver issueContextResolver() {
        return IssueContext::of;
    }

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
     * 使用配置的签发间隔创建完整验证码键默认冷却规则。
     *
     * @param properties 签发限流配置属性
     * @return 默认完整验证码键冷却规则
     */
    @Bean
    @Conditional(OnPositiveIssueRateLimitIntervalCondition.class)
    @ConditionalOnMissingBean(IssueRateLimiter.class)
    IssueRateLimitRule defaultKeyCooldownIssueRateLimitRule(IssueRateLimitProperties properties) {
        return IssueRateLimitRule.of(
                "default-key-cooldown",
                context -> IssueLimitBucket.of(
                        context.key().namespace(),
                        context.key().purpose(),
                        context.key().subject()),
                1,
                properties.getInterval());
    }

    /**
     * 收集容器内全部签发规则并创建统一限流管理器。
     *
     * @param rules 容器内的签发限流规则
     * @param properties 签发限流配置属性
     * @param store 签发限流状态存储
     * @param contextResolver 签发上下文解析器
     * @return 统一签发限流入口
     */
    @Bean
    @ConditionalOnBean(IssueRateLimitStore.class)
    @ConditionalOnMissingBean(IssueRateLimiter.class)
    IssueRateLimiter issueRateLimiter(
            ObjectProvider<IssueRateLimitRule> rules,
            IssueRateLimitProperties properties,
            IssueRateLimitStore store,
            IssueContextResolver contextResolver) {
        List<IssueRateLimitRule> configuredRules = properties.toRules();
        List<IssueRateLimitRule> allRules = new ArrayList<>(configuredRules.size() + 1);
        allRules.addAll(configuredRules);
        rules.orderedStream().forEach(allRules::add);
        return new IssueRateLimitManager(List.copyOf(allRules), store, contextResolver);
    }
}
