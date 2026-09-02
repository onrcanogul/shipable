package dev.onrcanogul.appbackend.appconfig.api.port;

import dev.onrcanogul.appbackend.appconfig.api.model.PlatformConfigView;
import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import java.util.List;

/** Managing version gating per platform. Used by the admin module. */
public interface PlatformConfigAdminService {

    List<PlatformConfigView> list();

    /**
     * Sets the minimum and latest version for a platform.
     *
     * <p>Versions are validated as versions, not stored as text. A typo here rejects every
     * client on the platform, and the error should land on the person making the change
     * rather than on the users.
     */
    PlatformConfigView update(
            ClientPlatform platform, String minimumSupportedVersion, String latestVersion, String updateUrl);
}
