package dev.onrcanogul.appbackend.appconfig;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings for remote config, bound from {@code app.config.*}.
 *
 * @param defaultMinimumVersion used until the database has a row for a platform. Kept at
 *                              a version everything satisfies, so an empty table does not
 *                              lock every client out
 * @param versionGateEnabled    lets you switch the whole gate off without a deploy if it
 *                              ever misfires. A gate that can lock out all your users
 *                              should have an off switch
 */
@Validated
@ConfigurationProperties(prefix = "app.config")
public record AppConfigProperties(@NotBlank String defaultMinimumVersion, boolean versionGateEnabled) {

    public AppConfigProperties {
        defaultMinimumVersion = defaultMinimumVersion == null || defaultMinimumVersion.isBlank()
                ? "0.0.0"
                : defaultMinimumVersion;
    }
}
