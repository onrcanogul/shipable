package dev.onrcanogul.appbackend.quota.api.model;

import java.time.Instant;

/**
 * The answer to "may this go ahead?", asked before the money is spent.
 *
 * @param remaining what is left in the window; -1 when unlimited
 * @param resetsAt  when the window rolls over, so the client can say "try again at" rather
 *                  than "try again later"
 */
public record QuotaDecision(boolean allowed, QuotaKey key, long remaining, Instant resetsAt, String reason) {

    public static QuotaDecision allow(QuotaKey key, long remaining, Instant resetsAt) {
        return new QuotaDecision(true, key, remaining, resetsAt, null);
    }

    public static QuotaDecision deny(QuotaKey key, Instant resetsAt, String reason) {
        return new QuotaDecision(false, key, 0L, resetsAt, reason);
    }
}
