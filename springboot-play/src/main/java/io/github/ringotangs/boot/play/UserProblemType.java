package io.github.ringotangs.boot.play;

import io.github.ringotangs.commons.core.ProblemType;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum UserProblemType implements ProblemType {

    INVALID_USER_ID(
            "urn:problem:spring-commons:user:invalid-id",
            "Invalid user id",
            "User id must be greater than 0",
            HttpStatus.BAD_REQUEST
    ),

    USER_NOT_FOUND(
            "urn:problem:spring-commons:user:not-found",
            "User not found",
            "The requested user does not exist",
            HttpStatus.NOT_FOUND
    );

    private final URI type;
    private final String title;
    private final String defaultDetail;
    private final HttpStatus httpStatus;

    UserProblemType(String type, String title, String defaultDetail, HttpStatus httpStatus) {
        this.type = URI.create(type);
        this.title = title;
        this.defaultDetail = defaultDetail;
        this.httpStatus = httpStatus;
    }

    @Override
    public URI getType() {
        return type;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDefaultDetail() {
        return defaultDetail;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus.value();
    }
}
