package dev.onrcanogul.appbackend.privacy.internal.service;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.core.api.port.UserDataContributor;
import dev.onrcanogul.appbackend.privacy.PrivacyProperties;
import dev.onrcanogul.appbackend.privacy.api.model.DeletionRequest;
import dev.onrcanogul.appbackend.privacy.api.port.AccountDeletionService;
import dev.onrcanogul.appbackend.privacy.internal.persistence.repository.DeletionRequestRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skeleton implementation of {@link AccountDeletionService}.
 *
 * <p>The design decision already made here is the one that matters: erasure fans out over
 * every {@link UserDataContributor} rather than over a hard-coded list of tables. A module
 * added next year joins the deletion flow by implementing the interface — nobody has to
 * remember it.
 *
 * <p>Notes for whoever implements the TODOs, because deletion is unusually easy to get
 * subtly wrong:
 * <ul>
 *   <li><b>Erase per contributor, and record which ones finished.</b> The fan-out spans
 *       modules and can fail halfway; a retry must resume rather than start over or give
 *       up.</li>
 *   <li><b>Delete the identity row last.</b> It is the key everything else is found by. Lose
 *       it first and the remaining data is orphaned rather than deleted — the worst
 *       outcome, since it is both still there and no longer findable.</li>
 *   <li><b>Keep what the law says to keep.</b> Purchase records usually have a retention
 *       obligation that outlives the account. Anonymise those instead of deleting them,
 *       and write down which ones and why.</li>
 *   <li><b>Tell the stores.</b> RevenueCat has its own deletion endpoint; deleting locally
 *       does not delete there.</li>
 * </ul>
 */
public class DefaultAccountDeletionService implements AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAccountDeletionService.class);

    private final DeletionRequestRepository repository;
    private final List<UserDataContributor> contributors;
    private final PrivacyProperties properties;
    private final Clock clock;

    public DefaultAccountDeletionService(
            DeletionRequestRepository repository,
            List<UserDataContributor> contributors,
            PrivacyProperties properties,
            Clock clock) {
        this.repository = repository;
        this.contributors = List.copyOf(contributors);
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public DeletionRequest requestDeletion(UserId userId) {
        // TODO: return the existing PENDING request if there is one - a user tapping twice
        // must not end up with two, and the second must not reset the grace period.
        // Otherwise insert one scheduled for now + properties.deletionGracePeriod().
        throw new UnsupportedOperationException("TODO: requestDeletion is not implemented");
    }

    @Override
    public DeletionRequest cancelDeletion(UserId userId) {
        // TODO: only a PENDING request can be cancelled. Once erasure has started there is
        // nothing to come back to.
        throw new UnsupportedOperationException("TODO: cancelDeletion is not implemented");
    }

    @Override
    public Optional<DeletionRequest> statusOf(UserId userId) {
        // TODO: repository.findFirstByUserIdOrderByRequestedAtDesc(...).map(this::toModel)
        throw new UnsupportedOperationException("TODO: statusOf is not implemented");
    }

    @Override
    public int processDueDeletions() {
        // TODO: for each due request, walk `contributors` calling eraseFor, recording each
        // completed dataSetName so a crash resumes rather than restarts, then mark the
        // request COMPLETED.
        log.debug("Deletion sweep found {} contributors registered", contributors.size());
        throw new UnsupportedOperationException("TODO: processDueDeletions is not implemented");
    }

    /**
     * The contributors that will be asked to erase.
     *
     * <p>Exposed so a test can assert every module that stores user data is present — the
     * check that keeps deletion honest as the app grows.
     */
    public List<String> registeredDataSets() {
        return contributors.stream().map(UserDataContributor::dataSetName).sorted().toList();
    }
}
