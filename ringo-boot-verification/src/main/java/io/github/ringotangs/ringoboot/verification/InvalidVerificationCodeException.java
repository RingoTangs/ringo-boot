package io.github.ringotangs.ringoboot.verification;

/**
 * 验证码无法通过校验时抛出的业务异常。
 *
 *
 * <p><strong>API 注意事项：</strong> 此异常有意隐藏验证码不存在、过期、不匹配或尝试次数耗尽等内部原因，避免调用方泄露验证状态。
 */
public final class InvalidVerificationCodeException extends VerificationException {

    /**
     * 创建验证码无效异常。
     */
    public InvalidVerificationCodeException() {
        super("The verification code is invalid");
    }
}
