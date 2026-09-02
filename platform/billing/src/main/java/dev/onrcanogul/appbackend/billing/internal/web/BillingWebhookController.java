package dev.onrcanogul.appbackend.billing.internal.web;

import dev.onrcanogul.appbackend.billing.api.dto.RevenueCatWebhookRequest;
import dev.onrcanogul.appbackend.billing.api.port.WebhookAuthenticator;
import dev.onrcanogul.appbackend.billing.internal.service.RevenueCatWebhookProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives RevenueCat webhooks.
 *
 * <p>Two rules, and they are the whole reason this is a separate controller:
 *
 * <p><b>Authenticate first.</b> This endpoint is public and easy to find. Without the
 * shared-secret check, anyone can POST an INITIAL_PURCHASE event and grant themselves the
 * paid tier.
 *
 * <p><b>Answer 202 quickly, then work.</b> RevenueCat times out and retries; doing the
 * database work inline turns a slow query into a storm of duplicate deliveries. We accept
 * the event and process it after responding — which only works because processing is
 * idempotent.
 */
@RestController
@RequestMapping("/api/v1/billing/webhooks")
@Tag(name = "Billing webhooks", description = "Inbound subscription events from RevenueCat")
public class BillingWebhookController {

    private static final Logger log = LoggerFactory.getLogger(BillingWebhookController.class);

    private final WebhookAuthenticator authenticator;
    private final RevenueCatWebhookProcessor processor;

    public BillingWebhookController(WebhookAuthenticator authenticator, RevenueCatWebhookProcessor processor) {
        this.authenticator = authenticator;
        this.processor = processor;
    }

    @PostMapping("/revenuecat")
    @Operation(summary = "RevenueCat subscription event",
            description = "Authenticated by the shared secret configured in the RevenueCat dashboard. "
                    + "Always answers 202 for an authentic request, including one it decides to ignore.")
    public ResponseEntity<Void> receive(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody RevenueCatWebhookRequest request) {

        if (!authenticator.isAuthentic(authorization)) {
            // Nothing about what was wrong: a detailed rejection is a hint for whoever is
            // probing the endpoint.
            log.warn("Rejected an unauthenticated RevenueCat webhook");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // TODO: hand this to an executor or an outbox so the response really does return
        // first. Called inline for now, which is honest about the current state - the
        // processor throws until it is implemented.
        try {
            processor.process(request);
        } catch (UnsupportedOperationException e) {
            log.warn("RevenueCat webhook accepted but not processed: {} is not implemented yet",
                    request.eventType());
        } catch (RuntimeException e) {
            // Still 202. A retry storm will not fix a bug in our processing, and the
            // payload is recoverable from the log until the event ledger is writing.
            log.error("Failed to process RevenueCat webhook {}", request.eventId(), e);
        }

        return ResponseEntity.accepted().build();
    }
}
