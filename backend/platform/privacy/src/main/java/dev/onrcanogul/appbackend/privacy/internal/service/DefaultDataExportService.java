package dev.onrcanogul.appbackend.privacy.internal.service;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.core.api.port.UserDataContributor;
import dev.onrcanogul.appbackend.privacy.api.model.DataExport;
import dev.onrcanogul.appbackend.privacy.api.port.DataExportService;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects the export from every {@link UserDataContributor}.
 *
 * <p>This one is implemented: the fan-out is the whole feature, and each contributor
 * already knows how to answer for itself. Contributors currently return empty maps, so the
 * export is a real document with empty sections rather than an error.
 */
public class DefaultDataExportService implements DataExportService {

    private final List<UserDataContributor> contributors;
    private final Clock clock;

    public DefaultDataExportService(List<UserDataContributor> contributors, Clock clock) {
        this.contributors = List.copyOf(contributors);
        this.clock = clock;
    }

    @Override
    public DataExport exportFor(UserId userId) {
        // LinkedHashMap so the document has a stable section order between runs; a user
        // diffing two exports should not see everything move.
        Map<String, Object> dataSets = new LinkedHashMap<>();
        contributors.stream()
                .sorted(java.util.Comparator.comparing(UserDataContributor::dataSetName))
                .forEach(contributor -> dataSets.put(contributor.dataSetName(), contributor.exportFor(userId)));

        return new DataExport(userId, clock.instant(), dataSets);
    }
}
