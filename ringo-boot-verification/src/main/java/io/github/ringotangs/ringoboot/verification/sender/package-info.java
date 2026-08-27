/**
 * 提供跨渠道共用的验证码派发结果和异常。
 *
 * <p>渠道发送器应将供应商正常受理映射为 {@link
 * io.github.ringotangs.ringoboot.verification.sender.CodeSendResult#ACCEPTED}，明确拒绝映射为 {@link
 * io.github.ringotangs.ringoboot.verification.sender.CodeSendResult#REJECTED}，超时或响应丢失等不确定结果映射为 {@link
 * io.github.ringotangs.ringoboot.verification.sender.CodeSendResult#UNKNOWN}。只有确定请求未提交给供应商的适配器故障才应抛出 {@link
 * io.github.ringotangs.ringoboot.verification.sender.CodeSenderException}。
 */
@NullMarked
package io.github.ringotangs.ringoboot.verification.sender;

import org.jspecify.annotations.NullMarked;
