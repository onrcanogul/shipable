package dev.onrcanogul.appbackend.core.api.model;

import dev.onrcanogul.appbackend.core.api.error.AppError;
import dev.onrcanogul.appbackend.core.api.error.AppException;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.http.HttpStatus;

/**
 * Return type for failures that are part of the contract, as an alternative to throwing.
 *
 * <p>The rule: <b>expected</b> outcomes (a token the provider rejected, a receipt that did
 * not check out) come back as a {@code Result}; <b>unexpected</b> ones throw. Callers of a
 * {@code Result}-returning method are forced to look at the failure case.
 *
 * @param <T> the success value
 */
public sealed interface Result<T> permits Result.Ok, Result.Err {

    record Ok<T>(T value) implements Result<T> {
    }

    record Err<T>(AppError error) implements Result<T> {
    }

    static <T> Result<T> ok(T value) {
        return new Ok<>(value);
    }

    static <T> Result<T> err(AppError error) {
        return new Err<>(error);
    }

    static <T> Result<T> err(String code, String message) {
        return new Err<>(AppError.of(code, message));
    }

    default boolean isOk() {
        return this instanceof Ok<T>;
    }

    /**
     * The success value when present. Named {@code asOptional} rather than {@code value}
     * because {@code Ok.value()} is already the record accessor.
     */
    default Optional<T> asOptional() {
        return this instanceof Ok<T>(T v) ? Optional.of(v) : Optional.empty();
    }

    /** The error when present. */
    default Optional<AppError> failure() {
        return this instanceof Err<T>(AppError e) ? Optional.of(e) : Optional.empty();
    }

    @SuppressWarnings("unchecked")
    default <R> Result<R> map(Function<T, R> mapper) {
        return switch (this) {
            case Ok<T>(T v) -> ok(mapper.apply(v));
            case Err<T> e -> (Result<R>) e;
        };
    }

    @SuppressWarnings("unchecked")
    default <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
        return switch (this) {
            case Ok<T>(T v) -> mapper.apply(v);
            case Err<T> e -> (Result<R>) e;
        };
    }

    /** Turns the error case into an exception, using the supplied status. */
    default T orElseThrow(HttpStatus status) {
        return switch (this) {
            case Ok<T>(T v) -> v;
            case Err<T>(AppError e) -> throw new AppException(status, e);
        };
    }
}
