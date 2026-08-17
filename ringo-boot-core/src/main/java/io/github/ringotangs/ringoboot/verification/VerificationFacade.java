package io.github.ringotangs.ringoboot.verification;

import java.time.Instant;

/**
 * 为业务用例提供验证码签发和一次性校验的高层契约。
 *
 *
 * <p><strong>API 注意事项：</strong> 同时启用多个验证码渠道时，Spring 容器中会存在多个此接口的实现。普通业务应注入
 *     {@code EmailVerificationFacade} 或 {@code SmsVerificationFacade} 等具体渠道接口；通用编排可以注入
 *     集合或 Map。
 */
public interface VerificationFacade {

    /**
     * 签发并派发验证码。
     *
     *
     * @param namespace 业务命名空间
     * @param purpose 验证用途
     * @param subject 验证主体
     * @return 验证码过期时间
     * @throws VerificationThrottledException 当签发频率限制尚未解除时
     */
    Instant issue(String namespace, String purpose, String subject);

    /**
     * 校验并一次性消费验证码。
     *
     *
     * @param namespace 业务命名空间
     * @param purpose 验证用途
     * @param subject 验证主体
     * @param code 待校验验证码
     * @throws InvalidVerificationCodeException 当验证码未成功通过校验时
     */
    void verify(String namespace, String purpose, String subject, String code);
}
