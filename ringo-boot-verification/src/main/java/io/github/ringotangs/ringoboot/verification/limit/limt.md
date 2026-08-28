# 验证码限流规则参考

当前代码架构、运行流程和 Rule Bean 示例见同目录下的 `README.md`。本文重点记录业界常见规则和阈值建议。

业界没有一套统一的验证码限流数值，但主流做法非常一致：采用“多维度、多时间窗口、签发与校验分离”的组合限流，而不是只有一
个 60 秒冷却时间。

| 限流层次   | 常用维度                   | 主要解决的问题             |
| :--------- | :------------------------- | :------------------------- |
| 重发冷却   | 业务用途 + 渠道 + 手机号/邮箱 | 防止用户连续点击           |
| 接收方配额 | 手机号/邮箱 + 分钟/小时/天 | 防短信轰炸、骚扰和成本攻击 |
| 来源配额   | IP、设备指纹、会话         | 防攻击者批量轰炸不同号码   |
| 账号配额   | 用户账号 + 时间窗口        | 防登录、找回密码接口滥用   |
| 业务配额   | namespace + purpose        | 隔离登录、注册、绑卡等用途 |
| 渠道配额   | SMS、Email、Voice          | 保护第三方供应商额度       |
| 全局配额   | 应用、租户、地区           | 熔断异常流量和成本失控     |
| 校验次数   | 验证码、账号、IP           | 防止暴力猜测验证码         |

### 1. 同一接收方的重发冷却

这是项目当前已经实现的规则：

namespace + purpose + channel + subject

例如同一个邮箱用于登录：

account:login:email:user@example.com

一般设置为 30～60 秒一次。Twilio 也建议针对同一号码设置发送间隔，并在连续请求时使用递增延迟。Twilio 防欺诈建议
(https://www.twilio.com/docs/verify/preventing-toll-fraud)

它主要改善用户误操作，但单独使用远远不够。例如攻击者可以每次换一个手机号，从而绕过该限制。

### 2. 同一手机号或邮箱的周期配额

通常会叠加多个窗口：

每分钟最多 N 次
每小时最多 N 次
每天最多 N 次

例如可以采用以下初始值：

同一 subject + purpose：60 秒 1 次
同一 subject：10 分钟 5 次
同一 subject：1 小时 10 次
同一 subject：1 天 20 次

这些是适合普通业务的建议起点，不是行业强制标准。实际值需要根据注册、登录、账号找回等风险分别调整。

AWS Cognito 对部分验证码操作采用每用户每小时 5 次的限制，例如重发注册确认码和获取用户属性验证码。Amazon Cognito 配额
(https://docs.aws.amazon.com/cognito/latest/developerguide/quotas.html)

### 3. IP、设备和会话限流

只限制手机号会被批量号码绕过，因此公开签发接口一般还会限制：

IP + purpose
deviceId + purpose
sessionId + purpose

例如：

同一 IP：10 分钟最多 20 次
同一 IP：1 小时最多 50 次
同一设备：1 小时最多 20 次

IP 阈值不宜太低，因为公司、校园或移动网络可能有大量用户共享公网 IP。通常 IP 限制比单个手机号限制宽松。

Firebase 就同时设置了项目级、IP 级以及单一手机号相关限制，例如公开文档列出了每 IP
每分钟和每小时的短信发送额度。Firebase Authentication 限制 (https://firebase.google.com/docs/auth/limits)

### 4. 账号维度限流

如果请求已经关联登录账号，还应限制：

accountId + purpose

它与手机号限流不能互相替代，因为：

- 一个账号可能不断更换绑定手机号。
- 一个手机号可能关联多个账号。
- 攻击者可能从多个 IP 攻击同一账号。

OWASP 建议账号找回等接口至少采用账号维度限流，防止攻击者向特定用户持续发送短信或邮件。OWASP Forgot Password Cheat
Sheet (https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html)

### 5. 验证码校验次数限制

签发限流和校验限流必须分开设计。

常见规则是：

单个验证码最多验证 5 次
连续失败后验证码立即失效
同一账号连续失败时增加等待时间

你的项目已有：

VerificationPolicy.maxAttempts

默认 5 次，这个设计是合理的。

NIST 要求低熵 OTP 必须防止在线猜测，并建议根据连续失败次数逐渐延长等待时间；100
次只是规范中的上限，实际系统可以采用更低限制。NIST SP 800-63B (https://pages.nist.gov/800-63-4/sp800-63b.html)

### 6. 递增延迟、临时封禁与 CAPTCHA

达到普通配额后，不一定立即永久封禁，可以分级处理：

正常 → 允许
轻微超限 → 返回 429 和 retryAfter
持续超限 → 延长冷却时间
高风险 → 要求 CAPTCHA
严重异常 → 临时封禁 IP、设备或账号

常见递增延迟类似：

第一次超限：等待 60 秒
第二次超限：等待 2 分钟
第三次超限：等待 5 分钟
持续攻击：等待 30 分钟或要求 CAPTCHA

OWASP 建议把 CAPTCHA 当作纵深防御措施，并可在少量失败后再触发，避免影响所有正常用户。OWASP Authentication Cheat Sheet
(https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)

### 7. 供应商和全局成本限流

短信属于付费资源，还需要业务键以外的保护：

application + channel
tenant + channel
countryCode + channel
provider + channel

典型规则包括：

- 单应用每分钟最多发送多少短信。
- 单租户每日短信预算。
- 某国家或号段的单独配额。
- 短信供应商接近额度时降级、切换或熔断。
- 邮件和短信使用不同阈值。

Twilio 支持业务传入自定义限流键，例如用户 IP；Firebase 同时实施项目级、IP
级和发送量限制。这说明供应商侧配额不能替代应用自身的限流。Twilio Service Rate Limits
(https://www.twilio.com/docs/verify/api/service-rate-limits)、Firebase Authentication 限制
(https://firebase.google.com/docs/auth/limits)

### 对当前项目的建议

项目初期建议分两层实现：

1. 基础层：
   - VerificationKey 冷却时间：60 秒。
   - 单验证码最多校验 5 次。
   - subject 每小时和每天的累计签发配额。
   - Redis 跨实例原子限流。
   - 返回明确的 retryAfter。

2. 风控扩展层：
   - IP、设备、账号限流。
   - 多时间窗口组合。
   - CAPTCHA 和递增惩罚。
   - 租户、供应商、地区及成本配额。
   - 告警、审计和动态黑名单。

当前业务调用方仍只需调用 `VerificationService.issue(VerificationKey)`。签发服务在入口创建 `IssueContext`，通过
`customizeIssueContext` 模板钩子补充环境信息后传给 `IssueRateLimiter.acquire(IssueContext, Instant)`，因此规则仍可使用 IP、设备、账号和租户维度：

```java
@Override
protected IssueContext customizeIssueContext(IssueContext context) {
    return context
            .with("ip-address", resolveTrustedClientIp())
            .with("device-id", resolveTrustedDeviceId());
}
```

需要环境维度的应用应继承对应的邮件或短信验证码服务并覆盖该模板方法，再把自定义服务注册为 Bean。HTTP 请求提取和可信代理策略属于
应用层职责，限流规则本身只读取上下文，不直接依赖 Web API。
