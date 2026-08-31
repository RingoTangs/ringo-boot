package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.autoconfigure.verification.redis.RedisIssueLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitManager;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimiter;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 自动配置验证码签发限流器、状态存储和统一管理器。
 *
 * <p>应用没有提供规则时默认允许所有签发请求；提供规则 Bean 后，自动创建状态存储并按照 Spring 顺序统一收集规则。应用提供自定义
 * Store 或 Limiter 时，对应默认组件会自动回退。</p>
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(IssueLimiter.class)
@ConditionalOnMissingBean(IssueLimiter.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
public class IssueLimitAutoConfiguration {

    /**
     * 收集容器内全部签发规则并创建签发限流入口。
     *
     * <p>没有规则时显式允许全部签发请求，并且不要求存在限流状态存储；存在规则时才获取唯一 Store 并创建统一限流管理器。
     *
     * @param rules  容器内的签发限流规则
     * @param stores 容器内的签发限流状态存储
     * @return 允许全部签发请求的实现或统一签发限流管理器
     */
    @Bean
    IssueLimiter issueLimiter(ObjectProvider<IssueLimitRule> rules, ObjectProvider<IssueLimitStore> stores) {
        List<IssueLimitRule> ruleBeans = rules.orderedStream().toList();
        if (ruleBeans.isEmpty()) {
            return IssueLimiter.permitAll();
        }
        return new IssueLimitManager(ruleBeans, stores.getObject());
    }

    /**
     * 在应用提供规则、选择内存模式且未提供存储时创建进程内签发限流状态存储。
     *
     * @return 进程内验证码签发限流状态存储
     */
    @Bean
    @ConditionalOnBean(IssueLimitRule.class)
    @ConditionalOnMissingBean(IssueLimitStore.class)
    @ConditionalOnProperty(
            prefix = VerificationProperties.PREFIX,
            name = "store",
            havingValue = "memory",
            matchIfMissing = true)
    IssueLimitStore inMemoryIssueLimitStore() {
        return new InMemoryIssueLimitStore();
    }

    /**
     * 在显式选择 Redis 且应用提供限流规则时配置跨实例限流状态存储。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnBean({StringRedisTemplate.class, IssueLimitRule.class})
    @ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "store", havingValue = "redis")
    static class RedisStoreConfiguration {

        /**
         * 使用 Redis、共享 HMAC 密钥和应用名称创建签发限流状态存储。
         *
         * @param redisTemplate Redis 字符串操作模板
         * @param hmacKey       应用提供的验证码 HMAC 密钥
         * @param environment   Spring 环境
         * @return Redis 验证码签发限流状态存储
         */
        @Bean
        @ConditionalOnMissingBean(IssueLimitStore.class)
        IssueLimitStore redisIssueLimitStore(
                StringRedisTemplate redisTemplate, VerificationHmacKey hmacKey, Environment environment) {
            return new RedisIssueLimitStore(
                    redisTemplate, hmacKey.getEncoded(), environment.getRequiredProperty("spring.application.name"));
        }
    }
}
