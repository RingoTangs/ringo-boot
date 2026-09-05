package io.github.ringotangs.ringoboot.verification;

import java.util.Objects;

/**
 * 验证码校验未通过时抛出的业务异常。
 *
 * <p><strong>API 注意事项：</strong> {@link #result()} 保留内部校验结果，供诊断、审计或统计使用；面向最终用户的响应仍应隐藏验证码不存在、过期、不匹配或尝试次数耗尽等差异。
 */
public final class VerificationRejectedException extends VerificationException {

    private final VerifyResult result;

    /**
     * 创建验证码校验拒绝异常。
     *
     * @param result 非成功的校验结果
     * @throws NullPointerException 当结果为 {@code null} 时
     * @throws IllegalArgumentException 当结果为 {@link VerifyResult#SUCCESS} 时
     */
    public VerificationRejectedException(VerifyResult result) {
        super("The verification code is invalid");
        this.result = Objects.requireNonNull(result, "result must not be null");
        if (result == VerifyResult.SUCCESS) {
            throw new IllegalArgumentException("result must not be SUCCESS");
        }
    }

    /**
     * 返回内部校验结果。
     *
     * @return 非成功的校验结果
     */
    public VerifyResult result() {
        return result;
    }
}
