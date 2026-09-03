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
 * @param status HTTP 错误状态码
 */
public record ProblemDescriptor(URI type, String messageCode, String title, String defaultDetail, int status) {

    private static final int MIN_ERROR_STATUS = 400;
    private static final int MAX_ERROR_STATUS = 599;

    /**
     * 创建问题描述并校验必填字段和 HTTP 错误状态码。
     *
     * @throws NullPointerException 当问题类型 URI、国际化消息基础键、标题或默认详情为 {@code null} 时
     * @throws IllegalArgumentException 当国际化消息基础键为空白，或者 HTTP 状态码不在 {@code 400–599} 范围内时
     */
    public ProblemDescriptor {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(messageCode, "messageCode must not be null");
        if (messageCode.isBlank()) {
            throw new IllegalArgumentException("messageCode must not be blank");
        }
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(defaultDetail, "defaultDetail must not be null");
        if (!isErrorStatus(status)) {
            throw new IllegalArgumentException(
                    "status must be between " + MIN_ERROR_STATUS + " and " + MAX_ERROR_STATUS + ": " + status);
        }
    }

    /**
     * 使用 URI 字符串创建问题描述。
     *
     * @param type 问题类型 URI 字符串
     * @param messageCode 国际化消息基础键
     * @param title 问题标题
     * @param defaultDetail 默认问题详情
     * @param status HTTP 错误状态码
     * @return 问题描述
     */
    public static ProblemDescriptor of(
            String type, String messageCode, String title, String defaultDetail, int status) {
        return of(
                URI.create(Objects.requireNonNull(type, "type must not be null")),
                messageCode,
                title,
                defaultDetail,
                status);
    }

    /**
     * 使用 URI 创建问题描述。
     *
     * @param type 问题类型 URI
     * @param messageCode 国际化消息基础键
     * @param title 问题标题
     * @param defaultDetail 默认问题详情
     * @param status HTTP 错误状态码
     * @return 问题描述
     */
    public static ProblemDescriptor of(URI type, String messageCode, String title, String defaultDetail, int status) {
        return new ProblemDescriptor(type, messageCode, title, defaultDetail, status);
    }

    private static boolean isErrorStatus(int status) {
        return status >= MIN_ERROR_STATUS && status <= MAX_ERROR_STATUS;
    }
}
