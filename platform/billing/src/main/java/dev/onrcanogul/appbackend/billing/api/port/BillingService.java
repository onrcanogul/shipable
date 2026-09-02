package dev.onrcanogul.appbackend.billing.api.port;

import dev.onrcanogul.appbackend.billing.api.model.CustomerEntitlements;
import dev.onrcanogul.appbackend.billing.api.model.EntitlementId;
import dev.onrcanogul.appbackend.core.api.model.UserId;

/**
 * What the rest of the app asks about a user's subscription.
 *
 * <p>Reads come from our own snapshot of RevenueCat rather than from RevenueCat directly:
 * a paywall check on every screen must not depend on a third party being reachable, and
 * RevenueCat rate-limits. The snapshot is kept fresh by webhooks.
 */
public interface BillingService {

    /** Never null: a user with no purchase gets an empty entitlement set. */
    CustomerEntitlements entitlementsOf(UserId userId);

    /** The question almost every caller actually has. */
    default boolean hasEntitlement(UserId userId, EntitlementId entitlementId) {
        return entitlementsOf(userId).has(entitlementId);
    }

    /**
     * Throws when the user does not hold the entitlement, for guarding a paid feature.
     *
     * @throws dev.onrcanogul.appbackend.core.api.error.AppException 403 with
     *         {@code entitlement_required}
     */
    void requireEntitlement(UserId userId, EntitlementId entitlementId);

    /**
     * Pulls the current state from RevenueCat and replaces our snapshot.
     *
     * <p>Called on sign-in and when a client reports a purchase. Webhooks keep things
     * current afterwards; this exists for the cases webhooks miss — a restore on a new
     * device, a webhook we failed to process, a user who contacts support.
     */
    CustomerEntitlements refreshFromProvider(UserId userId);
}
