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
`CodeGenerator`、`VerificationPolicy` 和 `VerificationStore` 均可通过自定义 Bean 覆盖。

`VerificationService` 只定义签发和校验的业务契约。`AbstractVerificationService` 是该契约的抽象骨架实现，统一编排生成、
存储、限流、派发、派发失败补偿和校验消费。邮件或短信渠道通过继承该抽象服务并实现
`dispatch` 钩子完成具体派发：

```java
final class EmailVerificationService extends AbstractVerificationService {
    private final EmailClient emailClient;

    EmailVerificationService(
            CodeGenerator generator,
            VerificationStore store,
            VerificationPolicy policy,
            EmailClient emailClient) {
        super(generator, store, policy);
        this.emailClient = emailClient;
    }

    @Override
    protected void dispatch(CodeDelivery delivery) {
        emailClient.send(delivery.key().subject(), delivery.code(), delivery.expiresAt());
    }
}
```

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
