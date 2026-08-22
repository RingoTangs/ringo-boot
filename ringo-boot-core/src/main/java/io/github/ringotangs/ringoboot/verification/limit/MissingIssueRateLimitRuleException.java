package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationException;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.util.Objects;

/** 表示验证码签发请求没有任何限流规则覆盖。 */
public final class MissingIssueRateLimitRuleException extends VerificationException {

    /** 创建一个表示限流管理器没有配置任何规则的异常。 */
    public MissingIssueRateLimitRuleException() {
        super("at least one issue rate limit rule is required");
    }

    /**
     * 创建一个表示当前验证码业务没有匹配规则的异常。
     *
     * <p>诊断消息只包含业务命名空间和验证用途，不包含可能为邮箱或手机号的 subject。
     *
     * @param key 没有限流规则覆盖的验证码键
     * @throws NullPointerException 当验证码键为 {@code null} 时
     */
    public MissingIssueRateLimitRuleException(VerificationKey key) {
        super(message(Objects.requireNonNull(key, "key must not be null")));
    }

    private static String message(VerificationKey key) {
        return "no issue rate limit rule matches namespace=" + key.namespace() + ", purpose=" + key.purpose();
    }
}
