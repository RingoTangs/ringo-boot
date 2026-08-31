package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.core.KebabCase;
import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * 按客户端 IP 地址限制指定业务用途的验证码签发次数。
 *
 * <p>该规则匹配指定 namespace、purpose 和 channel，额度桶由 namespace、purpose、channel 和
 * {@value #ATTRIBUTE_NAME} 属性组成。因此，同一业务用途下不同验证主体从同一客户端 IP 发起的请求会共享额度。
 *
 * <p>规则只消费已经写入 {@link IssueContext} 的客户端 IP，不负责解析代理请求头、校验 IP 格式或规范化 IPv6 地址。
 *
 * @param namespace 需要限制的业务命名空间
 * @param purpose   需要限制的验证码用途
 * @param channel   需要限制的验证码渠道
 * @param maxIssues 每个客户端 IP 在滚动窗口内允许签发的最大次数
 * @param window    滚动窗口长度
 */
public record ClientIpQuotaRule(
        String namespace, String purpose, VerificationChannel channel, int maxIssues, Duration window)
        implements IssueLimitRule {

    /**
     * 客户端 IP 地址在 {@link IssueContext#attributes()} 中使用的属性名。
     */
    public static final String ATTRIBUTE_NAME = "client-ip";

    /**
     * 创建并校验客户端 IP 配额规则。
     *
     * @throws NullPointerException     当业务范围、渠道或窗口为 {@code null} 时
     * @throws IllegalArgumentException 当规则定义非法时
     */
    public ClientIpQuotaRule {
        Objects.requireNonNull(channel, "channel must not be null");
        KebabCase.validate("namespace", namespace);
        KebabCase.validate("purpose", purpose);
        IssueLimitValidator.validateRuleDefinition(maxIssues, window);
    }

    /**
     * 创建客户端 IP 配额规则 Builder。
     *
     * @return 尚未配置任何字段的 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String id() {
        return IssueLimitRuleId.clientIpQuota(namespace, purpose, channel, maxIssues, window);
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
        String clientIp = context.attribute(ATTRIBUTE_NAME)
                .orElseThrow(() ->
                        new IllegalStateException("required issue context attribute is missing: " + ATTRIBUTE_NAME));
        return IssueLimitBucket.of(namespace, purpose, channel.value(), clientIp);
    }

    /**
     * 使用具名配置项构建客户端 IP 配额规则。
     */
    public static final class Builder {

        private @Nullable String namespace;
        private @Nullable String purpose;
        private @Nullable VerificationChannel channel;
        private @Nullable Integer maxIssues;
        private @Nullable Duration window;

        private Builder() {}

        public Builder namespace(String namespace) {
            this.namespace = Objects.requireNonNull(namespace, "namespace must not be null");
            return this;
        }

        public Builder purpose(String purpose) {
            this.purpose = Objects.requireNonNull(purpose, "purpose must not be null");
            return this;
        }

        public Builder channel(VerificationChannel channel) {
            this.channel = Objects.requireNonNull(channel, "channel must not be null");
            return this;
        }

        public Builder maxIssues(int maxIssues) {
            this.maxIssues = maxIssues;
            return this;
        }

        public Builder window(Duration window) {
            this.window = Objects.requireNonNull(window, "window must not be null");
            return this;
        }

        public ClientIpQuotaRule build() {
            return new ClientIpQuotaRule(
                    Objects.requireNonNull(namespace, "namespace must be configured"),
                    Objects.requireNonNull(purpose, "purpose must be configured"),
                    Objects.requireNonNull(channel, "channel must be configured"),
                    Objects.requireNonNull(maxIssues, "maxIssues must be configured"),
                    Objects.requireNonNull(window, "window must be configured"));
        }
    }
}
