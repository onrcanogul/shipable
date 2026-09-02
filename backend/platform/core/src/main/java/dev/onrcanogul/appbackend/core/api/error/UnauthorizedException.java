package dev.onrcanogul.appbackend.core.api.error;

import org.springframework.http.HttpStatus;

/**
 * No credentials, or credentials we could not accept.
 *
 * <p>Distinct from {@link ForbiddenException} on purpose: the client's reaction differs.
 * 401 means "sign in again", 403 means "signing in again will not help".
 */
public class UnauthorizedException extends AppException {

    public UnauthorizedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static UnauthorizedException missingCredentials() {
        return new UnauthorizedException(ErrorCodes.UNAUTHORIZED, "Authentication is required");
    }

    public static UnauthorizedException expiredToken() {
        return new UnauthorizedException(ErrorCodes.TOKEN_EXPIRED, "Access token has expired");
    }

    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException(ErrorCodes.TOKEN_INVALID, "Access token is not valid");
    }
}
