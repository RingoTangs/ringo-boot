package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** 收集、匹配和解析签发限流规则，并统一提交给限流后端。 */
public final class IssueRateLimitManager implements IssueRateLimiter {

    private static final Pattern RULE_ID_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final IssueLimitResult.Allowed ALLOWED = new IssueLimitResult.Allowed();

    private final List<IssueRateLimitRule> rules;
    private final IssueRateLimitBackend backend;

    /** 使用规则集合和原子执行后端创建管理器。 */
    public IssueRateLimitManager(List<IssueRateLimitRule> rules, IssueRateLimitBackend backend) {
        Objects.requireNonNull(rules, "rules must not be null");
        this.backend = Objects.requireNonNull(backend, "backend must not be null");
        this.rules = List.copyOf(rules);
        Set<String> ids = new HashSet<>();
        for (IssueRateLimitRule rule : this.rules) {
            Objects.requireNonNull(rule, "rule must not be null");
            validateRuleDefinition(rule.id(), rule.maxIssues(), rule.window());
            if (!ids.add(rule.id())) {
                throw new IllegalArgumentException("duplicate issue rate limit rule id: " + rule.id());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public IssueLimitResult acquire(IssueContext context, Instant requestedAt) throws IssueRateLimitException {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        List<IssueRateLimitConstraint> constraints = new ArrayList<>();
        for (IssueRateLimitRule rule : rules) {
            if (rule.matches(context)) {
                IssueLimitBucket bucket = Objects.requireNonNull(
                        rule.bucket(context), "issue rate limit rule bucket must not be null: " + rule.id());
                constraints.add(new IssueRateLimitConstraint(rule.id(), bucket, rule.maxIssues(), rule.window()));
            }
        }
        if (constraints.isEmpty()) {
            return ALLOWED;
        }
        return Objects.requireNonNull(
                backend.acquire(List.copyOf(constraints), requestedAt),
                "issue rate limit backend result must not be null");
    }

    static void validateRuleDefinition(String id, int maxIssues, Duration window) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(window, "window must not be null");
        if (!RULE_ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("rule id must be kebab-case: " + id);
        }
        if (maxIssues <= 0) {
            throw new IllegalArgumentException("maxIssues must be greater than 0: " + maxIssues);
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive: " + window);
        }
    }
}
