package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.core.KebabCase;
import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * 按验证主体限制指定业务用途的验证码签发次数。
 *
 * <p>该规则匹配指定 namespace、purpose 和 channel，额度桶由 namespace、purpose、channel 和运行时 subject 组成。因此不同渠道
 * 或验证主体拥有独立的周期额度。
 *
 * @param namespace 需要限制的业务命名空间
 * @param purpose   需要限制的验证码用途
 * @param channel   需要限制的验证码渠道
 * @param maxIssues 每个验证主体在滚动窗口内允许签发的最大次数
 * @param window    滚动窗口长度
 */
public record SubjectQuotaRule(
        String namespace, String purpose, VerificationChannel channel, int maxIssues, Duration window)
        implements IssueLimitRule {

    /**
     * 创建并校验验证主体配额规则。
     *
     * @throws NullPointerException     当业务范围、渠道或窗口为 {@code null} 时
     * @throws IllegalArgumentException 当规则定义非法时
     */
    public SubjectQuotaRule {
        Objects.requireNonNull(channel, "channel must not be null");
        KebabCase.validate("namespace", namespace);
        KebabCase.validate("purpose", purpose);
        IssueLimitValidator.validateRuleDefinition(maxIssues, window);
    }

    /**
     * 创建验证主体配额规则 Builder。
     *
     * @return 尚未配置任何字段的 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String id() {
        return IssueLimitRuleId.generate("subject-quota", namespace, purpose, channel, maxIssues, window);
    }

    @Override
    public boolean appliesTo(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return namespace.equals(context.key().namespace())
                && purpose.equals(context.key().purpose())
                && channel.equals(context.channel());
    }

    @Override
    public IssueLimitBucket bucket(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return IssueLimitBucket.of(
                namespace, purpose, channel.value(), context.key().subject());
    }

    /**
     * 使用具名配置项构建验证主体配额规则。
     */
    @SuppressWarnings("NullableProblems")
    public static final class Builder {

        private @Nullable String namespace;
        private @Nullable String purpose;
        private @Nullable VerificationChannel channel;
        private @Nullable Integer maxIssues;
        private @Nullable Duration window;

        private Builder() {}

        /**
         * 设置业务命名空间。
         *
         * @param namespace 业务命名空间
         * @return 当前 Builder
         */
        public Builder namespace(String namespace) {
            this.namespace = Objects.requireNonNull(namespace, "namespace must not be null");
            return this;
        }

        /**
         * 设置验证码用途。
         *
         * @param purpose 验证码用途
         * @return 当前 Builder
         */
        public Builder purpose(String purpose) {
            this.purpose = Objects.requireNonNull(purpose, "purpose must not be null");
            return this;
        }

        /**
         * 设置验证码渠道。
         *
         * @param channel 验证码渠道
         * @return 当前 Builder
         */
        public Builder channel(VerificationChannel channel) {
            this.channel = Objects.requireNonNull(channel, "channel must not be null");
            return this;
        }

        /**
         * 设置每个验证主体在窗口内允许签发的最大次数。
         *
         * @param maxIssues 最大签发次数
         * @return 当前 Builder
         */
        public Builder maxIssues(int maxIssues) {
            this.maxIssues = maxIssues;
            return this;
        }

        /**
         * 设置滚动窗口长度。
         *
         * @param window 滚动窗口长度
         * @return 当前 Builder
         */
        public Builder window(Duration window) {
            this.window = Objects.requireNonNull(window, "window must not be null");
            return this;
        }

        /**
         * 使用当前配置创建规则。
         *
         * @return 完整并经过校验的验证主体配额规则
         */
        public SubjectQuotaRule build() {
            return new SubjectQuotaRule(
                    Objects.requireNonNull(namespace, "namespace must be configured"),
                    Objects.requireNonNull(purpose, "purpose must be configured"),
                    Objects.requireNonNull(channel, "channel must be configured"),
                    Objects.requireNonNull(maxIssues, "maxIssues must be configured"),
                    Objects.requireNonNull(window, "window must be configured"));
        }
    }
}
