package dev.onrcanogul.appbackend.identity.api.port;

import dev.onrcanogul.appbackend.core.api.model.Result;
import dev.onrcanogul.appbackend.core.api.model.UserId;

/**
 * Issues and validates the access tokens this backend signs.
 *
 * <p>Separated from {@link AuthenticationService} so the signing strategy can change —
 * symmetric HMAC today, asymmetric keys once a second service needs to validate tokens
 * without sharing the secret — without touching the sign-in flows.
 */
public interface AccessTokenService {

    String issue(UserId userId);

    /** @return the subject, or an error when the token is expired, forged or malformed */
    Result<UserId> validate(String accessToken);
}
