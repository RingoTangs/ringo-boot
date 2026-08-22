package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class VerificationServiceTest {

    @Test
    void declaresOnlyServicePolicyIssueMethodAndSpecificVerifyException() throws NoSuchMethodException {
        Method issue = VerificationService.class.getMethod("issue", VerificationKey.class);
        Method verify = VerificationService.class.getMethod("verify", VerificationKey.class, String.class);

        assertArrayEquals(new Class<?>[] {VerificationException.class}, issue.getExceptionTypes());
        assertArrayEquals(new Class<?>[] {VerificationStoreException.class}, verify.getExceptionTypes());
        assertThrows(
                NoSuchMethodException.class,
                () -> VerificationService.class.getMethod("issue", VerificationKey.class, VerificationPolicy.class));
    }
}
