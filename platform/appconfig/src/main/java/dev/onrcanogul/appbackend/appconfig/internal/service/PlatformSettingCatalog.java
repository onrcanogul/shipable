package dev.onrcanogul.appbackend.appconfig.internal.service;

import dev.onrcanogul.appbackend.appconfig.AppConfigProperties;
import dev.onrcanogul.appbackend.appconfig.api.model.SettingDefinition;
import dev.onrcanogul.appbackend.appconfig.api.model.SettingType;
import dev.onrcanogul.appbackend.appconfig.api.port.SettingCatalog;
import dev.onrcanogul.appbackend.core.CoreProperties;
import dev.onrcanogul.appbackend.core.api.port.SettingKeys;
import java.util.List;

/**
 * The settings the platform itself understands.
 *
 * <p>Boot defaults are read from the bound properties rather than hard-coded, so the admin
 * API shows the value this deployment actually starts with - not the one the template
 * author happened to write down.
 *
 * <p>Your app adds its own by publishing another {@link SettingCatalog} bean from
 * {@code domain}; they show up in the admin listing with no change here.
 */
public class PlatformSettingCatalog implements SettingCatalog {

    private final CoreProperties coreProperties;
    private final AppConfigProperties appConfigProperties;

    public PlatformSettingCatalog(CoreProperties coreProperties, AppConfigProperties appConfigProperties) {
        this.coreProperties = coreProperties;
        this.appConfigProperties = appConfigProperties;
    }

    @Override
    public List<SettingDefinition> definitions() {
        return List.of(
                SettingDefinition.of(
                        SettingKeys.RATE_LIMIT_ENABLED,
                        SettingType.BOOLEAN,
                        "true",
                        "Per-IP rate limiting, applied before authentication. "
                                + "Turn it off only to rule it out as the cause of something."),
                SettingDefinition.of(
                        SettingKeys.RATE_LIMIT_PERMITS,
                        SettingType.INTEGER,
                        String.valueOf(coreProperties.rateLimit().permits()),
                        "Requests allowed per IP per window. Lower it while you are being hammered."),
                SettingDefinition.of(
                        SettingKeys.RATE_LIMIT_WINDOW,
                        SettingType.DURATION,
                        coreProperties.rateLimit().window().toString(),
                        "The window those permits are counted over, e.g. 1m."),
                SettingDefinition.of(
                        SettingKeys.VERSION_GATE_ENABLED,
                        SettingType.BOOLEAN,
                        String.valueOf(appConfigProperties.versionGateEnabled()),
                        "Rejects clients below the minimum supported version with 426. "
                                + "The off switch for when the gate itself misfires."),
                SettingDefinition.of(
                        SettingKeys.MAINTENANCE_MODE,
                        SettingType.BOOLEAN,
                        "false",
                        "Tells clients to show a maintenance screen instead of retrying."),
                SettingDefinition.of(
                        SettingKeys.MAINTENANCE_MESSAGE,
                        SettingType.STRING,
                        "",
                        "What the client shows during maintenance. Keep it short and honest."));
    }
}
