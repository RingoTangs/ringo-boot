# Ringo Boot Sample

## 邮箱验证码示例 / Email verification example

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
