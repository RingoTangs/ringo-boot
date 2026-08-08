package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ringotangs.ringoboot.verification.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ConsoleCodeSenderTest {

    @Test
    void logsEmailCodeWithDevelopmentWarningAndMaskedAddress(CapturedOutput output) {
        new ConsoleEmailCodeSender()
                .send(new CodeDelivery(
                        new VerificationKey("email-login", "user@example.com"),
                        "123456",
                        Instant.parse("2026-01-01T00:05:00Z")));

        assertThat(output)
                .contains("DEVELOPMENT ONLY")
                .contains("purpose=email-login")
                .contains("subject=u***@example.com")
                .contains("code=123456")
                .doesNotContain("user@example.com");
    }

    @Test
    void logsSmsCodeWithDevelopmentWarningAndMaskedPhoneNumber(CapturedOutput output) {
        new ConsoleSmsCodeSender()
                .send(new CodeDelivery(
                        new VerificationKey("sms-login", "+8613800000000"),
                        "654321",
                        Instant.parse("2026-01-01T00:05:00Z")));

        assertThat(output)
                .contains("DEVELOPMENT ONLY")
                .contains("purpose=sms-login")
                .contains("subject=***0000")
                .contains("code=654321")
                .doesNotContain("+8613800000000");
    }
}
