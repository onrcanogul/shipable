package dev.onrcanogul.appbackend.billing.api.model;

import java.util.Locale;

/** Where the subscription was bought, as RevenueCat reports it. */
public enum Store {
    APP_STORE,
    PLAY_STORE,
    STRIPE,
    /** RevenueCat's own test purchases, and anything we do not recognise. */
    OTHER;

    public static Store parse(String raw) {
        if (raw == null) {
            return OTHER;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "APP_STORE", "MAC_APP_STORE" -> APP_STORE;
            case "PLAY_STORE" -> PLAY_STORE;
            case "STRIPE", "RC_BILLING" -> STRIPE;
            default -> OTHER;
        };
    }
}
