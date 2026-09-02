package dev.onrcanogul.appbackend.quota.api.port;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.quota.api.model.QuotaDecision;
import dev.onrcanogul.appbackend.quota.api.model.QuotaExceededException;
import dev.onrcanogul.appbackend.quota.api.model.QuotaKey;

/**
 * Checks and records consumption per user.
 *
 * <p><b>Order matters:</b> {@link #check} runs <i>before</i> the expensive work,
 * {@link #record} runs after it with what was actually spent. Recording an estimate up
 * front and never correcting it is how quota accounting quietly drifts away from reality.
 *
 * <p>Distinct from core's rate limiter: that one stops abuse from strangers and is keyed
 * by IP; this one enforces what a known user paid for, and its numbers come from
 * {@link QuotaPolicy}.
 */
public interface QuotaService {

    /** Non-mutating: asks whether {@code requestedAmount} would fit. */
    QuotaDecision check(UserId userId, QuotaKey key, long requestedAmount);

    /** Records what was actually consumed, after the work completed. */
    void record(UserId userId, QuotaKey key, long consumedAmount);

    /** For callers that would rather fail fast than branch. */
    default void checkOrThrow(UserId userId, QuotaKey key, long requestedAmount) {
        QuotaDecision decision = check(userId, key, requestedAmount);
        if (!decision.allowed()) {
            throw new QuotaExceededException(decision);
        }
    }
}
