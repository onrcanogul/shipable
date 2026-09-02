package dev.onrcanogul.appbackend.notifications.api.model;

import java.util.Map;

/**
 * A push to send.
 *
 * @param data silent payload the app reads on open; keep it small, both services cap the
 *             total notification size
 */
public record PushMessage(String title, String body, Map<String, String> data) {

    public PushMessage {
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static PushMessage of(String title, String body) {
        return new PushMessage(title, body, Map.of());
    }
}
