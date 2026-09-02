package dev.onrcanogul.appbackend.core.api.error;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends AppException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, message);
    }
}
