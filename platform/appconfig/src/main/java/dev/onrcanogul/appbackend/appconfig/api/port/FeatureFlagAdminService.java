package dev.onrcanogul.appbackend.appconfig.api.port;

import dev.onrcanogul.appbackend.appconfig.api.model.FeatureFlagView;
import java.util.List;

/**
 * Managing feature flags. Used by the admin module.
 *
 * <p>Separate from {@link FeatureFlags}, which is the read side used on hot paths: the code
 * that checks a flag must not be able to flip it.
 */
public interface FeatureFlagAdminService {

    List<FeatureFlagView> list();

    /**
     * Creates the flag if it does not exist.
     *
     * <p>Upsert rather than a separate create: a flag is usually first written from the
     * admin API in the moment you need it off, and a 404 then is not helpful.
     */
    FeatureFlagView upsert(String key, boolean enabled, Boolean exposedToClient, String description);

    void delete(String key);
}
