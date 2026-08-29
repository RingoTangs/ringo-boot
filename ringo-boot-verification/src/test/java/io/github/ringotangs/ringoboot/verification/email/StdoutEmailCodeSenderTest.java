package io.github.ringotangs.ringoboot.verification.email;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StdoutEmailCodeSenderTest {

    @Test
    void writesCodeWithDevelopmentWarningAndMaskedAddress() {
        String output = captureOutput(() -> new StdoutEmailCodeSender()
                .send(
                        IssueContext.of(
                                new VerificationKey("account", "email-login", "user@example.com"),
                                VerificationChannel.EMAIL,
                                VerificationPolicy.defaults()),
                        "123456",
                        Instant.parse("2026-01-01T00:05:00Z")));

        assertTrue(output.contains("DEVELOPMENT ONLY"));
        assertTrue(output.contains("namespace=account"));
        assertTrue(output.contains("purpose=email-login"));
        assertTrue(output.contains("email=u***@example.com"));
        assertTrue(output.contains("code=123456"));
        assertTrue(output.contains("expiresAt=2026-01-01T00:05:00Z"));
        assertFalse(output.contains("user@example.com"));
    }

    private String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream replacement = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(replacement);
            action.run();
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8);
    }
}
