package io.github.ringotangs.ringoboot.verification.context;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class IssueContextValidatorTest {

    private static final VerificationKey KEY = new VerificationKey("account", "login", "subject");
    private static final VerificationPolicy POLICY = VerificationPolicy.defaults();
    private static final IssueContext CONTEXT = IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY);

    @Test
    void acceptsPreservedContext() {
        assertDoesNotThrow(() ->
                IssueContextValidator.requirePreservedContext(CONTEXT, CONTEXT.with("tenant", "tenant-1"), "test"));
    }

    @Test
    void rejectsChangedIdentity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueContextValidator.requirePreservedContext(
                        CONTEXT,
                        IssueContext.of(
                                new VerificationKey("account", "login", "other"), VerificationChannel.EMAIL, POLICY),
                        "test"));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueContextValidator.requirePreservedContext(
                        CONTEXT, IssueContext.of(KEY, VerificationChannel.SMS, POLICY), "test"));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueContextValidator.requirePreservedContext(
                        CONTEXT,
                        IssueContext.of(
                                KEY, VerificationChannel.EMAIL, new VerificationPolicy(6, Duration.ofMinutes(1), 5)),
                        "test"));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(
                NullPointerException.class, () -> IssueContextValidator.requirePreservedContext(null, CONTEXT, "test"));
        assertThrows(
                NullPointerException.class, () -> IssueContextValidator.requirePreservedContext(CONTEXT, null, "test"));
        assertThrows(
                NullPointerException.class,
                () -> IssueContextValidator.requirePreservedContext(CONTEXT, CONTEXT, null));
    }
}
