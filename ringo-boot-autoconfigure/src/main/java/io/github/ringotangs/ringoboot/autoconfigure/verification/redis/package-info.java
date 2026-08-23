/**
 * 提供基于 Redis 的验证码状态存储、签发限流状态存储与 Spring Boot 自动配置。
 *
 * <p>状态以 Redis Hash 保存，并通过单键 Lua 脚本保证签发、校验、消费和作废操作的原子性。
 * Redis 键中的验证主体和验证码本身均使用共享密钥生成 HMAC 摘要。</p>
 */
@NullMarked
package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import org.jspecify.annotations.NullMarked;
