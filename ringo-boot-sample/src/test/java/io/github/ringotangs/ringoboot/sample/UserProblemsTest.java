package io.github.ringotangs.ringoboot.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class UserProblemsTest {

    @Test
    void exposesSpringHttpStatusValues() {
        assertEquals(HttpStatus.BAD_REQUEST.value(), UserProblems.INVALID_USER_ID.status());
        assertEquals(HttpStatus.NOT_FOUND.value(), UserProblems.USER_NOT_FOUND.status());
    }
}
