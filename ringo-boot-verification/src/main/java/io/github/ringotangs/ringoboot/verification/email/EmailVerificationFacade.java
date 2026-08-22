package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.VerificationFacade;

/**
 * 邮箱验证码签发和一次性校验的应用层契约。
 *
 *
 * <p><strong>API 注意事项：</strong> 应用可以提供自定义实现以替换默认邮箱地址规范化或业务编排行为。实现必须确保签发和校验使用一致的
 *     验证主体规范化规则。
 */
public interface EmailVerificationFacade extends VerificationFacade {}
