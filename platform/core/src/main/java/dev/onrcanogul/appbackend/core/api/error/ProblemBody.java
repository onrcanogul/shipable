package dev.onrcanogul.appbackend.core.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * The one and only error body shape.
 *
 * <p>Every failure from every endpoint looks like this, which is what lets a mobile client
 * write one error handler instead of one per screen.
 *
 * <pre>
 * {
 *   "status": 400,
 *   "code": "validation_failed",
 *   "message": "Request validation failed",
 *   "requestId": "0f2c...",
 *   "fieldErrors": [{"field": "email", "message": "must be a well-formed email address"}]
 * }
 * </pre>
 *
 * <p>Public API rather than internal: filters in other modules answer errors too, and they
 * must produce exactly this shape. A second error format is how a client ends up with two
 * error handlers and a bug in the one nobody tested.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ProblemBody(
        int status,
        String code,
        String message,
        String requestId,
        List<AppError.FieldError> fieldErrors) {

    public static ProblemBody of(int status, AppError error, String requestId) {
        return new ProblemBody(status, error.code(), error.message(), requestId, error.fieldErrors());
    }
}
