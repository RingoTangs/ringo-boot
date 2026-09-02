package io.github.ringotangs.ringoboot.verification.context;

import java.util.List;
import java.util.Objects;

/**
 * 按固定顺序组合并执行一组 {@link IssueContextContributor} 的上下文 Manager。
 */
public final class CompositeIssueContextManager implements IssueContextManager {

    private final List<IssueContextContributor> contributors;

    /**
     * 使用指定 Contributor 创建组合式 Manager。
     *
     * @param contributors 按执行顺序排列的上下文贡献器
     */
    public CompositeIssueContextManager(List<IssueContextContributor> contributors) {
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
        IssueContextValidator.requirePreservedContext(expected, actual, source);
        expected.attributes().forEach((name, value) -> {
            if (!value.equals(actual.attributes().get(name))) {
                throw new IllegalArgumentException(source + " must preserve existing issue context attribute: " + name);
            }
        });
        return actual;
    }
}
