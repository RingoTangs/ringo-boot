package io.github.ringotangs.ringoboot.autoconfigure.verification;

/** 从当前 Web 请求解析规范化客户端来源地址。 */
@FunctionalInterface
public interface ClientAddressResolver {

    /**
     * 解析当前请求的客户端来源地址。
     *
     * @return 非空、非空白的规范化 IP 地址
     * @throws ClientAddressResolutionException 当前请求或有效地址不可用时
     */
    String resolve() throws ClientAddressResolutionException;
}
