package io.github.ringotangs.ringoboot.verification;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 表示一次验证码签发流程共享的上下文。
 *
 * <p>验证码键、渠道和策略描述当前签发流程；扩展属性用于携带 IP、设备、账号、租户等由应用定义的信息。
 * 该记录不可变，构造时会复制属性集合，{@link #with(String, String)} 会返回新实例。
 *
 * @param key 验证码键
 * @param channel 验证码渠道
 * @param policy 本次签发使用的验证码策略
 * @param attributes IP、设备、会话等扩展属性
 */
public record IssueContext(
        VerificationKey key, VerificationChannel channel, VerificationPolicy policy, Map<String, String> attributes) {

    /**
     * 创建签发上下文并防御性复制扩展属性。
     *
     * @throws NullPointerException 当验证码键、渠道、策略、属性集合、属性名或属性值为 {@code null} 时
     * @throws IllegalArgumentException 当属性名或属性值为空白时
     */
    public IssueContext {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
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

    /**
     * 创建不含扩展属性的签发上下文。
     *
     * @param key 验证码键
     * @param channel 验证码渠道
     * @param policy 本次签发使用的验证码策略
     * @return 不含扩展属性的签发上下文
     */
    public static IssueContext of(VerificationKey key, VerificationChannel channel, VerificationPolicy policy) {
        return new IssueContext(key, channel, policy, Map.of());
    }

    /**
     * 返回增加或替换一个扩展属性后的新上下文。
     *
     * @param name 属性名
     * @param value 已规范化的属性值
     * @return 包含指定属性的新上下文
     */
    public IssueContext with(String name, String value) {
        requireAttributeName(name);
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("attribute value must not be blank");
        }
        Map<String, String> copy = new HashMap<>(attributes);
        copy.put(name, value);
        return new IssueContext(key, channel, policy, copy);
    }

    /**
     * 查找指定名称的扩展属性。
     *
     * @param name 属性名
     * @return 属性值；属性不存在时返回空
     */
    public Optional<String> attribute(String name) {
        requireAttributeName(name);
        return Optional.ofNullable(attributes.get(name));
    }

    /**
     * 返回不包含验证主体和属性值的诊断字符串。
     *
     * @return 仅包含命名空间、用途、渠道和属性名称的字符串
     */
    @Override
    public String toString() {
        return "IssueContext[namespace=" + key.namespace() + ", purpose=" + key.purpose() + ", channel=" + channel
                + ", attributes=" + attributes.keySet() + ']';
    }

    private static void requireAttributeName(String name) {
        Objects.requireNonNull(name, "attribute name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("attribute name must not be blank");
        }
    }
}
