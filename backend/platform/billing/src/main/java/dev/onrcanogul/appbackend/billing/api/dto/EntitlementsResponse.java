package dev.onrcanogul.appbackend.billing.api.dto;

import dev.onrcanogul.appbackend.billing.api.model.CustomerEntitlements;
import java.time.Instant;
import java.util.List;

/** What the client gets when it asks what the user has paid for. */
public record EntitlementsResponse(boolean paying, List<Entitlement> entitlements, Instant checkedAt) {

    public record Entitlement(
            String id,
            String productId,
            String store,
            Instant expiresAt,
            boolean willRenew,
            boolean inGracePeriod) {
    }

    public static EntitlementsResponse from(CustomerEntitlements entitlements) {
        return new EntitlementsResponse(
                entitlements.isPaying(),
                entitlements.active().stream()
                        .map(active -> new Entitlement(
                                active.id().value(),
                                active.productId(),
                                active.store().name(),
                                active.expiresAt(),
                                active.willRenew(),
                                active.inGracePeriod()))
                        .toList(),
                entitlements.checkedAt());
    }
}
