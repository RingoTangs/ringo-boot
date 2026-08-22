package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 表示一次验证码签发请求参与限流计算的上下文。
 *
 * <p>{@link VerificationKey} 提供命名空间、业务用途和验证主体；扩展属性用于携带 IP、设备、账号、租户等由应用定义的限流信号。
 * core 不预定义属性名称，也不校验属性来源是否可信。调用方应在创建上下文前完成解析、可信代理处理和格式规范化。
 *
 * <p>该记录不可变。构造时会复制属性集合，{@link #with(String, String)} 会返回新实例。{@link #toString()}
 * 仅输出属性名称，不输出验证主体和属性值。
 *
 * @param key 验证码键
 * @param attributes IP、设备、会话等由应用定义的额外属性
 */
public record IssueContext(VerificationKey key, Map<String, String> attributes) {

    /**
     * 创建签发上下文并防御性复制扩展属性。
     *
     * @throws NullPointerException 当验证码键、属性集合、属性名或属性值为 {@code null} 时
     * @throws IllegalArgumentException 当属性名或属性值为空白时
     */
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

    /**
     * 使用验证码键创建不含扩展属性的签发上下文。
     *
     * @param key 验证码键
     * @return 不含扩展属性的签发上下文
     * @throws NullPointerException 当验证码键为 {@code null} 时
     */
    public static IssueContext of(VerificationKey key) {
        return new IssueContext(key, Map.of());
    }

    /**
     * 返回增加或替换一个扩展属性后的新上下文。
     *
     * @param name 属性名；建议应用集中声明常量，避免不同调用方使用不同拼写
     * @param value 已规范化的属性值
     * @return 包含指定属性的新上下文，当前实例保持不变
     * @throws NullPointerException 当属性名或属性值为 {@code null} 时
     * @throws IllegalArgumentException 当属性名或属性值为空白时
     */
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

    /**
     * 查找指定名称的扩展属性。
     *
     * @param name 属性名
     * @return 包含属性值的 {@link Optional}；属性不存在时返回空
     * @throws NullPointerException 当属性名为 {@code null} 时
     * @throws IllegalArgumentException 当属性名为空白时
     */
    public Optional<String> attribute(String name) {
        requireAttributeName(name);
        return Optional.ofNullable(attributes.get(name));
    }

    /**
     * 返回不包含验证主体和属性值的诊断字符串。
     *
     * @return 仅包含命名空间、业务用途和属性名称的字符串
     */
    @Override
    public String toString() {
        return "IssueContext[namespace=" + key.namespace() + ", purpose=" + key.purpose() + ", attributes="
                + attributes.keySet() + ']';
    }

    /**
     * 校验扩展属性名称。
     *
     * @param name 待校验的属性名称
     * @throws NullPointerException 当属性名称为 {@code null} 时
     * @throws IllegalArgumentException 当属性名称为空白时
     */
    private static void requireAttributeName(String name) {
        Objects.requireNonNull(name, "attribute name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("attribute name must not be blank");
        }
    }
}
