/**
 * 提供框架无关的验证码签发、发送、存储与校验生命周期抽象。
 * 支持有效期、重发间隔、最大尝试次数、发送失败补偿以及成功后一次性消费等通用能力。
 * 验证码生成、派发和存储的技术故障统一继承 {@link
 * io.github.ringotangs.ringoboot.verification.VerificationException}。
 *
 */
@NullMarked
package io.github.ringotangs.ringoboot.verification;

import org.jspecify.annotations.NullMarked;
