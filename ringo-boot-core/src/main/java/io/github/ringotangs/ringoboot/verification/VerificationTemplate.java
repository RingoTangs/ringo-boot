package io.github.ringotangs.ringoboot.verification;

import java.util.Objects;

/**
 * 统一编排验证码签发、交付、发送失败补偿和校验流程。
 *
 * <p>Coordinates verification code issuance, delivery, delivery-failure compensation,
 * and verification.</p>
 */
public final class VerificationTemplate {

    private final VerificationService verificationService;

    /**
     * 使用验证码服务创建模板。
     *
     * <p>Creates a template backed by a verification service.</p>
     *
     * @param verificationService 验证码服务 / the verification service
     * @throws NullPointerException 当验证码服务为 {@code null} 时 / if the service is {@code null}
     */
    public VerificationTemplate(VerificationService verificationService) {
        this.verificationService = Objects.requireNonNull(verificationService, "verificationService must not be null");
    }

    /**
     * 使用默认策略签发并交付验证码。
     *
     * <p>Issues and delivers a verification code with the default policy.</p>
     *
     * @param key 验证码键 / the verification key
     * @param sender 验证码发送器 / the code sender
     * @return 安全的交付结果 / the safe delivery result
     */
    public DeliveryResult issue(VerificationKey key, CodeSender sender) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(sender, "sender must not be null");
        return deliver(key, verificationService.issue(key), sender);
    }

    /**
     * 使用指定策略签发并交付验证码。
     *
     * <p>Issues and delivers a verification code with the supplied policy.</p>
     *
     * @param key 验证码键 / the verification key
     * @param policy 验证码策略 / the verification policy
     * @param sender 验证码发送器 / the code sender
     * @return 安全的交付结果 / the safe delivery result
     */
    public DeliveryResult issue(VerificationKey key, VerificationPolicy policy, CodeSender sender) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(sender, "sender must not be null");
        return deliver(key, verificationService.issue(key, policy), sender);
    }

    /**
     * 校验并根据结果消费验证码或扣减尝试次数。
     *
     * <p>Verifies a code and consumes it or decrements its attempts according to the
     * result.</p>
     *
     * @param key 验证码键 / the verification key
     * @param code 待校验验证码 / the code to verify
     * @return 校验结果 / the verification result
     */
    public VerificationResult verify(VerificationKey key, String code) {
        return verificationService.verify(key, code);
    }

    private DeliveryResult deliver(VerificationKey key, IssueResult result, CodeSender sender) {
        return switch (result) {
            case IssueResult.Throttled throttled -> new DeliveryResult.Throttled(throttled.retryAfter());
            case IssueResult.Issued issued -> {
                try {
                    sender.send(new CodeDelivery(key, issued.code(), issued.expiresAt()));
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
