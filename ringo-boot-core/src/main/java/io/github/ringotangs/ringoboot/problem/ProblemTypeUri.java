package io.github.ringotangs.ringoboot.problem;

import java.net.URI;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 创建稳定的 Problem Details 类型 URN。
 *
 * <p>生成结果的格式为 {@code urn:problem:{domain}:{segment...}}。所有分段必须使用小写字母、数字以及连接分段的单个连字符，
 * 输入不会被自动转换。
 */
public final class ProblemTypeUri {

    private static final String PREFIX = "urn:problem";

    /**
     * 校验 URN 分段的小写 kebab-case 正则表达式。
     *
     * <p>{@code [a-z0-9]+} 要求分段以一个或多个小写字母或数字开始；{@code (?:-[a-z0-9]+)*}
     * 允许使用单个连字符连接后续内容。因此不允许大写字母、下划线、空格、冒号、首尾连字符或连续连字符。
     *
     * <p>合法示例：{@code mvc}、{@code error2}、{@code invalid-parameter}。非法示例：{@code MVC}、
     * {@code invalid_parameter}、{@code -invalid}、{@code invalid-}、{@code invalid--parameter}。
     */
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private ProblemTypeUri() {}

    /**
     * 使用领域和一个或多个问题分段创建问题类型 URI。
     *
     * <p>例如，{@code of("mvc", "invalid-parameter")} 返回
     * {@code urn:problem:mvc:invalid-parameter}；{@code of("business", "user", "not-found")} 返回
     * {@code urn:problem:business:user:not-found}。
     *
     * @param domain 问题所属领域
     * @param segments 一个或多个问题类型分段
     * @return 问题类型 URI
     * @throws NullPointerException 当领域、分段数组或任一分段为 {@code null} 时
     * @throws IllegalArgumentException 当没有提供问题分段，或者领域或任一分段不符合小写 kebab-case 规则时
     */
    public static URI of(String domain, String... segments) {
        validateSegment(Objects.requireNonNull(domain, "domain must not be null"), "domain");
        Objects.requireNonNull(segments, "segments must not be null");
        if (segments.length == 0) {
            throw new IllegalArgumentException("at least one problem segment is required");
        }

        StringBuilder value = new StringBuilder(PREFIX).append(':').append(domain);
        for (String valueSegment : segments) {
            String segment = Objects.requireNonNull(valueSegment, "segment must not be null");
            validateSegment(segment, "segment");
            value.append(':').append(segment);
        }
        return URI.create(value.toString());
    }

    private static void validateSegment(String value, String name) {
        if (!SEGMENT_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must contain only lowercase letters, digits, and single hyphens: " + value);
        }
    }
}
