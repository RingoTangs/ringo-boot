# Ringo Boot Sample

## 邮箱验证码示例 / Email verification example

示例通过验证码自动配置创建服务。启用后默认提供邮件和短信 Stdout Sender，并使用 Redis 保存验证码状态：

```yaml
ringo:
  boot:
    verification:
      enabled: true
      store: redis
      length: 6
      ttl: 5m
      max-attempts: 5
```

sample 已引入 `spring-boot-starter-data-redis`。Spring Boot 创建 `StringRedisTemplate` 后，Ringo Boot
自动配置会创建 `RedisVerificationStore`；应用不需要自行提供 Redis Store Bean。
`CodeGenerator`、`VerificationStore` 和 Sender 均可通过自定义 Bean 覆盖。
渠道服务的默认 `VerificationPolicy` 由 `ringo.boot.verification.*` 配置直接创建，不注册为 Spring Bean；
业务特定策略可通过 `VerificationService.issue(key, policy)` 在调用时传入。

限流规则通过 `IssueRateLimitRule` Bean 在代码中定义，不支持 YAML 配置。应用没有提供规则 Bean 时，自动配置使用同一完整验证码键
60 秒只能签发一次的安全默认规则；提供任意规则 Bean 后，该默认规则自动回退。sample 的 `IssueRateLimitConfiguration`
显式注册了 60 秒冷却、当前应用每小时 1000 次以及每个邮箱每小时 10 次三条规则。
某个业务没有任何匹配规则时不会签发验证码。确实需要完全关闭限流的应用应显式提供
`IssueRateLimiter.permitAll()` Bean。

`VerificationService` 只定义签发和校验的业务契约。`AbstractVerificationService` 是该契约的抽象骨架实现，统一编排生成、
存储、限流、派发、派发失败补偿和校验消费。core 已提供 `EmailVerificationService`
和 `SmsVerificationService`。应用只需提供对应的 Sender Bean，自动配置会创建渠道服务：

```java
@Bean
EmailCodeSender emailCodeSender(EmailClient emailClient) {
    return delivery -> emailClient.send(
            delivery.key().subject(), delivery.code(), delivery.expiresAt());
}
```

## Redis 验证码存储 / Redis verification storage

sample 默认连接本机 Redis。可以通过 `REDIS_URL` 指定远程地址、认证信息和数据库编号：

```yaml
spring:
  data:
    redis:
      url: ${REDIS_URL:redis://localhost:6379}

ringo:
  boot:
    verification:
      enabled: true
      store: redis
```

sample 会读取环境变量 `VERIFICATION_HMAC_KEY` 并创建 `VerificationHmacKey` Bean。该值必须是 Base64
编码且解码后至少 32 字节的共享密钥，所有应用实例必须使用相同值。
Redis 中只保存验证码及验证键的 HMAC 摘要，不保存验证码、邮箱或手机号明文。

可以使用 OpenSSL 生成密钥：

```shell
openssl rand -base64 32
```

启动前设置连接和密钥环境变量。Redis 无认证且运行在本机时可以省略 `REDIS_URL`：

```shell
$env:REDIS_URL="redis://username:password@redis.example.com:6379/0"
$env:VERIFICATION_HMAC_KEY="生成的 Base64 密钥"
mvn -pl ringo-boot-sample -am spring-boot:run
```

测试环境通过测试配置切换回 `InMemoryVerificationStore`，因此执行 Maven 测试不需要 Redis 实例。

Stdout Sender 会输出明文验证码，仅能用于本地开发。应用提供真实的 `EmailCodeSender`
或 `SmsCodeSender` Bean 时，对应的默认实现会自动回退。

签发验证码。业务响应只包含过期时间，不返回明文验证码：

```shell
curl -i -X POST http://localhost:8080/verification/email/code \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}'
```

从应用标准输出中找到包含 `DEVELOPMENT ONLY` 的记录，读取其中的六位验证码，然后完成校验：

```shell
curl -i -X POST http://localhost:8080/verification/email/verify \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","code":"123456"}'
```

成功校验返回 `204 No Content`，并立即消费验证码。相同验证码不能再次使用。

> [!WARNING]
> Stdout Sender 会输出明文验证码，仅用于本地演示，不能用于生产环境。
> 生产应用必须提供真实的 `EmailCodeSender` 和 `SmsCodeSender` 覆盖默认实现。
