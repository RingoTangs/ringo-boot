package io.github.ringotangs.springcommons.core;

import java.net.URI;
import java.util.Objects;

/**
 * 保存 RFC 9457 Problem Details 问题类型的不可变元数据。
 *
 * <p>Stores the immutable metadata of an RFC 9457 Problem Details type.</p>
 *
 * @param type 问题类型 URI / the problem type URI
 * @param messageCode 国际化消息基础键 / the base internationalization message code
 * @param title 问题标题 / the problem title
 * @param defaultDetail 默认问题详情 / the default problem detail
 * @param httpStatus HTTP 错误状态码 / the HTTP error status code
 */
public record ProblemDefinition(
        URI type,
        String messageCode,
        String title,
        String defaultDetail,
        int httpStatus
) {

    private static final int MIN_HTTP_ERROR_STATUS = 400;
    private static final int MAX_HTTP_ERROR_STATUS = 599;

    /**
     * 创建问题定义并校验必填字段和 HTTP 错误状态码。
     *
     * <p>Creates a problem definition and validates its required fields and HTTP error
     * status code.</p>
     */
    public ProblemDefinition {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(messageCode, "messageCode must not be null");
        if (messageCode.isBlank()) {
            throw new IllegalArgumentException("messageCode must not be blank");
        }
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(defaultDetail, "defaultDetail must not be null");
        if (!isErrorStatus(httpStatus)) {
            throw new IllegalArgumentException(
                    "httpStatus must be between "
                            + MIN_HTTP_ERROR_STATUS
                            + " and "
                            + MAX_HTTP_ERROR_STATUS
                            + ": "
                            + httpStatus
            );
        }
    }

    /**
     * 使用 URI 字符串创建问题定义。
     *
     * <p>Creates a problem definition from a URI string.</p>
     *
     * @param type 问题类型 URI 字符串 / the problem type URI string
     * @param messageCode 国际化消息基础键 / the base internationalization message code
     * @param title 问题标题 / the problem title
     * @param defaultDetail 默认问题详情 / the default problem detail
     * @param httpStatus HTTP 错误状态码 / the HTTP error status code
     * @return 问题定义 / the problem definition
     */
    public static ProblemDefinition of(
            String type,
            String messageCode,
            String title,
            String defaultDetail,
            int httpStatus
    ) {
        return new ProblemDefinition(
                URI.create(Objects.requireNonNull(type, "type must not be null")),
                messageCode,
                title,
                defaultDetail,
                httpStatus
        );
    }

    private static boolean isErrorStatus(int httpStatus) {
        return httpStatus >= MIN_HTTP_ERROR_STATUS
                && httpStatus <= MAX_HTTP_ERROR_STATUS;
    }
}
