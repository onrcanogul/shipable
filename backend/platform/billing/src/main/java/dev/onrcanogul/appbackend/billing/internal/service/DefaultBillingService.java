package dev.onrcanogul.appbackend.billing.internal.service;

import dev.onrcanogul.appbackend.billing.api.model.CustomerEntitlements;
import dev.onrcanogul.appbackend.billing.api.model.EntitlementId;
import dev.onrcanogul.appbackend.billing.api.port.BillingProvider;
import dev.onrcanogul.appbackend.billing.api.port.BillingService;
import dev.onrcanogul.appbackend.billing.internal.persistence.repository.EntitlementSnapshotRepository;
import dev.onrcanogul.appbackend.core.api.error.AppException;
import dev.onrcanogul.appbackend.core.api.error.ErrorCodes;
import dev.onrcanogul.appbackend.core.api.model.UserId;
import java.time.Clock;
import org.springframework.http.HttpStatus;

/**
 * Skeleton implementation of {@link BillingService}.
 *
 * <p>The safe default is real and already in place: a user we know nothing about holds no
 * entitlements. Everything that would <i>grant</i> access is unimplemented, which is the
 * correct direction for a bug in this module to fail — a paying user briefly seeing a
 * paywall is a support ticket, a non-paying user getting the product for free is a
 * business model.
 *
 * <p>Notes for whoever implements the TODOs:
 * <ul>
 *   <li><b>Read from the snapshot, not from RevenueCat.</b> A paywall check runs on every
 *       screen; it must not depend on a third party being up, and RevenueCat rate-limits
 *       anyway.</li>
 *   <li><b>Deduplicate webhooks before applying them.</b> RevenueCat delivers
 *       at-least-once and retries every non-2xx.</li>
 *   <li><b>Treat the grace period as active.</b> A failed card on a renewal is not a
 *       cancellation; cutting the user off while the store is still retrying is how you
 *       generate refunds.</li>
 * </ul>
 */
public class DefaultBillingService implements BillingService {

    private final BillingProvider provider;
    private final EntitlementSnapshotRepository snapshots;
    private final Clock clock;

    public DefaultBillingService(
            BillingProvider provider, EntitlementSnapshotRepository snapshots, Clock clock) {
        this.provider = provider;
        this.snapshots = snapshots;
        this.clock = clock;
    }

    @Override
    public CustomerEntitlements entitlementsOf(UserId userId) {
        // TODO: snapshots.findAllByUserId(userId.value()), drop rows that are neither
        // unexpired nor in a grace period, and map to the model.
        // Until then: nobody is entitled to anything, which is the side to be wrong on.
        return CustomerEntitlements.none(userId, clock.instant());
    }

    @Override
    public void requireEntitlement(UserId userId, EntitlementId entitlementId) {
        if (!hasEntitlement(userId, entitlementId)) {
            throw new AppException(HttpStatus.FORBIDDEN, ErrorCodes.ENTITLEMENT_REQUIRED,
                    "This feature requires the '" + entitlementId + "' entitlement");
        }
    }

    @Override
    public CustomerEntitlements refreshFromProvider(UserId userId) {
        // TODO: provider.fetchEntitlements(userId), then replace this user's snapshot rows
        // in one transaction. On a provider error, keep the existing snapshot rather than
        // wiping it - a RevenueCat outage must not revoke everyone's subscription.
        throw new UnsupportedOperationException("TODO: refreshFromProvider is not implemented");
    }
}
