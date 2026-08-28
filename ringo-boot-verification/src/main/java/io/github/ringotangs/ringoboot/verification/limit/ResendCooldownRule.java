package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.core.KebabCase;
import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import java.time.Duration;
import java.util.Objects;

/**
 * 限制同一验证码业务、渠道和接收方的重发频率。
 *
 * <p>该规则适用于任意验证码渠道。每个冷却窗口只允许签发一次，额度桶由 namespace、purpose、channel 和 subject 组成，
 * 因此不同业务、渠道或验证主体不会共享冷却额度。
 *
 * @param namespace 需要限制的业务命名空间
 * @param purpose   需要限制的验证码用途
 * @param channel   需要限制的验证码渠道
 * @param cooldown  两次签发之间的最短间隔
 */
public record ResendCooldownRule(String namespace, String purpose, VerificationChannel channel, Duration cooldown)
        implements IssueRateLimitRule {

    /**
     * 每个冷却窗口固定只允许签发一次，用于保证两次签发之间存在完整的冷却间隔。
     */
    private static final int MAX_ISSUES_PER_COOLDOWN = 1;

    /**
     * 创建并校验重发冷却规则。
     *
     * @throws NullPointerException     当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当 namespace 或 purpose 不是小写 kebab-case，或者冷却时间不为正数时
     */
    public ResendCooldownRule {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(cooldown, "cooldown must not be null");
        KebabCase.validate("namespace", namespace);
        KebabCase.validate("purpose", purpose);
        if (cooldown.isZero() || cooldown.isNegative()) {
            throw new IllegalArgumentException("cooldown must be positive: " + cooldown);
        }
    }

    /**
     * 返回根据业务范围自动生成的稳定规则标识。
     *
     * @return 无分段歧义的 kebab-case 规则标识
     */
    @Override
    public String id() {
        return "resend-cooldown-" + namespace.length() + '-' + namespace + '-' + purpose.length() + '-' + purpose + '-'
                + channel.value();
    }

    /**
     * 判断当前请求是否属于配置的业务和渠道。
     *
     * @param context 签发上下文
     * @return namespace、purpose 和 channel 全部相同时返回 {@code true}
     * @throws NullPointerException 当上下文为 {@code null} 时
     */
    @Override
    public boolean matches(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return namespace.equals(context.key().namespace())
                && purpose.equals(context.key().purpose())
                && channel.equals(context.channel());
    }

    /**
     * 使用完整业务范围和接收方创建冷却额度桶。
     *
     * @param context 已匹配的签发上下文
     * @return namespace、purpose、channel 和 subject 组成的额度桶
     * @throws NullPointerException 当上下文为 {@code null} 时
     */
    @Override
    public IssueLimitBucket bucket(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return IssueLimitBucket.of(
                namespace, purpose, channel.value(), context.key().subject());
    }

    /**
     * 返回每个冷却窗口允许签发的一次额度。
     *
     * <p>该值固定为 {@code 1}。如果一个窗口允许多次签发，请使用普通窗口配额规则；否则同一时刻可能连续签发多次，不再符合重发冷却语义。
     */
    @Override
    public int maxIssues() {
        return MAX_ISSUES_PER_COOLDOWN;
    }

    /**
     * 返回配置的重发冷却时间。
     */
    @Override
    public Duration window() {
        return cooldown;
    }
}
