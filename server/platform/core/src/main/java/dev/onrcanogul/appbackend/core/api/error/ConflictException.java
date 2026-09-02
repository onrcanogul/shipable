package dev.onrcanogul.appbackend.core.api.error;

import org.springframework.http.HttpStatus;

public class ConflictException extends AppException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, ErrorCodes.CONFLICT, message);
    }

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
