package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Instant;
import java.util.List;

/** 原子检查并消费一组已经解析的验证码签发限流约束。 */
@FunctionalInterface
public interface IssueRateLimitBackend {

    /**
     * 原子获取全部约束对应的一次签发名额。
     *
     * <p>只有所有约束均允许时才能消费额度；任一约束受限时不得消费其他约束。
     */
    IssueLimitResult acquire(List<IssueRateLimitConstraint> constraints, Instant requestedAt)
            throws IssueRateLimitException;
}
