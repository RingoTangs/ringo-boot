package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.core.KebabCase;
import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.IssueContextAttributes;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** 限制同一客户端来源地址对指定验证码业务和渠道的签发总量。 */
public record ClientAddressIssueQuotaRule(
        String id, String namespace, String purpose, VerificationChannel channel, int maxIssues, Duration window)
        implements IssueRateLimitRule {

    /** 创建并校验客户端来源地址配额规则。 */
    public ClientAddressIssueQuotaRule {
        Objects.requireNonNull(channel, "channel must not be null");
        KebabCase.validate("namespace", namespace);
        KebabCase.validate("purpose", purpose);
        validateDefinition(id, maxIssues, window);
    }

    /** 创建规则 Builder。 */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean matches(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return namespace.equals(context.key().namespace())
                && purpose.equals(context.key().purpose())
                && channel.equals(context.channel());
    }

    @Override
    public IssueLimitBucket bucket(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        String address = context.attribute(IssueContextAttributes.CLIENT_ADDRESS)
                .orElseThrow(() -> new ClientAddressResolutionException(
                        "required issue context attribute is missing: " + IssueContextAttributes.CLIENT_ADDRESS));
        return IssueLimitBucket.of(namespace, purpose, channel.value(), address);
    }

    private static void validateDefinition(String id, int maxIssues, Duration window) {
        KebabCase.validate("id", id);
        Objects.requireNonNull(window, "window must not be null");
        if (maxIssues <= 0) {
            throw new IllegalArgumentException("maxIssues must be greater than zero");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be greater than zero");
        }
    }

    /** 使用具名配置项构建规则。 */
    public static final class Builder {

        private @Nullable String id;
        private @Nullable String namespace;
        private @Nullable String purpose;
        private @Nullable VerificationChannel channel;
        private @Nullable Integer maxIssues;
        private @Nullable Duration window;

        private Builder() {}

        public Builder id(String id) {
            this.id = Objects.requireNonNull(id, "id must not be null");
            return this;
        }

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

        public ClientAddressIssueQuotaRule build() {
            return new ClientAddressIssueQuotaRule(
                    Objects.requireNonNull(id, "id must be configured"),
                    Objects.requireNonNull(namespace, "namespace must be configured"),
                    Objects.requireNonNull(purpose, "purpose must be configured"),
                    Objects.requireNonNull(channel, "channel must be configured"),
                    Objects.requireNonNull(maxIssues, "maxIssues must be configured"),
                    Objects.requireNonNull(window, "window must be configured"));
        }
    }
}
