package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.VerificationFacade;

/**
 * 短信验证码签发和一次性校验的应用层契约。
 *
 * <p>Application-level contract for issuing and verifying SMS verification codes once.</p>
 *
 * @apiNote 应用可以提供自定义实现以替换默认手机号规范化或业务编排行为。实现必须确保签发和校验使用一致的
 *     subject 规范化规则。 / Applications may provide a custom implementation to replace the
 *     default phone-number normalization or orchestration behavior. Implementations must use
 *     consistent subject normalization for issuance and verification.
 */
public interface SmsVerificationFacade extends VerificationFacade {}
