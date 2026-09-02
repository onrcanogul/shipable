package dev.onrcanogul.appbackend.privacy.api.port;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.privacy.api.model.DataExport;

/**
 * Data portability: hand the user everything you hold about them.
 *
 * <p>Assembled from every {@code UserDataContributor}, so a new module that stores user
 * data joins the export by implementing an interface rather than by being remembered.
 */
public interface DataExportService {

    DataExport exportFor(UserId userId);
}
