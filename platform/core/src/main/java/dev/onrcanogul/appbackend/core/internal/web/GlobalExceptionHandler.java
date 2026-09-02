package dev.onrcanogul.appbackend.core.internal.web;

import dev.onrcanogul.appbackend.core.api.context.RequestContextHolder;
import dev.onrcanogul.appbackend.core.api.error.AppError;
import dev.onrcanogul.appbackend.core.api.error.AppException;
import dev.onrcanogul.appbackend.core.api.error.ErrorCodes;
import dev.onrcanogul.appbackend.core.api.error.ProblemBody;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Turns every failure into the same {@link ProblemBody}.
 *
 * <p>Two rules worth keeping:
 * <ul>
 *   <li><b>Nothing internal leaks.</b> An unexpected exception becomes a generic 500 with
 *       the request id; the stack trace goes to the log, not to the client.</li>
 *   <li><b>Validation failures name their fields.</b> A mobile client can then highlight
 *       the offending input instead of showing a generic message.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Anything the application threw on purpose: the status travels with the exception. */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ProblemBody> handleAppException(AppException e) {
        if (e.status().is5xxServerError()) {
            log.error("Application error: {}", e.error().code(), e);
        } else {
            log.debug("Rejected request: {} - {}", e.error().code(), e.error().message());
        }
        return respond(e.status(), e.error());
    }

    /** {@code @Valid} on a request body. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemBody> handleBodyValidation(MethodArgumentNotValidException e) {
        List<AppError.FieldError> fields = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new AppError.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return respond(HttpStatus.BAD_REQUEST,
                AppError.of(ErrorCodes.VALIDATION_FAILED, "Request validation failed", fields));
    }

    /** {@code @Validated} on path variables and request parameters. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemBody> handleParameterValidation(ConstraintViolationException e) {
        List<AppError.FieldError> fields = e.getConstraintViolations().stream()
                .map(violation -> new AppError.FieldError(lastPathNode(violation), violation.getMessage()))
                .toList();
        return respond(HttpStatus.BAD_REQUEST,
                AppError.of(ErrorCodes.VALIDATION_FAILED, "Request validation failed", fields));
    }

    /** Malformed JSON, or a missing required parameter. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestValueException.class})
    public ResponseEntity<ProblemBody> handleMalformedRequest(Exception e) {
        log.debug("Malformed request", e);
        return respond(HttpStatus.BAD_REQUEST,
                AppError.of(ErrorCodes.MALFORMED_REQUEST, "Request could not be read"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemBody> handleNoHandler(NoHandlerFoundException e) {
        return respond(HttpStatus.NOT_FOUND,
                AppError.of(ErrorCodes.NOT_FOUND, "No endpoint at " + e.getRequestURL()));
    }

    /**
     * The catch-all.
     *
     * <p>Deliberately says nothing specific: exception messages have a habit of containing
     * table names, file paths and occasionally credentials.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemBody> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                AppError.of(ErrorCodes.INTERNAL_ERROR, "Something went wrong on our side"));
    }

    private ResponseEntity<ProblemBody> respond(HttpStatus status, AppError error) {
        return ResponseEntity.status(status)
                .body(ProblemBody.of(status.value(), error, RequestContextHolder.requestId()));
    }

    private static String lastPathNode(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }
}
