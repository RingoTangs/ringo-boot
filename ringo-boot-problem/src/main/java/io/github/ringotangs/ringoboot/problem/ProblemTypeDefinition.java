package io.github.ringotangs.ringoboot.problem;

import java.net.URI;
import java.util.Objects;

/**
 * 保存 RFC 9457 Problem Details 问题类型的不可变元数据。
 *
 * @param type 问题类型 URI
 * @param messageCode 国际化消息基础键
 * @param title 问题标题
 * @param defaultDetail 默认问题详情
 * @param httpStatus HTTP 错误状态码
 */
public record ProblemTypeDefinition(URI type, String messageCode, String title, String defaultDetail, int httpStatus) {

    private static final int MIN_HTTP_ERROR_STATUS = 400;
    private static final int MAX_HTTP_ERROR_STATUS = 599;

    /**
     * 创建问题定义并校验必填字段和 HTTP 错误状态码。
     *
     * @throws NullPointerException 当问题类型 URI、国际化消息基础键、标题或默认详情为 {@code null} 时
     * @throws IllegalArgumentException 当国际化消息基础键为空白，或者 HTTP 状态码不在 {@code 400–599} 范围内时
     */
    public ProblemTypeDefinition {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(messageCode, "messageCode must not be null");
        if (messageCode.isBlank()) {
            throw new IllegalArgumentException("messageCode must not be blank");
        }
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(defaultDetail, "defaultDetail must not be null");
        if (!isErrorStatus(httpStatus)) {
            throw new IllegalArgumentException("httpStatus must be between "
                    + MIN_HTTP_ERROR_STATUS
                    + " and "
                    + MAX_HTTP_ERROR_STATUS
                    + ": "
                    + httpStatus);
        }
    }

    /**
     * 使用 URI 字符串创建问题定义。
     *
     * @param type 问题类型 URI 字符串
     * @param messageCode 国际化消息基础键
     * @param title 问题标题
     * @param defaultDetail 默认问题详情
     * @param httpStatus HTTP 错误状态码
     * @return 问题定义
     * @throws NullPointerException 当任一必填参数为 {@code null} 时
     * @throws IllegalArgumentException 当 URI 字符串非法、国际化消息基础键为空白，或者 HTTP 状态码不在
     *     {@code 400–599} 范围内时
     */
    public static ProblemTypeDefinition of(
            String type, String messageCode, String title, String defaultDetail, int httpStatus) {
        return of(
                URI.create(Objects.requireNonNull(type, "type must not be null")),
                messageCode,
                title,
                defaultDetail,
                httpStatus);
    }

    /**
     * 使用 URI 创建问题定义。
     *
     * @param type 问题类型 URI
     * @param messageCode 国际化消息基础键
     * @param title 问题标题
     * @param defaultDetail 默认问题详情
     * @param httpStatus HTTP 错误状态码
     * @return 问题定义
     * @throws NullPointerException 当任一必填参数为 {@code null} 时
     * @throws IllegalArgumentException 当国际化消息基础键为空白，或者 HTTP 状态码不在 {@code 400–599} 范围内时
     */
    public static ProblemTypeDefinition of(
            URI type, String messageCode, String title, String defaultDetail, int httpStatus) {
        return new ProblemTypeDefinition(type, messageCode, title, defaultDetail, httpStatus);
    }

    private static boolean isErrorStatus(int httpStatus) {
        return httpStatus >= MIN_HTTP_ERROR_STATUS && httpStatus <= MAX_HTTP_ERROR_STATUS;
    }
}
