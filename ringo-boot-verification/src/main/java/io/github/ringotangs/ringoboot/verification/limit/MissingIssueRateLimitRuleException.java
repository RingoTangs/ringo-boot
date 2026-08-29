package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationKey;

import java.util.Objects;

/**
 * 表示限流管理器没有配置规则，或者验证码签发请求没有任何规则覆盖。
 *
 * <p>该异常表示服务端限流配置缺失，不是正常达到额度上限。正常超限使用 {@link IssueLimitResult.Throttled} 表达。基于验证码键创建
 * 异常时，诊断消息不会包含可能为邮箱或手机号的 subject。
 */
public final class MissingIssueRateLimitRuleException extends IssueRateLimitException {

    /**
     * 创建一个表示限流管理器没有配置任何规则的异常。
     *
     * <p>该构造器通常由 {@link IssueRateLimitManager} 在创建阶段使用。
     */
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

    /**
     * 构建不包含验证主体的安全诊断消息。
     *
     * @param key 没有限流规则覆盖的验证码键
     * @return 只包含命名空间和验证用途的诊断消息
     */
    private static String message(VerificationKey key) {
        return "no issue rate limit rule matches namespace=" + key.namespace() + ", purpose=" + key.purpose();
    }
}
