package io.github.ringotangs.ringoboot.verification;

import java.util.List;
import java.util.Objects;

/** 按固定顺序执行一组 {@link IssueContextContributor} 的默认上下文 Manager。 */
public final class DefaultIssueContextManager implements IssueContextManager {

    private final List<IssueContextContributor> contributors;

    /**
     * 使用指定 Contributor 创建 Manager。
     *
     * @param contributors 按执行顺序排列的上下文贡献器
     */
    public DefaultIssueContextManager(List<IssueContextContributor> contributors) {
        Objects.requireNonNull(contributors, "contributors must not be null");
        contributors.forEach(contributor -> Objects.requireNonNull(contributor, "contributor must not be null"));
        this.contributors = List.copyOf(contributors);
    }

    @Override
    public IssueContext enrich(IssueContext context) {
        IssueContext enriched = Objects.requireNonNull(context, "context must not be null");
        for (int index = 0; index < contributors.size(); index++) {
            IssueContext contributed = Objects.requireNonNull(
                    contributors.get(index).contribute(enriched),
                    "issue context contributor result must not be null: " + index);
            enriched = requireEnrichedContext(enriched, contributed, "issue context contributor at index " + index);
        }
        return enriched;
    }

    private static IssueContext requireEnrichedContext(IssueContext expected, IssueContext actual, String source) {
        requirePreservedContext(expected, actual, source);
        expected.attributes().forEach((name, value) -> {
            if (!value.equals(actual.attributes().get(name))) {
                throw new IllegalArgumentException(source + " must preserve existing issue context attribute: " + name);
            }
        });
        return actual;
    }

    private static void requirePreservedContext(IssueContext expected, IssueContext actual, String source) {
        if (!actual.key().equals(expected.key())) {
            throw new IllegalArgumentException(source + " must preserve the verification key");
        }
        if (!actual.channel().equals(expected.channel())) {
            throw new IllegalArgumentException(source + " must preserve the verification channel");
        }
        if (!actual.policy().equals(expected.policy())) {
            throw new IllegalArgumentException(source + " must preserve the verification policy");
        }
    }
}
