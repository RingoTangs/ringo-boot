/**
 * 提供验证码生成、状态存储以及邮件和短信渠道服务的 Spring Boot 自动配置。
 *
 * <p>默认配置安全的数字验证码生成器、进程内状态存储和标准输出发送器。应用可以提供相同类型的 Bean
 * 替换任一默认组件；生产环境的多实例部署可以选择 Redis 状态存储。</p>
 *
 * <p>Provides Spring Boot auto-configuration for verification code generation, state storage,
 * and email and SMS channel services. It configures a secure numeric generator, in-process state
 * storage, and standard-output senders by default. Applications can replace any default component
 * with a bean of the same type, while multi-instance production deployments can select Redis-backed
 * state storage.</p>
 */
@NullMarked
package io.github.ringotangs.ringoboot.autoconfigure.verification;

import org.jspecify.annotations.NullMarked;
