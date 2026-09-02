package dev.onrcanogul.appbackend.billing.internal.web;

import dev.onrcanogul.appbackend.billing.api.dto.EntitlementsResponse;
import dev.onrcanogul.appbackend.billing.api.port.BillingService;
import dev.onrcanogul.appbackend.identity.api.context.CurrentUserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the client asks about its own subscription.
 *
 * <p>The user always comes from the bearer token. There is deliberately no way to ask about
 * someone else's entitlements.
 */
@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing", description = "Subscription state for the signed-in user")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/entitlements")
    @Operation(summary = "What the current user has paid for",
            description = "Served from our snapshot, so it works while RevenueCat is unreachable.")
    public EntitlementsResponse entitlements() {
        return EntitlementsResponse.from(
                billingService.entitlementsOf(CurrentUserHolder.require().userId()));
    }

    @PostMapping("/entitlements/refresh")
    @Operation(summary = "Re-read subscription state from RevenueCat",
            description = "For restore-purchases and after an in-app purchase completes. "
                    + "Webhooks keep things current the rest of the time, so clients should not poll this.")
    public EntitlementsResponse refresh() {
        return EntitlementsResponse.from(
                billingService.refreshFromProvider(CurrentUserHolder.require().userId()));
    }
}
