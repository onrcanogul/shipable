package dev.onrcanogul.appbackend.appconfig.api.model;

import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import java.time.Instant;

/** Version gating for one platform, as the admin API shows it. */
public record PlatformConfigView(
        ClientPlatform platform,
        String minimumSupportedVersion,
        String latestVersion,
        String updateUrl,
        Instant updatedAt) {
}
