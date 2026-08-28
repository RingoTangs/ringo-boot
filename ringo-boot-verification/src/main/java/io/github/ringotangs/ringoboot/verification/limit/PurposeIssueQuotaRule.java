package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.core.KebabCase;
import io.github.ringotangs.ringoboot.verification.IssueContext;

import java.time.Duration;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * 限制同一业务用途内的验证码签发总量。
 *
 * <p>该规则匹配指定 namespace 和 purpose 下的全部渠道和验证主体，额度桶由 namespace 和 purpose 组成。
 *
 * @param id        全局唯一且稳定的规则标识
 * @param namespace 需要限制的业务命名空间
 * @param purpose   需要限制的验证码用途
 * @param maxIssues 滚动窗口内允许签发的最大次数
 * @param window    滚动窗口长度
 */
public record PurposeIssueQuotaRule(String id, String namespace, String purpose, int maxIssues, Duration window)
        implements IssueRateLimitRule {

    /**
     * 创建并校验业务用途配额规则。
     *
     * @throws NullPointerException     当规则标识、业务范围或窗口为 {@code null} 时
     * @throws IllegalArgumentException 当规则定义非法时
     */
    public PurposeIssueQuotaRule {
        KebabCase.validate("namespace", namespace);
        KebabCase.validate("purpose", purpose);
        IssueRateLimitValidator.validateRuleDefinition(id, maxIssues, window);
    }

    /**
     * 创建业务用途配额规则 Builder。
     *
     * @return 尚未配置任何字段的 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean matches(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return namespace.equals(context.key().namespace())
                && purpose.equals(context.key().purpose());
    }

    @Override
    public IssueLimitBucket bucket(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return IssueLimitBucket.of(namespace, purpose);
    }

    /**
     * 使用具名配置项构建业务用途配额规则。
     */
    public static final class Builder {

        private @Nullable String id;
        private @Nullable String namespace;
        private @Nullable String purpose;
        private @Nullable Integer maxIssues;
        private @Nullable Duration window;

        private Builder() {
        }

        /**
         * 设置规则标识。
         *
         * @param id 全局唯一且稳定的规则标识
         * @return 当前 Builder
         */
        public Builder id(String id) {
            this.id = Objects.requireNonNull(id, "id must not be null");
            return this;
        }

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
         * @return 完整并经过校验的业务用途配额规则
         */
        public PurposeIssueQuotaRule build() {
            return new PurposeIssueQuotaRule(
                    Objects.requireNonNull(id, "id must be configured"),
                    Objects.requireNonNull(namespace, "namespace must be configured"),
                    Objects.requireNonNull(purpose, "purpose must be configured"),
                    Objects.requireNonNull(maxIssues, "maxIssues must be configured"),
                    Objects.requireNonNull(window, "window must be configured"));
        }
    }
}
