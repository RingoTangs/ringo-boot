package io.github.ringotangs.ringoboot.verification;

/**
 * 为验证码签发上下文补充当前执行环境中的信息。
 *
 * <p>实现可以采集 IP、设备、账号或租户等信息，但不应执行额度检查。返回结果必须保留传入上下文的验证码键和渠道。
 */
@FunctionalInterface
public interface IssueContextResolver {

    /**
     * 解析并补充签发上下文。
     *
     * @param context 签发服务创建的基础上下文
     * @return 补充环境信息后的上下文
     * @throws RuntimeException 当当前执行环境无法提供所需信息时
     */
    IssueContext resolve(IssueContext context);
}
