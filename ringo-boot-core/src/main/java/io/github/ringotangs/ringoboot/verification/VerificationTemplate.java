package io.github.ringotangs.ringoboot.verification;

import java.util.Objects;

/**
 * 统一编排验证码签发、渠道派发和派发失败补偿流程。
 *
 * <p>Coordinates verification code issuance, channel dispatch, and dispatch-failure
 * compensation.</p>
 *
 * @apiNote 子类只需实现 {@link #dispatch(CodeDelivery)} 以完成邮件、短信等渠道的派发。
 *     验证码校验统一通过 {@link VerificationService#verify(VerificationKey, String)} 完成。 /
 *     Subclasses only implement {@link #dispatch(CodeDelivery)} for channels such as
 *     email or SMS. Verification is performed through
 *     {@link VerificationService#verify(VerificationKey, String)}.
 */
public abstract class VerificationTemplate {

    private final VerificationService verificationService;

    /**
     * 使用验证码服务创建渠道模板。
     *
     * <p>Creates a template backed by a verification service.</p>
     *
     * @param verificationService 验证码服务 / the verification service
     * @throws NullPointerException 当验证码服务为 {@code null} 时 / if the service is {@code null}
     */
    protected VerificationTemplate(VerificationService verificationService) {
        this.verificationService = Objects.requireNonNull(verificationService, "verificationService must not be null");
    }

    /**
     * 使用默认策略签发并交付验证码。
     *
     * <p>Issues and delivers a verification code with the default policy.</p>
     *
     * @param key 验证码键 / the verification key
     * @return 安全的交付结果 / the safe delivery result
     */
    public final DeliveryResult issue(VerificationKey key) {
        Objects.requireNonNull(key, "key must not be null");
        return deliver(key, verificationService.issue(key));
    }

    /**
     * 使用指定策略签发并交付验证码。
     *
     * <p>Issues and delivers a verification code with the supplied policy.</p>
     *
     * @param key 验证码键 / the verification key
     * @param policy 验证码策略 / the verification policy
     * @return 安全的交付结果 / the safe delivery result
     */
    public final DeliveryResult issue(VerificationKey key, VerificationPolicy policy) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        return deliver(key, verificationService.issue(key, policy));
    }

    /**
     * 将已签发的验证码派发到具体渠道。
     *
     * <p>Dispatches an issued verification code through the concrete channel.</p>
     *
     * @param delivery 验证码交付内容 / the verification code delivery
     */
    protected abstract void dispatch(CodeDelivery delivery);

    private DeliveryResult deliver(VerificationKey key, IssueResult result) {
        return switch (result) {
            case IssueResult.Throttled throttled -> new DeliveryResult.Throttled(throttled.retryAfter());
            case IssueResult.Issued issued -> {
                try {
                    dispatch(new CodeDelivery(key, issued.code(), issued.expiresAt()));
                } catch (RuntimeException deliveryFailure) {
                    invalidateAfterFailure(key, issued.code(), deliveryFailure);
                    throw deliveryFailure;
                }
                yield new DeliveryResult.Delivered(issued.expiresAt());
            }
        };
    }

    private void invalidateAfterFailure(VerificationKey key, String code, RuntimeException deliveryFailure) {
        try {
            verificationService.invalidate(key, code);
        } catch (RuntimeException invalidationFailure) {
            deliveryFailure.addSuppressed(invalidationFailure);
        }
    }
}
