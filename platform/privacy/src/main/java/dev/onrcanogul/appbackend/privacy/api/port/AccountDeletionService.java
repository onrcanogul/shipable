package dev.onrcanogul.appbackend.privacy.api.port;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.privacy.api.model.DeletionRequest;
import java.util.Optional;

/**
 * In-app account deletion.
 *
 * <p>Not optional: both Apple and Google require an app that lets people create an account
 * to let them delete it from inside the app. "E-mail us" is a rejection.
 */
public interface AccountDeletionService {

    /** Schedules deletion after a grace period. Idempotent: asking twice does not queue two. */
    DeletionRequest requestDeletion(UserId userId);

    /** Cancels a pending request. Only possible during the grace period. */
    DeletionRequest cancelDeletion(UserId userId);

    Optional<DeletionRequest> statusOf(UserId userId);

    /**
     * Runs the erasure for requests whose grace period has passed.
     *
     * <p>Called by a scheduled job. Must be safe to run concurrently with itself and safe
     * to re-run after a crash halfway through.
     */
    int processDueDeletions();
}
