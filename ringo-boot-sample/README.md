# Ringo Boot Sample

## 邮箱验证码示例 / Email verification example

示例由应用显式组装邮件验证服务。启用后自动配置提供邮件和短信 Stdout Sender，并使用 Redis 保存验证码状态：

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
`VerificationStore` 和 Sender 均可通过自定义 Bean 覆盖。`CodeGenerator` 不会自动注册到 Spring 容器，
由应用在组装渠道服务时直接创建或传入自定义实现。渠道服务的默认 `VerificationPolicy` 由
`VerificationProperties.toPolicy()` 根据 `ringo.boot.verification.*` 配置创建，不注册为 Spring Bean；
业务特定策略可通过 `VerificationService.issue(key, policy)` 在调用时传入。

sample 不注册 `IssueLimitRule`，因此自动配置使用 `IssueLimiter.permitAll()`，不会创建签发限流 Store。Redis 仍用于
保存验证码状态，但不会写入签发限流 ZSET。实际应用需要限流时，可以在代码中注册规则 Bean；限流规则不支持 YAML 配置。

`VerificationService` 只定义签发和校验的业务契约。`AbstractVerificationService` 是该契约的抽象骨架实现，统一编排生成、
存储、限流、派发、派发失败补偿和校验消费。core 已提供 `EmailVerificationService`
和 `SmsVerificationService`。应用需要显式将所需的渠道服务注册为 Bean：

```java
@Bean
EmailVerificationService emailVerificationService(
        VerificationStore store,
        IssueLimiter issueLimiter,
        VerificationProperties properties,
        EmailCodeSender sender) {
    return new EmailVerificationService(
            new NumericCodeGenerator(),
            store,
            issueLimiter,
            properties.toPolicy(),
            sender);
}
```

Sender 直接接收本次签发的 `IssueContext`、明文验证码和准确过期时间。例如邮件模板可以从
`context.policy().ttl()` 读取有效时长，从 `context.key().subject()` 读取收件地址：

```java
@Bean
EmailCodeSender emailCodeSender(EmailClient emailClient) {
    return (context, code, expiresAt) -> emailClient.send(
            context.key().subject(), code, context.policy().ttl(), expiresAt);
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
