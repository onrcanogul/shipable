package dev.onrcanogul.appbackend.billing.api.model;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Everything this backend knows about what a user has paid for.
 *
 * @param checkedAt when this snapshot was taken, so callers can judge how stale it is
 */
public record CustomerEntitlements(UserId userId, List<ActiveEntitlement> active, Instant checkedAt) {

    public CustomerEntitlements {
        active = active == null ? List.of() : List.copyOf(active);
    }

    /** A user we have never seen a purchase for. The safe default everywhere. */
    public static CustomerEntitlements none(UserId userId, Instant checkedAt) {
        return new CustomerEntitlements(userId, List.of(), checkedAt);
    }

    public boolean has(EntitlementId entitlementId) {
        return active.stream().anyMatch(entitlement -> entitlement.id().equals(entitlementId));
    }

    public Optional<ActiveEntitlement> find(EntitlementId entitlementId) {
        return active.stream().filter(entitlement -> entitlement.id().equals(entitlementId)).findFirst();
    }

    public boolean isPaying() {
        return !active.isEmpty();
    }
}
