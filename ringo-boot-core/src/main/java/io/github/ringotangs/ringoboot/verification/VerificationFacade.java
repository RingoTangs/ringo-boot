package io.github.ringotangs.ringoboot.verification;

import java.time.Instant;

/**
 * 为业务用例提供验证码签发和一次性校验的高层契约。
 *
 * <p>High-level contract for issuing and verifying one-time verification codes in business use
 * cases.</p>
 *
 * @apiNote 同时启用多个验证码渠道时，Spring 容器中会存在多个此接口的实现。普通业务应注入
 *     {@code EmailVerificationFacade} 或 {@code SmsVerificationFacade} 等具体渠道接口；通用编排可以注入
 *     集合或 Map。 / When multiple verification channels are enabled, the Spring container has
 *     multiple implementations of this interface. Regular business code should inject a
 *     channel-specific interface such as {@code EmailVerificationFacade} or {@code
 *     SmsVerificationFacade}; generic orchestration may inject a collection or map.
 */
public interface VerificationFacade {

    /**
     * 签发并派发验证码。
     *
     * <p>Issues and delivers a verification code.</p>
     *
     * @param namespace 业务命名空间 / business namespace
     * @param purpose 验证用途 / verification purpose
     * @param subject 验证主体 / verification subject
     * @return 验证码过期时间 / code expiration instant
     * @throws VerificationThrottledException 当重发间隔尚未结束时 / if the resend interval has not elapsed
     */
    Instant issue(String namespace, String purpose, String subject);

    /**
     * 校验并一次性消费验证码。
     *
     * <p>Verifies and consumes a verification code once.</p>
     *
     * @param namespace 业务命名空间 / business namespace
     * @param purpose 验证用途 / verification purpose
     * @param subject 验证主体 / verification subject
     * @param code 待校验验证码 / verification code to check
     * @throws InvalidVerificationCodeException 当验证码未成功通过校验时 / if verification is unsuccessful
     */
    void verify(String namespace, String purpose, String subject, String code);
}
