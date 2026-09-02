package dev.onrcanogul.appbackend.billing.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * The envelope RevenueCat POSTs to us.
 *
 * <p>Unknown fields are ignored deliberately: RevenueCat adds event types and fields over
 * time, and a webhook endpoint that 400s on an unfamiliar payload turns a new provider
 * feature into an outage.
 *
 * <p>The event is kept as a loose map for the same reason. What we actually need from it is
 * the id (for deduplication), the type, and the {@code app_user_id}; the rest is stored
 * raw for debugging and re-processing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueCatWebhookRequest(Map<String, Object> event, String apiVersion) {

    public RevenueCatWebhookRequest {
        event = event == null ? Map.of() : Map.copyOf(event);
    }

    public String eventId() {
        return stringField("id");
    }

    public String eventType() {
        return stringField("type");
    }

    /** The RevenueCat {@code app_user_id}, which is our {@code UserId}. */
    public String appUserId() {
        return stringField("app_user_id");
    }

    private String stringField(String name) {
        Object value = event.get(name);
        return value == null ? null : value.toString();
    }
}
