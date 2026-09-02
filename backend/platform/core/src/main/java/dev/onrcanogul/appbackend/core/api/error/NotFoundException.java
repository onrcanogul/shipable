package dev.onrcanogul.appbackend.core.api.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends AppException {

    public NotFoundException(String what) {
        super(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, what + " not found");
    }
}
