package dev.onrcanogul.appbackend.appconfig.internal.service;

import dev.onrcanogul.appbackend.appconfig.api.model.FeatureFlagView;
import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlagAdminService;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.entity.FeatureFlagEntity;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.FeatureFlagRepository;
import dev.onrcanogul.appbackend.core.api.error.NotFoundException;
import dev.onrcanogul.appbackend.core.api.error.ValidationException;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, flips and deletes feature flags.
 *
 * <p>Writes go straight to the database and the read-side snapshot picks them up on its
 * next refresh, plus immediately on this instance. That is deliberate: a flag exists so you
 * can turn something off <i>now</i>, and waiting on a deploy defeats the purpose.
 */
public class DefaultFeatureFlagAdminService implements FeatureFlagAdminService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFeatureFlagAdminService.class);

    /** Keeps keys greppable and safe to put in a URL path. */
    private static final Pattern VALID_KEY = Pattern.compile("^[a-z0-9][a-z0-9._-]{1,126}[a-z0-9]$");

    private final FeatureFlagRepository repository;
    private final DatabaseFeatureFlags readSide;

    public DefaultFeatureFlagAdminService(FeatureFlagRepository repository, DatabaseFeatureFlags readSide) {
        this.repository = repository;
        this.readSide = readSide;
    }

    @Override
    public List<FeatureFlagView> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(FeatureFlagEntity::getFlagKey))
                .map(DefaultFeatureFlagAdminService::toView)
                .toList();
    }

    @Override
    @Transactional
    public FeatureFlagView upsert(String key, boolean enabled, Boolean exposedToClient, String description) {
        if (key == null || !VALID_KEY.matcher(key).matches()) {
            throw new ValidationException(
                    "Flag key must be lowercase letters, digits, dot, dash or underscore: " + key);
        }

        FeatureFlagEntity flag = repository.findByFlagKey(key)
                .orElseGet(() -> repository.save(FeatureFlagEntity.create(key)));

        flag.update(
                enabled,
                // null leaves the existing exposure alone, so flipping a flag on cannot
                // accidentally publish a server-only flag to every client.
                exposedToClient != null ? exposedToClient : flag.isExposedToClient(),
                description != null ? description : flag.getDescription());

        log.info("Feature flag '{}' set to enabled={} exposedToClient={}",
                key, enabled, flag.isExposedToClient());

        readSide.reload();
        return toView(flag);
    }

    @Override
    @Transactional
    public void delete(String key) {
        FeatureFlagEntity flag = repository.findByFlagKey(key)
                .orElseThrow(() -> new NotFoundException("Feature flag '" + key + "'"));
        repository.delete(flag);
        log.info("Feature flag '{}' deleted", key);
        readSide.reload();
    }

    private static FeatureFlagView toView(FeatureFlagEntity entity) {
        return new FeatureFlagView(
                entity.getFlagKey(),
                entity.isEnabled(),
                entity.isExposedToClient(),
                entity.getDescription(),
                entity.getUpdatedAt());
    }
}
