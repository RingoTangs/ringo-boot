package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Instant;
import java.util.List;

/**
 * 原子保存和消费一组已经解析的验证码签发配额。
 *
 * <p>Store 只处理 {@link IssueLimitQuota} 和对应的滚动窗口状态，不保存验证码，也不理解验证码业务字段或执行规则匹配。
 * 单 JVM 实现至少应保证线程安全；分布式实现必须保证跨进程原子性。
 *
 * <p>实现应使用 {@link IssueRateLimitException} 包装 Redis、网络或其他基础设施故障，不应使用它包装参数错误、非法配额或实现违反
 * 接口契约造成的编程错误。
 */
@FunctionalInterface
public interface IssueRateLimitStore {

    /**
     * 原子获取全部配额对应的一次签发名额。
     *
     * <p>只有所有配额均允许时才能同时消费额度；任一配额受限时不得消费其他配额。多条规则同时受限时，应返回其中最大的剩余等待
     * 时间。空配额集合应直接允许且不访问底层存储。
     *
     * @param quotas 本次签发请求需要同时满足的不可变配额集合
     * @param requestedAt 请求签发的时间
     * @return 允许签发或受限结果
     * @throws NullPointerException 当配额集合、任一配额或请求时间为 {@code null} 时
     * @throws IllegalArgumentException 当配额不受当前 Store 支持或同一规则的运行时定义发生变化时
     * @throws IssueRateLimitException 当底层存储或原子操作失败时
     */
    IssueLimitResult acquire(List<IssueLimitQuota> quotas, Instant requestedAt) throws IssueRateLimitException;
}
