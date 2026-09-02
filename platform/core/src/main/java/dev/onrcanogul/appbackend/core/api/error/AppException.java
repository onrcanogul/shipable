package dev.onrcanogul.appbackend.core.api.error;

import org.springframework.http.HttpStatus;

/**
 * Root of the exception family. Carries both the {@link AppError} the client sees and the
 * HTTP status it maps to, so the mapping lives with the exception rather than in a growing
 * switch inside the handler.
 */
public class AppException extends RuntimeException {

    private final transient AppError error;
    private final HttpStatus status;

    public AppException(HttpStatus status, AppError error) {
        super(error.message());
        this.status = status;
        this.error = error;
    }

    public AppException(HttpStatus status, String code, String message) {
        this(status, AppError.of(code, message));
    }

    public AppError error() {
        return error;
    }

    public HttpStatus status() {
        return status;
    }
}
