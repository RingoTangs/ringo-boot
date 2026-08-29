/**
 * 提供验证码状态存储、签发限流以及默认邮件和短信发送器的 Spring Boot 自动配置。
 *
 * <p>默认配置进程内状态存储和标准输出发送器。应用显式组装验证服务和验证码生成器；生产环境的多实例部署可以选择 Redis 状态存储。</p>
 */
@NullMarked
package io.github.ringotangs.ringoboot.autoconfigure.verification;

import org.jspecify.annotations.NullMarked;
