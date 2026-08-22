package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 保存一次验证码签发请求的业务键和可扩展来源属性。
 *
 * <p>属性由调用方解析并规范化，core 不预定义属性名称，也不校验来源是否可信。
 *
 * @param key 验证码键
 * @param attributes IP、设备、会话等由应用定义的额外属性
 */
public record IssueContext(VerificationKey key, Map<String, String> attributes) {

    /** 创建并校验签发上下文。 */
    public IssueContext {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
        Map<String, String> copy = new HashMap<>();
        attributes.forEach((name, value) -> {
            requireAttributeName(name);
            Objects.requireNonNull(value, "attribute value must not be null");
            if (value.isBlank()) {
                throw new IllegalArgumentException("attribute value must not be blank: " + name);
            }
            copy.put(name, value);
        });
        attributes = Map.copyOf(copy);
    }

    /** 使用验证码键创建不含额外属性的签发上下文。 */
    public static IssueContext of(VerificationKey key) {
        return new IssueContext(key, Map.of());
    }

    /** 返回增加或替换一个属性后的新上下文。 */
    public IssueContext with(String name, String value) {
        requireAttributeName(name);
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("attribute value must not be blank");
        }
        Map<String, String> copy = new HashMap<>(attributes);
        copy.put(name, value);
        return new IssueContext(key, copy);
    }

    /** 查找指定名称的扩展属性。 */
    public Optional<String> attribute(String name) {
        requireAttributeName(name);
        return Optional.ofNullable(attributes.get(name));
    }

    /** 不输出验证主体和属性值，避免日志意外泄露敏感信息。 */
    @Override
    public String toString() {
        return "IssueContext[namespace=" + key.namespace() + ", purpose=" + key.purpose() + ", attributes="
                + attributes.keySet() + ']';
    }

    private static void requireAttributeName(String name) {
        Objects.requireNonNull(name, "attribute name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("attribute name must not be blank");
        }
    }
}
