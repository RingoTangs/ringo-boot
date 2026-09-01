package io.github.ringotangs.ringoboot.verification.context;

/**
 * 统一准备验证码签发流程使用的最终上下文。
 */
@FunctionalInterface
public interface IssueContextManager {

    /**
     * 使用基础上下文生成签发流程使用的最终上下文。
     *
     * <p>实现不得修改验证码键、渠道或策略，也不得返回 {@code null}。
     *
     * @param context 当前基础签发上下文
     * @return 补充请求级属性后的签发上下文
     */
    IssueContext enrich(IssueContext context);
}
