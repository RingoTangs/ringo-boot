package io.github.ringotangs.commons.core;

import java.net.URI;

/**
 * 描述符合 RFC 9457 Problem Details 规范的稳定业务问题类型。
 * 异常处理层可使用该接口提供的元数据构建 HTTP 错误响应；实现类应保持
 * {@code type}、{@code title} 和 HTTP 状态的语义稳定。
 *
 * <p>Describes a stable business problem type that follows RFC 9457 Problem Details.
 * Exception handlers can use this metadata to build HTTP error responses. Implementations
 * should keep the semantics of {@code type}, {@code title}, and the HTTP status stable.</p>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9457.html">RFC 9457</a>
 */
public interface ProblemType {

    /**
     * 返回该问题类型的不可变定义。
     *
     * <p>Returns the immutable definition of this problem type.</p>
     *
     * @return 问题定义 / the problem definition
     */
    ProblemDefinition getDefinition();

    /**
     * 返回供客户端识别问题类型的稳定 URI，建议使用绝对 URI 或 URN。
     *
     * <p>Returns the stable URI that clients use to identify the problem type.
     * An absolute URI or URN is recommended.</p>
     *
     * @return 问题类型 URI / the problem type URI
     */
    default URI getType() {
        return getDefinition().type();
    }

    /**
     * 返回简短且人类可读的问题标题；除本地化外，同一问题类型的标题应保持不变。
     *
     * <p>Returns a short, human-readable problem title. Except for localization, the title
     * should remain unchanged for the same problem type.</p>
     *
     * @return 问题标题 / the problem title
     */
    default String getTitle() {
        return getDefinition().title();
    }

    /**
     * 返回问题的默认详情，可由 {@link ProblemException} 中的自定义详情覆盖。
     *
     * <p>Returns the default problem detail, which a custom detail in
     * {@link ProblemException} may override.</p>
     *
     * @return 默认问题详情 / the default problem detail
     */
    default String getDefaultDetail() {
        return getDefinition().defaultDetail();
    }

    /**
     * 返回实际 HTTP 响应使用的状态码，该值必须与 Problem Details 的
     * {@code status} 字段一致。
     *
     * <p>Returns the status code used by the actual HTTP response. It must match the
     * {@code status} member in the Problem Details response.</p>
     *
     * @return HTTP 状态码 / the HTTP status code
     */
    default int getHttpStatus() {
        return getDefinition().httpStatus();
    }
}
