package dev.onrcanogul.appbackend.appconfig.internal.persistence.entity;

import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import dev.onrcanogul.appbackend.core.api.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Version gating for one platform, in the database rather than in a config file.
 *
 * <p>In the database on purpose: raising the minimum version is something you do in a
 * hurry, at a bad moment, because a shipped build is doing damage. Needing a redeploy for
 * that is how the redeploy becomes the outage.
 */
@Entity
@Table(name = "platform_config", schema = "appconfig")
public class PlatformConfigEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 16)
    private ClientPlatform platform;

    @Column(name = "minimum_supported_version", nullable = false, length = 32)
    private String minimumSupportedVersion;

    @Column(name = "latest_version", nullable = false, length = 32)
    private String latestVersion;

    @Column(name = "update_url", length = 512)
    private String updateUrl;

    @Column(name = "maintenance_mode", nullable = false)
    private boolean maintenanceMode;

    @Column(name = "maintenance_message", length = 512)
    private String maintenanceMessage;

    protected PlatformConfigEntity() {
        // for JPA
    }

    public ClientPlatform getPlatform() {
        return platform;
    }

    public String getMinimumSupportedVersion() {
        return minimumSupportedVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public String getMaintenanceMessage() {
        return maintenanceMessage;
    }
}
