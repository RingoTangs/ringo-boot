package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 保存一次验证码签发请求参与限流的业务键和额外来源信号。
 *
 * <p>命名空间、用途和验证主体始终来自 {@link VerificationKey}。IP、设备和会话标识由调用方解析并规范化后提供；core
 * 不依赖 Servlet API，也不校验这些信号是否可信。
 *
 * @param key 验证码键
 * @param attributes IP、设备和会话等额外限流属性
 */
public record IssueContext(VerificationKey key, Map<IssueLimitDimension, String> attributes) {

    /**
     * 创建并校验签发上下文。
     *
     * @throws NullPointerException 当验证码键、属性集合或任一属性键值为 {@code null} 时
     * @throws IllegalArgumentException 当额外属性试图覆盖验证码键维度，或者属性值为空白时
     */
    public IssueContext {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
        EnumMap<IssueLimitDimension, String> copy = new EnumMap<>(IssueLimitDimension.class);
        attributes.forEach((dimension, value) -> {
            Objects.requireNonNull(dimension, "attribute dimension must not be null");
            Objects.requireNonNull(value, "attribute value must not be null");
            requireAdditionalDimension(dimension);
            if (value.isBlank()) {
                throw new IllegalArgumentException("attribute value must not be blank: " + dimension);
            }
            copy.put(dimension, value);
        });
        attributes = Collections.unmodifiableMap(copy);
    }

    /**
     * 使用验证码键创建不含额外来源信号的签发上下文。
     *
     * @param key 验证码键
     * @return 签发上下文
     */
    public static IssueContext of(VerificationKey key) {
        return new IssueContext(key, Map.of());
    }

    /**
     * 返回增加或替换一个额外来源信号后的新上下文。
     *
     * @param dimension IP、设备或会话维度
     * @param value 已规范化的维度值
     * @return 包含指定信号的新上下文
     * @throws NullPointerException 当维度或值为 {@code null} 时
     * @throws IllegalArgumentException 当维度属于验证码键，或者值为空白时
     */
    public IssueContext with(IssueLimitDimension dimension, String value) {
        Objects.requireNonNull(dimension, "dimension must not be null");
        Objects.requireNonNull(value, "value must not be null");
        requireAdditionalDimension(dimension);
        EnumMap<IssueLimitDimension, String> copy = new EnumMap<>(IssueLimitDimension.class);
        copy.putAll(attributes);
        copy.put(dimension, value);
        return new IssueContext(key, copy);
    }

    /**
     * 查找指定限流维度的值。
     *
     * @param dimension 限流维度
     * @return 验证码键中的固有值或额外来源信号
     * @throws NullPointerException 当维度为 {@code null} 时
     */
    public Optional<String> value(IssueLimitDimension dimension) {
        Objects.requireNonNull(dimension, "dimension must not be null");
        return switch (dimension) {
            case NAMESPACE -> Optional.of(key.namespace());
            case PURPOSE -> Optional.of(key.purpose());
            case SUBJECT -> Optional.of(key.subject());
            case IP_ADDRESS, DEVICE_ID, SESSION_ID -> Optional.ofNullable(attributes.get(dimension));
        };
    }

    /** 不输出验证主体及来源信号的具体值，避免日志意外泄露敏感信息。 */
    @Override
    public String toString() {
        return "IssueContext[namespace=" + key.namespace() + ", purpose=" + key.purpose() + ", additionalDimensions="
                + attributes.keySet() + ']';
    }

    private static void requireAdditionalDimension(IssueLimitDimension dimension) {
        if (dimension == IssueLimitDimension.NAMESPACE
                || dimension == IssueLimitDimension.PURPOSE
                || dimension == IssueLimitDimension.SUBJECT) {
            throw new IllegalArgumentException(
                    "verification key dimension cannot be supplied as an attribute: " + dimension);
        }
    }
}
