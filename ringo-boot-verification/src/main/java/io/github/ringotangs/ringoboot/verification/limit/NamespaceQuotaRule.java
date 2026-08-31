package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.core.KebabCase;
import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * 限制同一业务命名空间内的验证码签发总量。
 *
 * <p>该规则匹配指定 namespace 和 channel 下的全部用途和验证主体，额度桶由 namespace 和 channel 组成。适合用作指定渠道的业务模块级成本和突发流量兜底。
 *
 * @param namespace 需要限制的业务命名空间
 * @param channel   需要限制的验证码渠道
 * @param maxIssues 滚动窗口内允许签发的最大次数
 * @param window    滚动窗口长度
 */
public record NamespaceQuotaRule(String namespace, VerificationChannel channel, int maxIssues, Duration window)
        implements IssueLimitRule {

    /**
     * 创建并校验业务命名空间配额规则。
     *
     * @throws NullPointerException     当命名空间、渠道或窗口为 {@code null} 时
     * @throws IllegalArgumentException 当规则定义非法时
     */
    public NamespaceQuotaRule {
        Objects.requireNonNull(channel, "channel must not be null");
        KebabCase.validate("namespace", namespace);
        IssueLimitValidator.validateRuleDefinition(maxIssues, window);
    }

    /**
     * 创建业务命名空间配额规则 Builder。
     *
     * @return 尚未配置任何字段的 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String id() {
        return IssueLimitRuleId.generate("namespace-quota", namespace, null, channel, maxIssues, window);
    }

    @Override
    public boolean appliesTo(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return namespace.equals(context.key().namespace()) && channel.equals(context.channel());
    }

    @Override
    public IssueLimitBucket bucket(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return IssueLimitBucket.of(namespace, channel.value());
    }

    /**
     * 使用具名配置项构建业务命名空间配额规则。
     */
    @SuppressWarnings("NullableProblems")
    public static final class Builder {

        private @Nullable String namespace;
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
         * 设置窗口内允许签发的最大次数。
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
         * @return 完整并经过校验的业务命名空间配额规则
         */
        public NamespaceQuotaRule build() {
            return new NamespaceQuotaRule(
                    Objects.requireNonNull(namespace, "namespace must be configured"),
                    Objects.requireNonNull(channel, "channel must be configured"),
                    Objects.requireNonNull(maxIssues, "maxIssues must be configured"),
                    Objects.requireNonNull(window, "window must be configured"));
        }
    }
}
