package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.VerificationFacade;

/**
 * 邮箱验证码签发和一次性校验的应用层契约。
 *
 * <p>Application-level contract for issuing and verifying email verification codes once.</p>
 *
 * @apiNote 应用可以提供自定义实现以替换默认邮箱地址规范化或业务编排行为。实现必须确保签发和校验使用一致的
 *     subject 规范化规则。 / Applications may provide a custom implementation to replace the
 *     default email normalization or orchestration behavior. Implementations must use consistent
 *     subject normalization for issuance and verification.
 */
public interface EmailVerificationFacade extends VerificationFacade {}
