package dev.onrcanogul.appbackend.billing.internal.client;

import dev.onrcanogul.appbackend.billing.BillingProperties;
import dev.onrcanogul.appbackend.billing.api.port.WebhookAuthenticator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Checks the shared secret RevenueCat sends in the {@code Authorization} header.
 *
 * <p>RevenueCat does not sign webhook bodies; it sends back whatever header value you
 * configured in the dashboard. So this is a shared-secret comparison, and the only thing
 * that makes it safe is that the secret is long, random and only ever in the environment.
 *
 * <p>The comparison is constant-time. String equality short-circuits on the first
 * differing byte, which leaks the secret one character at a time to anyone patient enough
 * to measure - a real attack on an endpoint an attacker can call as often as they like.
 */
public class RevenueCatWebhookAuthenticator implements WebhookAuthenticator {

    private final byte[] expected;

    public RevenueCatWebhookAuthenticator(BillingProperties properties) {
        this.expected = properties.revenueCat().webhookSecret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean isAuthentic(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(authorizationHeader.getBytes(StandardCharsets.UTF_8), expected);
    }
}
