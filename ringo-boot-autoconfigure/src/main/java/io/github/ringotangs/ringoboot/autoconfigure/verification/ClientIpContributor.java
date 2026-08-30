package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.IssueContextContributor;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Objects;

/**
 * 将当前 Servlet 请求的客户端 IP 地址添加到验证码签发上下文。
 *
 * <p>该 Contributor 只使用 {@link HttpServletRequest#getRemoteAddr()}，不会自行解析或信任 {@code Forwarded}、
 * {@code X-Forwarded-For} 等代理请求头。使用反向代理时，应用必须通过可信的 Servlet 基础设施配置远端地址解析。
 */
public final class ClientIpContributor implements IssueContextContributor {

    /**
     * 客户端 IP 地址在 {@link IssueContext#attributes()} 中使用的属性名。
     */
    public static final String ATTRIBUTE_NAME = "client-ip";

    private final HttpServletRequest request;

    /**
     * 使用当前 Servlet 请求创建 Contributor。
     *
     * @param request 当前请求；在单例 Bean 中通常由 Spring 注入请求代理
     * @throws NullPointerException 当请求为 {@code null} 时
     */
    public ClientIpContributor(HttpServletRequest request) {
        this.request = Objects.requireNonNull(request, "request must not be null");
    }

    /**
     * 将当前请求的客户端 IP 地址添加到签发上下文。
     *
     * @param context 当前签发上下文
     * @return 包含 {@value #ATTRIBUTE_NAME} 属性的新上下文
     * @throws NullPointerException     当上下文或请求返回的远端地址为 {@code null} 时
     * @throws IllegalArgumentException 当上下文已经包含客户端 IP 属性时
     * @throws IllegalStateException    当请求返回的远端地址为空白时
     */
    @Override
    public IssueContext contribute(IssueContext context) {
        Objects.requireNonNull(context, "context must not be null");
        if (context.attributes().containsKey(ATTRIBUTE_NAME)) {
            throw new IllegalArgumentException("issue context attribute already exists: " + ATTRIBUTE_NAME);
        }
        String clientIp = Objects.requireNonNull(request.getRemoteAddr(), "request remote address must not be null")
                .trim();
        if (clientIp.isEmpty()) {
            throw new IllegalStateException("request remote address must not be blank");
        }
        return context.with(ATTRIBUTE_NAME, clientIp);
    }
}
