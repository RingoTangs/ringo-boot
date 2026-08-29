package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.IssueContextAttributes;
import io.github.ringotangs.ringoboot.verification.IssueContextContributor;
import java.util.List;
import java.util.Objects;

/** 在客户端地址规则适用于当前业务时补充规范化来源地址。 */
public final class ClientAddressIssueContextContributor implements IssueContextContributor {

    private final List<ClientAddressIssueQuotaRule> rules;
    private final ClientAddressResolver resolver;

    /** 使用客户端地址规则和解析器创建 Contributor。 */
    public ClientAddressIssueContextContributor(
            List<ClientAddressIssueQuotaRule> rules, ClientAddressResolver resolver) {
        Objects.requireNonNull(rules, "rules must not be null");
        this.rules = List.copyOf(rules);
        if (this.rules.isEmpty()) {
            throw new IllegalArgumentException("at least one client address issue quota rule is required");
        }
        this.rules.forEach(rule -> Objects.requireNonNull(rule, "rule must not be null"));
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    }

    @Override
    public IssueContext contribute(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        if (rules.stream().noneMatch(rule -> rule.matches(context))) {
            return context;
        }
        return context.with(IssueContextAttributes.CLIENT_ADDRESS, resolver.resolve());
    }
}
