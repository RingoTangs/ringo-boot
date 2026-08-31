package io.github.ringotangs.ringoboot.autoconfigure.verification;

/** 可由自动配置选择的验证码状态存储类型。 */
public enum VerificationStoreType {
    /**
     * 使用当前应用进程内的内存存储状态，不在实例之间共享，进程重启后数据丢失。
     */
    MEMORY,

    /**
     * 使用 Redis 共享状态，适用于多实例部署；需要 Spring Data Redis、可用的连接模板和 HMAC 密钥。
     */
    REDIS
}
