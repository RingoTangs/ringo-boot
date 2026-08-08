package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.DefaultVerificationService;
import io.github.ringotangs.ringoboot.verification.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.NumericCodeGenerator;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerificationService;
import io.github.ringotangs.ringoboot.verification.VerificationStore;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 自动配置框架无关的验证码服务组件。
 *
 * <p>Auto-configures the framework-neutral verification service components.</p>
 */
@AutoConfiguration
@ConditionalOnClass(VerificationService.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(VerificationProperties.class)
public class VerificationAutoConfiguration {

    /**
     * 在用户未提供生成器时创建安全的数字验证码生成器。
     *
     * <p>Creates a secure numeric code generator when the user has not supplied one.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    CodeGenerator verificationCodeGenerator() {
        return new NumericCodeGenerator();
    }

    /**
     * 根据配置属性创建默认验证码策略。
     *
     * <p>Creates the default verification policy from configuration properties.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    VerificationPolicy verificationPolicy(VerificationProperties properties) {
        return new VerificationPolicy(
                properties.getLength(),
                properties.getTtl(),
                properties.getMaxAttempts(),
                properties.getResendInterval());
    }

    /**
     * 显式启用内存存储且用户未提供存储时创建内存实现。
     *
     * <p>Creates the in-memory store when explicitly enabled and no user store is
     * available.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "in-memory-enabled", havingValue = "true")
    VerificationStore inMemoryVerificationStore() {
        return new InMemoryVerificationStore();
    }

    /**
     * 使用唯一存储、生成器和默认策略创建验证码服务。
     *
     * <p>Creates the verification service with the unique store, generator, and default
     * policy.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    VerificationService verificationService(
            CodeGenerator codeGenerator, ObjectProvider<VerificationStore> storeProvider, VerificationPolicy policy) {
        VerificationStore store = storeProvider.getIfUnique();
        if (store == null) {
            long storeCount = storeProvider.stream().count();
            if (storeCount == 0) {
                throw new IllegalStateException("Verification is enabled but no VerificationStore bean is available. "
                        + "Provide a VerificationStore bean or set ringo.boot.verification.in-memory-enabled=true");
            }
            throw new IllegalStateException("Verification requires a unique VerificationStore bean. "
                    + "Mark one store as @Primary or provide a custom VerificationService");
        }
        return new DefaultVerificationService(codeGenerator, store, policy, Clock.systemUTC());
    }
}
