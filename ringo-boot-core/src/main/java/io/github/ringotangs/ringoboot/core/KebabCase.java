package io.github.ringotangs.ringoboot.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 校验由小写字母、数字和单个连字符组成的稳定名称。
 *
 * <p>合法示例：{@code account}、{@code user-account2}、{@code reset-password}。非法示例：{@code User}、
 * {@code user_account}、{@code -user}、{@code user-}、{@code user--account}。
 */
public final class KebabCase {

    private static final Pattern PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private KebabCase() {}

    /**
     * 校验指定值是否符合小写 kebab-case 格式。
     *
     * <p>输入不会被自动转换。{@code [a-z0-9]+} 要求名称至少包含一个小写字母或数字；
     * {@code (?:-[a-z0-9]+)*} 允许使用单个连字符连接后续内容。
     *
     * @param name  参数名称，用于生成异常消息
     * @param value 需要校验的稳定名称
     * @throws NullPointerException     当参数名称或待校验值为 {@code null} 时
     * @throws IllegalArgumentException 当待校验值不符合小写 kebab-case 格式时
     */
    public static void validate(String name, String value) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, name + " must not be null");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must be lowercase kebab-case: " + value);
        }
    }
}
