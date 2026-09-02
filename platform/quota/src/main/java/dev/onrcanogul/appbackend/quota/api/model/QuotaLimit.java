package dev.onrcanogul.appbackend.quota.api.model;

import java.time.Duration;

/**
 * How much of {@code key} may be spent per {@code window}.
 *
 * @param amount allowance within the window; negative means unlimited
 */
public record QuotaLimit(QuotaKey key, long amount, Duration window) {

    public static QuotaLimit of(QuotaKey key, long amount, Duration window) {
        return new QuotaLimit(key, amount, window);
    }

    public static QuotaLimit unlimited(QuotaKey key) {
        return new QuotaLimit(key, -1L, Duration.ZERO);
    }

    public boolean isUnlimited() {
        return amount < 0;
    }
}
