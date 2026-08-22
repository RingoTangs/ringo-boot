/**
 * 提供框架无关的验证码签发频率限制能力。
 *
 * <p>限流流程分为三层：应用通过 {@link io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule}
 * 声明业务匹配条件和额度桶；{@link io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitManager}
 * 收集并解析所有匹配规则；{@link io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitStore}
 * 原子检查和消费解析后的约束。验证码服务只依赖顶层
 * {@link io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter}。
 *
 * <p>{@link io.github.ringotangs.ringoboot.verification.limit.IssueContext} 支持应用自定义 IP、设备、账号和租户等属性，core
 * 不预定义业务维度。默认内存 Store 仅适用于单实例场景；分布式应用应使用具备跨进程原子性的 Store。
 */
@org.jspecify.annotations.NullMarked
package io.github.ringotangs.ringoboot.verification.limit;
