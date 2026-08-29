package io.github.ringotangs.ringoboot.verification;

/** 验证码签发上下文中由框架定义的标准属性名。 */
public final class IssueContextAttributes {

    /** 已由应用或可信代理基础设施解析并规范化的客户端来源地址。 */
    public static final String CLIENT_ADDRESS = "client-address";

    private IssueContextAttributes() {}
}
