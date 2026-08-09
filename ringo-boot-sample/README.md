# Ringo Boot Sample

## 邮箱验证码示例 / Email verification example

示例通过验证码自动配置创建服务。启用后默认提供邮件和短信 Stdout Sender：

```yaml
ringo:
  boot:
    verification:
      enabled: true
      length: 6
      ttl: 5m
      max-attempts: 5
      resend-interval: 60s
```

启用验证码功能后，缺少自定义实现时默认使用 `InMemoryVerificationStore`。
生产应用应提供自己的 `VerificationStore` Bean，以替换仅适用于本地开发和单实例应用的内存存储。
`CodeGenerator`、`VerificationStore` 和 Sender 均可通过自定义 Bean 覆盖。
渠道服务的默认 `VerificationPolicy` 由 `ringo.boot.verification.*` 配置直接创建，不注册为 Spring Bean；
业务特定策略可通过 `VerificationService.issue(key, policy)` 在调用时传入。

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

Stdout Sender 会输出明文验证码，仅能用于本地开发。应用提供真实的 `EmailCodeSender`
或 `SmsCodeSender` Bean 时，对应的默认实现会自动回退。

切换到 Redis 时只需提供 Redis 版本的 `VerificationStore` Bean，验证服务和发送逻辑不需要修改。

从仓库根目录启动示例：

```shell
mvn -pl ringo-boot-sample -am spring-boot:run
```

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
