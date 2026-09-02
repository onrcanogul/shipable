package dev.onrcanogul.appbackend.privacy.api.model;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import java.time.Instant;
import java.util.Map;

/**
 * Everything the app holds about one user, collected from every module.
 *
 * @param dataSets keyed by {@code UserDataContributor.dataSetName()}, so the export names
 *                 where each part came from rather than presenting one flat blob
 */
public record DataExport(UserId userId, Instant generatedAt, Map<String, Object> dataSets) {

    public DataExport {
        dataSets = dataSets == null ? Map.of() : Map.copyOf(dataSets);
    }
}
