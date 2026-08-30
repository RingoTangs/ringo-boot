package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import java.time.Duration;

/**
 * 声明一条验证码签发滚动窗口配额规则。
 *
 * <p>规则负责理解业务上下文：先通过 {@link #appliesTo(IssueContext)} 判断是否适用，再通过
 * {@link #bucket(IssueContext)} 计算额度累计身份。规则不负责读写内存、Redis 或其他存储，额度检查和消费由
 * {@link IssueLimitStore} 完成。
 *
 * <p>实现必须无状态、线程安全，并且在应用运行期间保持规则 ID、最大次数和窗口不变。Spring Boot 应用可以把实现注册为 Bean，
 * 由 {@link IssueLimitManager} 统一收集和执行。
 */
public interface IssueLimitRule {

    /**
     * 返回全局唯一且稳定的规则标识。
     *
     * <p>标识必须使用 kebab-case，例如 {@code login-ip-hour}。修改标识会创建新的额度历史，不能用于仅调整展示名称。
     *
     * @return 规则标识
     */
    String id();

    /**
     * 判断规则配置的业务范围是否适用于本次签发请求。
     *
     * <p>该方法只负责 namespace、purpose、channel 等规则适用范围，不负责解析 subject、客户端 IP 等运行时额度身份。实现不应通过
     * 返回 {@code false} 静默忽略已适用规则缺失的安全属性；这些属性应由 {@link #bucket(IssueContext)} 强制解析并在缺失时失败。
     *
     * @param context 签发上下文
     * @return 规则适用时返回 {@code true}
     * @throws NullPointerException 当上下文为 {@code null} 且实现不接受空值时
     */
    boolean appliesTo(IssueContext context);

    /**
     * 从签发上下文解析已适用规则的运行时额度桶。
     *
     * <p>{@link IssueLimitManager} 只会在 {@link #appliesTo(IssueContext)} 返回 {@code true} 后调用该方法。
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
}
