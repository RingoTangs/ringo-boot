/**
 * 提供框架无关的验证码签发、发送、存储与校验生命周期抽象。
 * 支持有效期、独立签发限流、最大尝试次数、发送失败补偿以及成功后一次性消费等通用能力。
 * {@link io.github.ringotangs.ringoboot.verification.IssueContext} 在签发入口创建，并显式贯穿限流和渠道派发流程。
 * 渠道发送器应将供应商正常受理映射为 {@link io.github.ringotangs.ringoboot.verification.CodeSendResult#ACCEPTED}，
 * 明确拒绝映射为 {@link io.github.ringotangs.ringoboot.verification.CodeSendResult#REJECTED}，超时或响应丢失等不确定结果映射为
 * {@link io.github.ringotangs.ringoboot.verification.CodeSendResult#UNKNOWN}。只有确定请求未提交给供应商的适配器故障才应抛出
 * {@link io.github.ringotangs.ringoboot.verification.CodeSenderException}。
 * 验证码生成、派发和存储的技术故障统一继承 {@link
 * io.github.ringotangs.ringoboot.verification.VerificationException}。
 */
@NullMarked
package io.github.ringotangs.ringoboot.verification;

import org.jspecify.annotations.NullMarked;
