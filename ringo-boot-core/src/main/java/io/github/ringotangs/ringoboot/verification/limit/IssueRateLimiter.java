package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Instant;

/**
 * 验证码服务使用的签发频率限制入口。
 *
 * <p>默认实现由 {@link IssueRateLimitManager} 提供。应用也可以替换整个入口，但自定义实现必须保证：一次获取涉及多条规则时，
 * 先检查全部规则；只有全部规则允许时，才能同时消费所有规则的额度。正常受限的请求不得消费任何规则的额度。
 *
 * <p>获得名额后即视为消耗，无论后续验证码生成、存储或派发是否成功，都不应自动退还。这样可以避免调用方利用后续步骤失败持续绕过
 * 签发频率限制。分布式实现必须保证跨进程的原子获取语义。
 *
 * <p>当多条规则同时受限时，返回的剩余等待时间应为所有受限规则中的最大值，表示全部规则再次允许签发所需的时间。
 *
 * <p>参数、规则声明和上下文数据违反契约时使用 Java 标准运行时异常；只有底层存储或原子操作等技术故障才使用
 * {@link IssueRateLimitException}。正常限流始终通过 {@link IssueLimitResult.Throttled} 返回。
 */
@FunctionalInterface
public interface IssueRateLimiter {

    /**
     * 尝试获取一次验证码签发名额。
     *
     * @param context 包含验证码键和应用扩展属性的签发上下文
     * @param requestedAt 请求签发的时间
     * @return 允许签发或受限结果
     * @throws NullPointerException 当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当规则声明或上下文数据非法时
     * @throws RuntimeException 当匹配规则无法从上下文解析额度桶时
     * @throws IssueRateLimitException 当底层限流操作失败时
     */
    IssueLimitResult acquire(IssueContext context, Instant requestedAt) throws IssueRateLimitException;

    /**
     * 使用不含扩展属性的验证码键尝试获取签发名额。
     *
     * <p>该重载只适用于所有匹配规则都能从 {@link VerificationKey} 生成额度桶的场景。依赖 IP、设备、账号等扩展属性时，应调用
     * {@link #acquire(IssueContext, Instant)}。
     *
     * @param key 验证码键
     * @param requestedAt 请求签发的时间
     * @return 允许签发或受限结果
     * @throws NullPointerException 当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当规则声明或验证码键数据非法时
     * @throws RuntimeException 当匹配规则依赖未提供的扩展属性时
     * @throws IssueRateLimitException 当底层限流操作失败时
     */
    default IssueLimitResult acquire(VerificationKey key, Instant requestedAt) throws IssueRateLimitException {
        return acquire(IssueContext.of(key), requestedAt);
    }
}
