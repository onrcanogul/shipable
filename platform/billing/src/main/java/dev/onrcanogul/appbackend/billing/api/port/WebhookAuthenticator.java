package dev.onrcanogul.appbackend.billing.api.port;

/**
 * Decides whether an inbound webhook really came from the billing provider.
 *
 * <p>The endpoint is public and anyone can find it. Without this, a stranger can POST
 * "this user is now on the pro plan" and be believed.
 */
public interface WebhookAuthenticator {

    /**
     * @param authorizationHeader the raw {@code Authorization} header as received
     * @return true when the request is authentic
     */
    boolean isAuthentic(String authorizationHeader);
}
