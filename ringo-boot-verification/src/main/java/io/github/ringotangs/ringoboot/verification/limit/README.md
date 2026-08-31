# 验证码签发限流

本包提供验证码签发阶段的滚动窗口限流能力。规则负责描述“哪些请求共享多少额度”，Store 负责原子检查并消费额度，验证码服务只通过
`IssueLimiter` 使用这套能力。

## 核心流程

```text
IssueContext
    ↓
IssueLimiter / IssueLimitManager
    ↓ 匹配全部适用规则并解析 bucket
List<IssueLimitQuota>
    ↓ 原子检查并消费
IssueLimitStore
    ↓
Allowed 或 Throttled
```

一次请求可能同时匹配多条规则，例如业务总量、用途总量、接收方冷却和客户端 IP 配额。所有匹配规则采用 AND 关系：只有全部额度允许
时才会同时消费；任一规则受限时，不消费任何规则的额度。

签发名额一旦获得便视为消耗。后续验证码生成、存储或发送失败时不会退还，避免调用方利用下游失败绕过限流。

## 内置规则

| 规则                 | 匹配范围                      | bucket 分段                            | 典型用途               |
|----------------------|-------------------------------|----------------------------------------|------------------------|
| `NamespaceQuotaRule` | namespace + channel           | namespace、channel                     | 业务模块或渠道总量兜底 |
| `PurposeQuotaRule`   | namespace + purpose + channel | namespace、purpose、channel            | 单个验证码用途总量     |
| `SubjectQuotaRule`   | namespace + purpose + channel | namespace、purpose、channel、subject   | 接收方冷却或周期配额   |
| `ClientIpQuotaRule`  | namespace + purpose + channel | namespace、purpose、channel、client IP | 来源 IP 配额           |

规则的 `appliesTo` 只判断业务范围是否匹配，`bucket` 决定哪些请求共享额度。不同规则即使解析出相同 bucket，也会因为规则 ID 不同而使用
独立的限流历史。

## 快速配置

验证码自动配置默认关闭，需要先显式启用：

```yaml
ringo:
  boot:
    verification:
      enabled: true
```

启用后，Spring Boot 会收集容器中的所有 `IssueLimitRule` Bean，并在存在规则时自动创建 `IssueLimitManager`。

```java
@Configuration(proxyBeanMethods = false)
class IssueLimitConfiguration {

    @Bean
    NamespaceQuotaRule accountEmailHourlyQuotaRule() {
        return NamespaceQuotaRule.builder()
                .namespace("account")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(100)
                .window(Duration.ofHours(1))
                .build();
    }

    @Bean
    SubjectQuotaRule emailVerificationResendRule() {
        return SubjectQuotaRule.builder()
                .namespace("account")
                .purpose("email-verification")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(5)
                .window(Duration.ofMinutes(1))
                .build();
    }
}
```

内置规则不接收手动 ID，而是根据规则类型和全部定义参数生成稳定 ID。上面的接收方规则会生成：

```text
rule@subject-quota:ns@account:purpose@email-verification:channel@email:issues@5:window@1minutes
```

窗口使用能够精确表达 `Duration` 的最大单位，因此 `Duration.ofSeconds(60)` 和 `Duration.ofMinutes(1)` 生成相同 ID。修改
namespace、purpose、channel、maxIssues 或 window 会生成新的 ID，也会开始一份新的限流历史。

## 匹配与默认行为

- 应用没有注册规则或自定义 `IssueLimiter` 时，自动配置使用 `IssueLimiter.permitAll()`，不会创建限流 Store。
- 注册任意规则后，`IssueLimitManager` 会严格要求每个签发请求至少匹配一条规则。
- 没有规则覆盖当前 namespace、purpose 和 channel 时抛出 `MissingIssueLimitRuleException`。
- 规则 ID 必须唯一；重复定义会在创建 Manager 时失败。
- 规则顺序只影响稳定的解析和诊断顺序，不影响 AND 限流语义。

如果只为部分业务配置规则，需要同时提供覆盖其他业务的规则，或者自行替换 `IssueLimiter` 实现。

## 客户端 IP 规则

`ClientIpQuotaRule` 从 `IssueContext` 的 `client-ip` 属性读取客户端地址。规则不会访问 Servlet API，也不会解析代理请求头。

```java
@Bean
ClientIpQuotaRule emailVerificationClientIpRule() {
    return ClientIpQuotaRule.builder()
            .namespace("account")
            .purpose("email-verification")
            .channel(VerificationChannel.EMAIL)
            .maxIssues(50)
            .window(Duration.ofHours(1))
            .build();
}
```

Servlet 应用可以显式启用默认 Contributor：

```yaml
ringo:
  boot:
    verification:
      enabled: true
      contributor:
        client-ip: true
```

该配置默认关闭。启用后，`ClientIpContributor` 在每次请求中读取 Servlet 容器解析出的远端地址并写入上下文。使用 Nginx 或其他反向代理时，
应先在可信 Web 基础设施中正确配置转发头处理，否则读取到的可能是代理服务器地址。不要直接信任任意客户端提交的
`X-Forwarded-For`。

非 Servlet 应用或需要自定义可信代理策略时，可以提供自己的 `IssueContextContributor`，向上下文写入
`ClientIpQuotaRule.ATTRIBUTE_NAME`。

## 状态存储

### 内存

`InMemoryIssueLimitStore` 是线程安全的单 JVM 实现。它会原子检查全部配额，并周期性清理过期的空历史。状态不会持久化或跨实例共享，
适合测试、本地开发和单实例应用。

### Redis

选择 Redis 后，自动配置使用跨实例原子执行的 `RedisIssueLimitStore`：

```yaml
spring:
  application:
    name: account-service

ringo:
  boot:
    verification:
      enabled: true
      store: redis
```

应用还需要提供：

- Spring Data Redis 的 `StringRedisTemplate`。
- 唯一的 `VerificationHmacKey` Bean，且同一应用的所有实例使用相同密钥。
- 有效的 `spring.application.name`，用于隔离 Redis key。

Redis key 包含应用隔离、Cluster hash tag、存储版本、规则 ID 和 bucket 的 HMAC-SHA256 摘要。subject、客户端 IP 等 bucket 原始值不会
直接出现在 key 中。

应用也可以提供自己的 `IssueLimitStore` Bean。自定义实现必须将一次 `acquire` 中的全部配额作为一个原子操作处理，不能出现部分消费。

## 结果与异常

`IssueLimitResult` 有两种正常结果：

- `Allowed`：全部额度允许，并且已经消费本次名额。
- `Throttled`：至少一条规则受限；`violations` 记录每条生效规则的 ID 和 `retryAfter`，总体等待时间取最大值。

验证码服务收到 `Throttled` 后抛出 `IssueLimitExceededException`。Web 层可以把总体等待秒数写入 HTTP `Retry-After` 响应头。

以下情况不是正常限流结果：

| 异常                                 | 含义                                     |
|--------------------------------------|------------------------------------------|
| `MissingIssueLimitRuleException`     | 已启用规则管理，但当前请求没有规则覆盖   |
| `IssueLimitStoreException`           | Store 或底层原子操作发生技术故障         |
| `IllegalArgumentException`           | 规则定义、额度或窗口违反契约             |

## 自定义扩展

应用可以按需要替换三个层次：

- 实现 `IssueLimitRule`：增加设备、账号、租户等额度维度。实现应无状态、线程安全，并返回稳定、唯一的 kebab-case ID。
- 实现 `IssueLimitStore`：接入其他共享存储，同时保证多配额的全有或全无消费语义。
- 实现 `IssueLimiter`：完全替换规则管理和状态存储流程。

自定义 Rule 的 `appliesTo` 应只判断业务范围。规则已经适用但缺少安全属性时，`bucket` 应明确失败，不应返回“不匹配”来静默绕过限流。
不要在 Rule Bean 中读取 HTTP 请求、访问数据库或调用远程服务；请求级信号应先通过 `IssueContextContributor` 写入上下文。
