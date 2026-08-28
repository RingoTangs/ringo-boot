package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.core.KebabCase;
import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * 限制同一验证码业务、渠道和接收方的重发频率。
 *
 * <p>该规则适用于任意验证码渠道。每个冷却窗口只允许签发一次，额度桶由 namespace、purpose、channel 和 subject 组成，
 * 因此不同业务、渠道或验证主体不会共享冷却额度。
 *
 * <p>推荐使用 Builder 创建规则，让每个配置项的含义更加清晰：
 *
 * <pre>{@code
 * ResendCooldownRule rule = ResendCooldownRule.builder()
 *         .namespace("account")
 *         .purpose("email-verification")
 *         .channel(VerificationChannel.EMAIL)
 *         .cooldown(Duration.ofMinutes(1))
 *         .build();
 * }</pre>
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
     * 创建重发冷却规则 Builder。
     *
     * @return 一个尚未配置任何字段的 Builder
     */
    public static Builder builder() {
        return new Builder();
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

    /**
     * 使用具名配置项构建重发冷却规则。
     *
     * <p>所有配置项都是必填项，不提供隐式默认值。Builder 只负责收集参数，最终校验仍由
     * {@link ResendCooldownRule#ResendCooldownRule(String, String, VerificationChannel, Duration)} 统一完成。
     */
    public static final class Builder {

        private @Nullable String namespace;
        private @Nullable String purpose;
        private @Nullable VerificationChannel channel;
        private @Nullable Duration cooldown;

        private Builder() {}

        /**
         * 设置需要限制的业务命名空间。
         *
         * @param namespace 业务命名空间
         * @return 当前 Builder
         * @throws NullPointerException 当命名空间为 {@code null} 时
         */
        public Builder namespace(String namespace) {
            this.namespace = Objects.requireNonNull(namespace, "namespace must not be null");
            return this;
        }

        /**
         * 设置需要限制的验证码用途。
         *
         * @param purpose 验证码用途
         * @return 当前 Builder
         * @throws NullPointerException 当验证码用途为 {@code null} 时
         */
        public Builder purpose(String purpose) {
            this.purpose = Objects.requireNonNull(purpose, "purpose must not be null");
            return this;
        }

        /**
         * 设置需要限制的验证码渠道。
         *
         * @param channel 验证码渠道
         * @return 当前 Builder
         * @throws NullPointerException 当验证码渠道为 {@code null} 时
         */
        public Builder channel(VerificationChannel channel) {
            this.channel = Objects.requireNonNull(channel, "channel must not be null");
            return this;
        }

        /**
         * 设置两次签发之间的最短间隔。
         *
         * @param cooldown 重发冷却时间
         * @return 当前 Builder
         * @throws NullPointerException 当冷却时间为 {@code null} 时
         */
        public Builder cooldown(Duration cooldown) {
            this.cooldown = Objects.requireNonNull(cooldown, "cooldown must not be null");
            return this;
        }

        /**
         * 使用当前配置创建重发冷却规则。
         *
         * @return 完整并经过校验的重发冷却规则
         * @throws NullPointerException 当存在未配置的必填项时
         * @throws IllegalArgumentException 当命名空间、用途或冷却时间非法时
         */
        public ResendCooldownRule build() {
            return new ResendCooldownRule(
                    Objects.requireNonNull(namespace, "namespace must be configured"),
                    Objects.requireNonNull(purpose, "purpose must be configured"),
                    Objects.requireNonNull(channel, "channel must be configured"),
                    Objects.requireNonNull(cooldown, "cooldown must be configured"));
        }
    }
}
