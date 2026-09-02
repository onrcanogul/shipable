package dev.onrcanogul.appbackend.core.api.context;

import java.util.Locale;

/**
 * Which client is calling.
 *
 * <p>Worth knowing per request: version gating, store-specific billing behaviour and
 * analytics all need it, and reading it from the User-Agent later is guesswork.
 */
public enum ClientPlatform {
    IOS,
    ANDROID,
    WEB,
    UNKNOWN;

    public static ClientPlatform parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
