package dev.onrcanogul.appbackend.core.api.error;

import java.util.List;

/**
 * A machine-readable error: a stable {@code code}, a human-readable {@code message}, and
 * optionally the fields that were wrong.
 *
 * <p>Mobile clients branch on {@code code} and never on {@code message} — message text is
 * free to change, and may end up localised.
 */
public record AppError(String code, String message, List<FieldError> fieldErrors) {

    public AppError {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static AppError of(String code, String message) {
        return new AppError(code, message, List.of());
    }

    public static AppError of(String code, String message, List<FieldError> fieldErrors) {
        return new AppError(code, message, fieldErrors);
    }

    /** One rejected input field. */
    public record FieldError(String field, String message) {
    }
}
