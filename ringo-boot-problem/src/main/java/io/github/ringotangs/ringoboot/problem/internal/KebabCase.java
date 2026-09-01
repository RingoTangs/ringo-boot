package io.github.ringotangs.ringoboot.problem.internal;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Problem feature 内部使用的 kebab-case 校验器，不属于稳定公共 API。
 */
public final class KebabCase {

    private static final Pattern PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private KebabCase() {}

    /**
     * 校验指定值是否符合小写 kebab-case 格式。
     *
     * @param name 参数名称，用于生成异常消息
     * @param value 需要校验的稳定名称
     * @throws NullPointerException 当参数名称或待校验值为 {@code null} 时
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
