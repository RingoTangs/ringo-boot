package io.github.ringotangs.ringoboot.verification.context;

/**
 * 为验证码签发流程补充请求级上下文属性。
 *
 * <p>Contributor 应当无状态、线程安全，只能向上下文增加属性，不能修改验证码键、渠道、策略或删除、覆盖已有属性。默认组合实现
 * {@link CompositeIssueContextManager} 会在每次调用后校验这些约束。
 */
@FunctionalInterface
public interface IssueContextContributor {

    /**
     * 为当前签发上下文补充属性。
     *
     * @param context 当前不可变签发上下文
     * @return 增加属性后的新上下文，或者未发生变化时返回原上下文
     */
    IssueContext contribute(IssueContext context);
}
