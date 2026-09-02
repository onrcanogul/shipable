package dev.onrcanogul.appbackend.core.api.context;

/**
 * Facts about the current request that are useful almost everywhere.
 *
 * <p>Deliberately does not carry the user: authentication happens later in the chain than
 * this context is built, and plenty of endpoints are public. The current user lives in
 * {@code identity}'s own holder.
 *
 * @param requestId     correlation id, echoed back in the {@code X-Request-Id} header
 * @param platform      the calling client
 * @param appVersion    client app version, e.g. "1.4.2"; null when the client did not say
 * @param clientIp      best-effort caller address, used for rate limiting
 */
public record RequestContext(String requestId, ClientPlatform platform, String appVersion, String clientIp) {
}
