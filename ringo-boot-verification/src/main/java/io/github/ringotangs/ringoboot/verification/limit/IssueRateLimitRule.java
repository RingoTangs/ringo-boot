package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 声明一条验证码签发滚动窗口配额规则。
 *
 * <p>规则负责理解业务上下文：先通过 {@link #matches(IssueContext)} 判断是否适用，再通过
 * {@link #bucket(IssueContext)} 计算额度累计身份。规则不负责读写内存、Redis 或其他存储，额度检查和消费由
 * {@link IssueRateLimitStore} 完成。
 *
 * <p>实现必须无状态、线程安全，并且在应用运行期间保持规则 ID、最大次数和窗口不变。Spring Boot 应用可以把实现注册为 Bean，
 * 由 {@link IssueRateLimitManager} 统一收集和执行。
 */
public interface IssueRateLimitRule {

    /**
     * 返回全局唯一且稳定的规则标识。
     *
     * <p>标识必须使用 kebab-case，例如 {@code login-ip-hour}。修改标识会创建新的额度历史，不能用于仅调整展示名称。
     *
     * @return 规则标识
     */
    String id();

    /**
     * 判断规则是否适用于本次签发请求。
     *
     * <p>实现必须明确判断业务适用范围，不应通过返回 {@code false} 静默忽略已适用规则缺失的安全属性。
     *
     * @param context 签发上下文
     * @return 规则适用时返回 {@code true}
     * @throws NullPointerException 当上下文为 {@code null} 且实现不接受空值时
     */
    boolean matches(IssueContext context);

    /**
     * 从签发上下文解析本规则的额度桶。
     *
     * @param context 签发上下文
     * @return 非空额度桶
     * @throws NullPointerException 当上下文为 {@code null}，或者实现依赖的值为 {@code null} 时
     * @throws RuntimeException     当实现无法从上下文解析所需额度桶时
     */
    IssueLimitBucket bucket(IssueContext context);

    /**
     * 返回滚动窗口内允许签发的最大次数。
     *
     * @return 大于零的最大签发次数
     */
    int maxIssues();

    /**
     * 返回滚动窗口长度。
     *
     * @return 大于零的窗口长度
     */
    Duration window();

    /**
     * 创建对所有请求生效的简单规则。
     *
     * @param id             全局唯一的 kebab-case 规则标识
     * @param bucketResolver 额度桶解析函数
     * @param maxIssues      滚动窗口内允许签发的最大次数
     * @param window         滚动窗口长度
     * @return 不可变简单规则
     * @throws NullPointerException     当任一引用参数为 {@code null} 时
     * @throws IllegalArgumentException 当规则标识、最大次数或窗口非法时
     */
    static IssueRateLimitRule of(
            String id, Function<IssueContext, IssueLimitBucket> bucketResolver, int maxIssues, Duration window) {
        return new SimpleIssueRateLimitRule(id, context -> true, bucketResolver, maxIssues, window);
    }

    /**
     * 创建带业务匹配条件的简单规则。
     *
     * @param id             全局唯一的 kebab-case 规则标识
     * @param matcher        业务适用范围判断函数
     * @param bucketResolver 额度桶解析函数
     * @param maxIssues      滚动窗口内允许签发的最大次数
     * @param window         滚动窗口长度
     * @return 不可变简单规则
     * @throws NullPointerException     当任一引用参数为 {@code null} 时
     * @throws IllegalArgumentException 当规则标识、最大次数或窗口非法时
     */
    static IssueRateLimitRule of(
            String id,
            Predicate<IssueContext> matcher,
            Function<IssueContext, IssueLimitBucket> bucketResolver,
            int maxIssues,
            Duration window) {
        return new SimpleIssueRateLimitRule(id, matcher, bucketResolver, maxIssues, window);
    }
}
