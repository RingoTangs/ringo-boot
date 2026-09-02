/**
 * 图片验证码签发、渲染和校验能力。
 *
 * <p>应用应使用不可预测且唯一的 challenge ID 作为 {@link
 * io.github.ringotangs.ringoboot.verification.VerificationKey#subject()}。由于每次签发都会更换 subject，{@code
 * SubjectQuotaRule} 不能单独防止滥用；公开接口还应配置 purpose 或客户端 IP 配额。
 *
 * <p>图片验证码不应作为唯一的用户验证方式；面向用户的应用需要提供音频或其他无障碍替代方案。
 */
@NullMarked
package io.github.ringotangs.ringoboot.verification.channel.image;

import org.jspecify.annotations.NullMarked;
