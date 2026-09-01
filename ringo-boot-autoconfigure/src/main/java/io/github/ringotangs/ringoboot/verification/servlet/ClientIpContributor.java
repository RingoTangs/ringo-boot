package io.github.ringotangs.ringoboot.verification.servlet;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.IssueContextContributor;
import io.github.ringotangs.ringoboot.verification.limit.ClientIpQuotaRule;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 将当前 Servlet 请求的客户端 IP 地址添加到验证码签发上下文。
 *
 * <p>该 Contributor 在每次贡献上下文时获取当前请求，因此可以作为单例使用。它只使用
 * {@link HttpServletRequest#getRemoteAddr()}，不会自行解析或信任 {@code Forwarded}、{@code X-Forwarded-For}
 * 等代理请求头。使用反向代理时，应用必须通过可信的 Servlet 基础设施配置远端地址解析。
 */
public final class ClientIpContributor implements IssueContextContributor {

    /**
     * 客户端 IP 地址在 {@link IssueContext#attributes()} 中使用的属性名。
     */
    public static final String ATTRIBUTE_NAME = ClientIpQuotaRule.ATTRIBUTE_NAME;

    private final ObjectProvider<HttpServletRequest> requestProvider;

    /**
     * 使用 Servlet 请求 Provider 创建 Contributor。
     *
     * @param requestProvider 按调用解析当前 Servlet 请求的 Provider
     * @throws NullPointerException 当 Provider 为 {@code null} 时
     */
    public ClientIpContributor(ObjectProvider<HttpServletRequest> requestProvider) {
        this.requestProvider = Objects.requireNonNull(requestProvider, "requestProvider must not be null");
    }

    /**
     * 将当前请求的客户端 IP 地址添加到签发上下文。
     *
     * <p>属性值约束由 {@link IssueContext#with(String, String)} 统一校验；已有属性保护由上下文 Manager 统一执行。
     *
     * @param context 当前签发上下文
     * @return 包含 {@value #ATTRIBUTE_NAME} 属性的新上下文
     */
    @Override
    public IssueContext contribute(IssueContext context) {
        HttpServletRequest request = requestProvider.getObject();
        return context.with(ATTRIBUTE_NAME, request.getRemoteAddr());
    }
}
