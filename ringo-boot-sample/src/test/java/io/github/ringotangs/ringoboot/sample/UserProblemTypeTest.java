package io.github.ringotangs.ringoboot.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class UserProblemTypeTest {

    @Test
    void exposesSpringHttpStatusValues() {
        assertEquals(HttpStatus.BAD_REQUEST.value(), UserProblemType.INVALID_USER_ID.getHttpStatus());
        assertEquals(HttpStatus.NOT_FOUND.value(), UserProblemType.USER_NOT_FOUND.getHttpStatus());
        assertEquals(
                UserProblemType.INVALID_USER_ID.getDefinition().httpStatus(),
                UserProblemType.INVALID_USER_ID.getHttpStatus());
    }
}
