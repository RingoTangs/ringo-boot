/**
 * 定义验证码渠道及渠道派发结果和异常。
 *
 * <p>渠道发送器应将供应商正常受理映射为 {@link
 * io.github.ringotangs.ringoboot.verification.channel.CodeSendResult#ACCEPTED}，明确拒绝映射为 {@link
 * io.github.ringotangs.ringoboot.verification.channel.CodeSendResult#REJECTED}，超时或响应丢失等不确定结果映射为 {@link
 * io.github.ringotangs.ringoboot.verification.channel.CodeSendResult#UNKNOWN}。只有确定请求未提交给供应商的适配器故障才应抛出 {@link
 * io.github.ringotangs.ringoboot.verification.channel.CodeSenderException}。
 *
 * <p>{@link io.github.ringotangs.ringoboot.verification.channel.CodeSendResult} 是发送器 SPI 的结果；{@link
 * io.github.ringotangs.ringoboot.verification.channel.DeliveryResult} 是邮件或短信服务成功签发后的业务结果。
 */
@NullMarked
package io.github.ringotangs.ringoboot.verification.channel;

import org.jspecify.annotations.NullMarked;
