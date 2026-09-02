package dev.onrcanogul.appbackend.billing.internal.service;

import dev.onrcanogul.appbackend.billing.api.dto.RevenueCatWebhookRequest;
import dev.onrcanogul.appbackend.billing.internal.persistence.repository.ProcessedWebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies a RevenueCat webhook event to our snapshot.
 *
 * <p>Separate from the controller because the two have genuinely different jobs: the
 * controller decides whether to accept the request, this decides what it means.
 *
 * <p><b>The processing contract, which is where webhook handling usually goes wrong:</b>
 * <ul>
 *   <li>Deduplicate on the event id <i>in the same transaction</i> as the state change. A
 *       dedup check that commits separately is a race, and RevenueCat will find it.</li>
 *   <li>Never trust the event body for the resulting state. The event says <i>something
 *       happened</i>; re-fetch the customer to learn what is true now. Events can arrive
 *       out of order, and an out-of-order CANCELLATION applied literally revokes a
 *       subscription the user has already renewed.</li>
 *   <li>Return 2xx for anything you have decided not to act on. A non-2xx makes RevenueCat
 *       retry, and retrying will not make an unknown event type known.</li>
 * </ul>
 *
 * <p>The event types worth handling: INITIAL_PURCHASE, RENEWAL, CANCELLATION,
 * UNCANCELLATION, NON_RENEWING_PURCHASE, EXPIRATION, BILLING_ISSUE, PRODUCT_CHANGE,
 * TRANSFER, SUBSCRIPTION_PAUSED. TEST exists to make the dashboard button do something —
 * accept and ignore it.
 */
public class RevenueCatWebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(RevenueCatWebhookProcessor.class);

    private final ProcessedWebhookEventRepository processedEvents;

    public RevenueCatWebhookProcessor(ProcessedWebhookEventRepository processedEvents) {
        this.processedEvents = processedEvents;
    }

    /**
     * Applies the event.
     *
     * <p>Runs after the controller has already answered 202, so it must not throw its way
     * out to a caller that is no longer listening. Failures belong in the log and in the
     * stored payload, to be replayed later.
     */
    public void process(RevenueCatWebhookRequest request) {
        String eventId = request.eventId();
        if (eventId == null) {
            log.warn("RevenueCat webhook without an event id, ignoring");
            return;
        }

        // TODO: in one transaction - insert into processed_webhook_event (the unique index
        // on event_id is what actually makes this idempotent when two deliveries race),
        // then refresh this user's snapshot from the provider.
        throw new UnsupportedOperationException("TODO: RevenueCat webhook processing is not implemented");
    }
}
