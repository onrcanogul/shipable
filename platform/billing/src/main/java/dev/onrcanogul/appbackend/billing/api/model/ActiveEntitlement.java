package dev.onrcanogul.appbackend.billing.api.model;

import java.time.Instant;

/**
 * One entitlement the user currently holds.
 *
 * @param expiresAt   null for a lifetime purchase
 * @param willRenew   false once the user has cancelled but before the period ends - the
 *                    moment to show a win-back offer, and the reason this field exists
 * @param inGracePeriod true when payment failed but the stores are still retrying; the
 *                    user should keep access
 */
public record ActiveEntitlement(
        EntitlementId id,
        String productId,
        Store store,
        Instant expiresAt,
        boolean willRenew,
        boolean inGracePeriod) {
}
