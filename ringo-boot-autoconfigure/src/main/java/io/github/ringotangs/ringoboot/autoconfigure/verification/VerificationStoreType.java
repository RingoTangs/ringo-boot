package io.github.ringotangs.ringoboot.autoconfigure.verification;

/**
 * 验证码状态存储类型。
 *
 * <p>Verification state storage type.</p>
 */
public enum VerificationStoreType {
    /** 仅适用于本地、测试和单实例应用的进程内存储。 / In-process storage for local, test, and single-instance use. */
    MEMORY,

    /** 适用于多实例生产部署的 Redis 存储。 / Redis storage for multi-instance production deployments. */
    REDIS
}
