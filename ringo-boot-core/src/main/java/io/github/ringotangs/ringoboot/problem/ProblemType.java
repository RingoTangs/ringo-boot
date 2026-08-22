package io.github.ringotangs.ringoboot.problem;

import java.net.URI;

/**
 * 描述符合 RFC 9457 Problem Details 规范的稳定问题类型。
 * 异常处理层可使用该接口提供的元数据构建 HTTP 错误响应；实现类应保持
 * {@code type}、{@code title} 和 HTTP 状态的语义稳定。
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9457.html">RFC 9457</a>
 */
@SuppressWarnings("unused") // 公开的便捷访问方法由下游应用调用。
public interface ProblemType {

    /**
     * 返回该问题类型的不可变定义。
     *
     * @return 问题定义
     */
    ProblemDefinition getDefinition();

    /**
     * 返回供客户端识别问题类型的稳定 URI，建议使用绝对 URI 或 URN。
     *
     * @return 问题类型 URI
     */
    default URI getType() {
        return getDefinition().type();
    }

    /**
     * 返回用于解析标题和详情的国际化消息基础键。
     *
     * @return 国际化消息基础键
     */
    default String getMessageCode() {
        return getDefinition().messageCode();
    }

    /**
     * 返回简短且人类可读的问题标题；除本地化外，同一问题类型的标题应保持不变。
     *
     * @return 问题标题
     */
    default String getTitle() {
        return getDefinition().title();
    }

    /**
     * 返回问题的默认详情。{@link ProblemException} 可以携带消息参数，对该详情中的占位符进行格式化。
     *
     * @return 默认问题详情
     */
    default String getDefaultDetail() {
        return getDefinition().defaultDetail();
    }

    /**
     * 返回实际 HTTP 响应使用的状态码，该值必须与 Problem Details 的
     * {@code status} 字段一致。
     *
     * @return HTTP 状态码
     */
    default int getHttpStatus() {
        return getDefinition().httpStatus();
    }
}
