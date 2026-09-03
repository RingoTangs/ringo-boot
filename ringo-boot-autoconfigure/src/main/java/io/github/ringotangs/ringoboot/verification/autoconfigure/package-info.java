/**
 * 提供验证码状态存储、签发限流和 Problem Details 适配的 Spring Boot 自动配置。
 *
 * <p>默认配置进程内状态存储和标准输出发送器。应用显式组装验证服务和验证码生成器；生产环境的多实例部署可以选择 Redis 状态存储。</p>
 */
@NullMarked
package io.github.ringotangs.ringoboot.verification.autoconfigure;

import org.jspecify.annotations.NullMarked;
