/**
 * 提供框架无关的验证码签发、存储与校验生命周期抽象。
 * 支持有效期、重发间隔、最大尝试次数以及成功后一次性消费等通用能力。
 *
 * <p>Provides framework-neutral abstractions for issuing, storing, and verifying
 * short-lived codes. The lifecycle supports expiration, resend throttling, maximum
 * attempts, and one-time consumption after successful verification.</p>
 */
@NullMarked
package io.github.ringotangs.ringoboot.verification;

import org.jspecify.annotations.NullMarked;
