package dev.onrcanogul.appbackend.analytics.api.model;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import java.time.Instant;
import java.util.Map;

/**
 * One product event.
 *
 * @param userId     may be null for events that happen before sign-in
 * @param properties keep these free of personal data. Whatever you record here ends up in
 *                   whichever third party you plug in later, and you will not remember
 *                   what you put in it
 */
public record AnalyticsEvent(String name, UserId userId, Map<String, Object> properties, Instant occurredAt) {

    public AnalyticsEvent {
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    public static AnalyticsEvent of(String name, UserId userId) {
        return new AnalyticsEvent(name, userId, Map.of(), Instant.now());
    }
}
