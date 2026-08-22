package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class VerificationServiceTest {

    @Test
    void declaresUnifiedIssueExceptionAndSpecificVerifyException() throws NoSuchMethodException {
        Method defaultIssue = VerificationService.class.getMethod("issue", VerificationKey.class);
        Method configuredIssue =
                VerificationService.class.getMethod("issue", VerificationKey.class, VerificationPolicy.class);
        Method verify = VerificationService.class.getMethod("verify", VerificationKey.class, String.class);

        assertArrayEquals(new Class<?>[] {VerificationException.class}, defaultIssue.getExceptionTypes());
        assertArrayEquals(new Class<?>[] {VerificationException.class}, configuredIssue.getExceptionTypes());
        assertArrayEquals(new Class<?>[] {VerificationStoreException.class}, verify.getExceptionTypes());
    }
}
