package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Instant;

/**
 * 原子地限制验证码的签发频率。
 *
 * <p><strong>实现要求：</strong>一次获取涉及多条规则时，必须先检查全部规则；只有全部规则允许时，才能同时消费所有规则的额度。
 * 正常受限的请求不得消费任何规则的额度。获得名额后即视为消耗，无论后续生成、存储或派发是否成功都不应自动退还。
 * 分布式实现必须保证跨进程的原子获取语义。
 *
 * <p>当多条规则同时受限时，返回的剩余等待时间应为所有受限规则中最大的值，表示全部规则再次允许签发所需的时间。
 */
@FunctionalInterface
public interface IssueRateLimiter {

    /**
     * 尝试获取一次验证码签发名额。
     *
     * @param key 验证码键
     * @param requestedAt 请求签发的时间
     * @return 允许签发或受限结果
     * @throws NullPointerException 当任一参数为 {@code null} 时
     * @throws IssueRateLimitException 当底层限流操作失败时
     */
    IssueLimitResult acquire(VerificationKey key, Instant requestedAt) throws IssueRateLimitException;
}
