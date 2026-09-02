/**
 * 提供框架无关的验证码签发频率限制能力。
 *
 * <p>限流流程分为三层：应用通过 {@link io.github.ringotangs.ringoboot.verification.limit.IssueLimitRule}
 * 声明业务匹配条件和额度桶；{@link io.github.ringotangs.ringoboot.verification.limit.RuleBasedIssueLimiter}
 * 收集并解析所有匹配规则；{@link io.github.ringotangs.ringoboot.verification.limit.IssueLimitStore}
 * 原子检查和消费解析后的签发配额。验证码服务只依赖顶层
 * {@link io.github.ringotangs.ringoboot.verification.limit.IssueLimiter}。
 *
 * <p>验证码服务创建 {@link io.github.ringotangs.ringoboot.verification.context.IssueContext}，子类可通过模板钩子补充
 * IP、设备、账号和租户等属性，再将同一上下文传入限流器。限流包不依赖 HTTP 请求。默认内存 Store 仅适用于单实例场景；分布式应用应使用具备跨进程原子性的 Store。
 *
 * <p>默认管理器采用严格拒绝策略：规则集合为空或没有规则覆盖当前验证码键时抛出
 * {@link io.github.ringotangs.ringoboot.verification.limit.MissingIssueLimitRuleException}。确实需要关闭限流的应用必须显式使用
 * {@link io.github.ringotangs.ringoboot.verification.limit.IssueLimiter#permitAll()}。
 *
 * <p>参数和规则定义错误使用 Java 标准运行时异常；正常受限通过
 * {@link io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult.Throttled} 表达；只有限流基础设施技术故障使用
 * {@link io.github.ringotangs.ringoboot.verification.limit.IssueLimitStoreException}。所有限流业务、配置和基础设施异常统一继承
 * {@link io.github.ringotangs.ringoboot.verification.limit.IssueLimitException}。
 */
@org.jspecify.annotations.NullMarked
package io.github.ringotangs.ringoboot.verification.limit;
