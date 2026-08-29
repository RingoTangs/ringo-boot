package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import java.time.Instant;
import java.util.Objects;

/**
 * 验证码服务使用的签发频率限制入口。
 *
 * <p>默认实现由 {@link IssueRateLimitManager} 提供。签发服务负责创建上下文，限流器只负责检查和消费额度。
 * 应用也可以替换整个入口，但自定义实现必须保证：一次获取涉及多条规则时，
 * 先检查全部规则；只有全部规则允许时，才能同时消费所有规则的额度。正常受限的请求不得消费任何规则的额度。
 *
 * <p>获得名额后即视为消耗，无论后续验证码生成、存储或派发是否成功，都不应自动退还。这样可以避免调用方利用后续步骤失败持续绕过
 * 签发频率限制。分布式实现必须保证跨进程的原子获取语义。
 *
 * <p>当多条规则同时受限时，返回的剩余等待时间应为所有受限规则中的最大值，表示全部规则再次允许签发所需的时间。
 *
 * <p>参数、规则声明和上下文数据违反契约时使用 Java 标准运行时异常；只有底层存储或原子操作等技术故障才使用
 * {@link IssueRateLimitStoreException}。正常限流始终通过 {@link IssueLimitResult.Throttled} 返回。
 *
 * <p>默认管理器采用严格拒绝策略，没有规则覆盖当前验证码键时抛出 {@link MissingIssueRateLimitRuleException}。不需要限制签发频率时
 * 可以使用 {@link #permitAll()}；Spring Boot 自动配置在应用没有提供规则或限流器时使用该实现。
 */
@FunctionalInterface
public interface IssueRateLimiter {

    /**
     * 创建一个显式允许所有签发请求的限流器。
     *
     * <p>该实现不会创建或消费任何额度，只适用于应用明确决定关闭签发限流的场景。生产应用通常应配置实际限流规则。
     * 返回的实现仍会校验验证码键和请求时间非空。
     *
     * @return 显式允许所有签发请求的限流器
     */
    static IssueRateLimiter permitAll() {
        return (context, requestedAt) -> {
            Objects.requireNonNull(context, "context must not be null");
            Objects.requireNonNull(requestedAt, "requestedAt must not be null");
            return new IssueLimitResult.Allowed();
        };
    }

    /**
     * 尝试获取一次验证码签发名额。
     *
     * @param context     当前签发流程的上下文
     * @param requestedAt 请求签发的时间
     * @return 允许签发或受限结果
     * @throws NullPointerException               当任一参数为 {@code null} 时
     * @throws IllegalArgumentException           当规则声明或解析出的上下文数据非法时
     * @throws RuntimeException                   当上下文解析失败或匹配规则无法解析额度桶时
     * @throws MissingIssueRateLimitRuleException 当没有规则覆盖当前验证码键时
     * @throws IssueRateLimitStoreException       当底层限流操作失败时
     */
    IssueLimitResult acquire(IssueContext context, Instant requestedAt) throws IssueRateLimitException;
}
