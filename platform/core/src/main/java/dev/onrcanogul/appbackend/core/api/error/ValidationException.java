package dev.onrcanogul.appbackend.core.api.error;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, message);
    }

    public ValidationException(String message, List<AppError.FieldError> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, AppError.of(ErrorCodes.VALIDATION_FAILED, message, fieldErrors));
    }
}
