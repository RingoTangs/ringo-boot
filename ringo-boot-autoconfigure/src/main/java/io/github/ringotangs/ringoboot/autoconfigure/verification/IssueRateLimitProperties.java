package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.limit.GlobalIssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 验证码签发频率限制的自动配置属性。 */
@ConfigurationProperties(IssueRateLimitProperties.PREFIX)
public class IssueRateLimitProperties {

    private static final Pattern KEY_SEGMENT_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    /** 配置属性前缀。 */
    public static final String PREFIX = VerificationProperties.PREFIX + ".issue-rate-limit";

    /** 同一验证码键的最小签发间隔，默认为 60 秒；设置为零可显式关闭限制。 */
    private Duration interval = Duration.ofSeconds(60);

    /** 通过配置声明的标准签发额度规则。 */
    private List<Rule> rules = new ArrayList<>();

    /**
     * 返回同一验证码键的最小签发间隔。
     *
     * @return 最小签发间隔
     */
    public Duration getInterval() {
        return interval;
    }

    /**
     * 设置同一验证码键的最小签发间隔。
     *
     * @param interval 最小签发间隔，不得为负数
     */
    public void setInterval(Duration interval) {
        this.interval = interval;
    }

    /**
     * 返回通过配置声明的标准签发额度规则。
     *
     * @return 配置规则列表
     */
    public List<Rule> getRules() {
        return rules;
    }

    /**
     * 设置通过配置声明的标准签发额度规则。
     *
     * @param rules 配置规则列表
     */
    public void setRules(List<Rule> rules) {
        this.rules = Objects.requireNonNull(rules, "rules must not be null");
    }

    List<IssueRateLimitRule> toRules() {
        return rules.stream().map(Rule::toRule).toList();
    }

    /** YAML 标准额度规则支持的分桶范围。 */
    public enum Scope {
        /** 当前限流状态存储隔离范围内的全部签发请求共享额度。 */
        GLOBAL,

        /** 精确匹配指定 namespace，并按 namespace 共享额度。 */
        NAMESPACE,

        /** 精确匹配指定 namespace 和 purpose，并按两者共享额度。 */
        PURPOSE,

        /** 精确匹配指定 namespace 和 purpose，并按每个 subject 独立累计额度。 */
        SUBJECT
    }

    /** 一条通过 YAML 声明的标准签发额度规则。 */
    public static class Rule {

        /** 全局唯一且稳定的 kebab-case 规则标识。 */
        private @Nullable String id;

        /** 规则的匹配和分桶范围。 */
        private @Nullable Scope scope;

        /** namespace 或更细范围规则需要精确匹配的业务命名空间。 */
        private @Nullable String namespace;

        /** purpose 或 subject 范围规则需要精确匹配的验证用途。 */
        private @Nullable String purpose;

        /** 滚动窗口内允许签发的最大次数。 */
        private int maxIssues;

        /** 滚动窗口长度。 */
        private @Nullable Duration window;

        /** @return 规则标识 */
        public @Nullable String getId() {
            return id;
        }

        /** @param id 规则标识 */
        public void setId(@Nullable String id) {
            this.id = id;
        }

        /** @return 规则范围 */
        public @Nullable Scope getScope() {
            return scope;
        }

        /** @param scope 规则范围 */
        public void setScope(@Nullable Scope scope) {
            this.scope = scope;
        }

        /** @return 需要匹配的业务命名空间 */
        public @Nullable String getNamespace() {
            return namespace;
        }

        /** @param namespace 需要匹配的业务命名空间 */
        public void setNamespace(@Nullable String namespace) {
            this.namespace = namespace;
        }

        /** @return 需要匹配的验证用途 */
        public @Nullable String getPurpose() {
            return purpose;
        }

        /** @param purpose 需要匹配的验证用途 */
        public void setPurpose(@Nullable String purpose) {
            this.purpose = purpose;
        }

        /** @return 滚动窗口内允许签发的最大次数 */
        public int getMaxIssues() {
            return maxIssues;
        }

        /** @param maxIssues 滚动窗口内允许签发的最大次数 */
        public void setMaxIssues(int maxIssues) {
            this.maxIssues = maxIssues;
        }

        /** @return 滚动窗口长度 */
        public @Nullable Duration getWindow() {
            return window;
        }

        /** @param window 滚动窗口长度 */
        public void setWindow(@Nullable Duration window) {
            this.window = window;
        }

        private IssueRateLimitRule toRule() {
            String ruleId = Objects.requireNonNull(id, "configured issue rate limit rule id must not be null");
            Scope ruleScope =
                    Objects.requireNonNull(scope, "configured issue rate limit rule scope must not be null: " + ruleId);
            Duration ruleWindow = Objects.requireNonNull(
                    window, "configured issue rate limit rule window must not be null: " + ruleId);
            return switch (ruleScope) {
                case GLOBAL -> globalRule(ruleId, ruleWindow);
                case NAMESPACE -> namespaceRule(ruleId, ruleWindow);
                case PURPOSE -> purposeRule(ruleId, ruleWindow);
                case SUBJECT -> subjectRule(ruleId, ruleWindow);
            };
        }

        private IssueRateLimitRule globalRule(String ruleId, Duration ruleWindow) {
            requireAbsent("namespace", namespace, ruleId, Scope.GLOBAL);
            requireAbsent("purpose", purpose, ruleId, Scope.GLOBAL);
            return new GlobalIssueRateLimitRule(ruleId, maxIssues, ruleWindow);
        }

        private IssueRateLimitRule namespaceRule(String ruleId, Duration ruleWindow) {
            String requiredNamespace = requireSegment("namespace", namespace, ruleId);
            requireAbsent("purpose", purpose, ruleId, Scope.NAMESPACE);
            return IssueRateLimitRule.of(
                    ruleId,
                    context -> context.key().namespace().equals(requiredNamespace),
                    context -> IssueLimitBucket.of(context.key().namespace()),
                    maxIssues,
                    ruleWindow);
        }

        private IssueRateLimitRule purposeRule(String ruleId, Duration ruleWindow) {
            String requiredNamespace = requireSegment("namespace", namespace, ruleId);
            String requiredPurpose = requireSegment("purpose", purpose, ruleId);
            return IssueRateLimitRule.of(
                    ruleId,
                    context -> context.key().namespace().equals(requiredNamespace)
                            && context.key().purpose().equals(requiredPurpose),
                    context -> IssueLimitBucket.of(
                            context.key().namespace(), context.key().purpose()),
                    maxIssues,
                    ruleWindow);
        }

        private IssueRateLimitRule subjectRule(String ruleId, Duration ruleWindow) {
            String requiredNamespace = requireSegment("namespace", namespace, ruleId);
            String requiredPurpose = requireSegment("purpose", purpose, ruleId);
            return IssueRateLimitRule.of(
                    ruleId,
                    context -> context.key().namespace().equals(requiredNamespace)
                            && context.key().purpose().equals(requiredPurpose),
                    context -> IssueLimitBucket.of(
                            context.key().namespace(),
                            context.key().purpose(),
                            context.key().subject()),
                    maxIssues,
                    ruleWindow);
        }

        private static String requireSegment(String name, @Nullable String value, String ruleId) {
            Objects.requireNonNull(value, "configured issue rate limit rule " + name + " must not be null: " + ruleId);
            if (!KEY_SEGMENT_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException(
                        "configured issue rate limit rule " + name + " must be lowercase kebab-case: " + ruleId);
            }
            return value;
        }

        private static void requireAbsent(String name, @Nullable String value, String ruleId, Scope ruleScope) {
            if (value != null) {
                throw new IllegalArgumentException(
                        "configured issue rate limit rule " + name + " must not be configured for scope "
                                + ruleScope.name().toLowerCase() + ": " + ruleId);
            }
        }
    }
}
