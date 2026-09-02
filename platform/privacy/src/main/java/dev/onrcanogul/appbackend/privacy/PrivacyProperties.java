package dev.onrcanogul.appbackend.privacy;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings for deletion and export, bound from {@code app.privacy.*}.
 *
 * @param deletionGracePeriod how long a user has to change their mind. Long enough to be a
 *                            real safety net, short enough to still read as "deleted" -
 *                            days, not months. Zero means erase immediately, which is
 *                            allowed but leaves no way back from a mis-tap
 */
@Validated
@ConfigurationProperties(prefix = "app.privacy")
public record PrivacyProperties(@NotNull Duration deletionGracePeriod) {

    public PrivacyProperties {
        deletionGracePeriod = deletionGracePeriod == null ? Duration.ofDays(7) : deletionGracePeriod;
    }
}
