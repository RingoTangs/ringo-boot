package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Instant;

/**
 * 原子地限制同一验证码键的签发频率。
 *
 * <p><strong>实现要求：</strong>获得名额即视为消耗，无论后续生成、存储或派发是否成功都不应自动退还。
 * 分布式实现必须保证跨进程的原子获取语义。
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
