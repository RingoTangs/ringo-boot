package io.github.ringotangs.ringoboot.problem;

import io.github.ringotangs.ringoboot.problem.internal.KebabCase;
import java.net.URI;
import java.util.Objects;

/**
 * 创建稳定的 Problem Details 类型 URN。
 *
 * <p>生成结果的格式为 {@code urn:problem:{domain}:{segment...}}。所有分段必须使用小写字母、数字以及连接分段的单个连字符，
 * 输入不会被自动转换。
 */
public final class ProblemTypeUri {

    private static final String PREFIX = "urn:problem";

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
        KebabCase.validate("domain", domain);
        Objects.requireNonNull(segments, "segments must not be null");
        if (segments.length == 0) {
            throw new IllegalArgumentException("at least one problem segment is required");
        }

        StringBuilder value = new StringBuilder(PREFIX).append(':').append(domain);
        for (String valueSegment : segments) {
            String segment = Objects.requireNonNull(valueSegment, "segment must not be null");
            KebabCase.validate("segment", segment);
            value.append(':').append(segment);
        }
        return URI.create(value.toString());
    }
}
