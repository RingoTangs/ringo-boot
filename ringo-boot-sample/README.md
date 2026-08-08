# Ringo Boot Sample

## 邮箱验证码示例 / Email verification example

示例通过验证码自动配置创建服务，并显式启用内存存储：

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
`CodeGenerator`、`VerificationPolicy`、`VerificationStore` 和 `VerificationService` 均可通过自定义 Bean 覆盖。

`VerificationTemplate` 统一处理签发、限流、发送失败补偿和校验。邮箱或短信业务只需要传入对应的
`CodeSender`：

```java
DeliveryResult result = verificationTemplate.issue(
        new VerificationKey("email-login", normalizedEmail),
        delivery -> emailClient.send(
                delivery.key().subject(), delivery.code(), delivery.expiresAt()));
```

切换到 Redis 时只需提供 Redis 版本的 `VerificationStore` Bean，模板和发送逻辑不需要修改。

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

从内存测试邮箱读取验证码：

```shell
curl "http://localhost:8080/verification/email/test-inbox?email=user@example.com"
```

使用测试邮箱返回的六位验证码完成校验：

```shell
curl -i -X POST http://localhost:8080/verification/email/verify \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","code":"123456"}'
```

成功校验返回 `204 No Content`，并立即消费验证码。相同验证码不能再次使用。

> [!WARNING]
> `/verification/email/test-inbox` 和 `InMemoryEmailCodeSender` 会暴露或保存明文验证码，
> 仅用于本地演示和自动化测试，不能用于生产环境。生产应用应替换为真实邮件服务，且不得提供测试邮箱接口。
