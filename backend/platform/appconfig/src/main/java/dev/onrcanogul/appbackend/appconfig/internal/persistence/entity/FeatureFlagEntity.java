package dev.onrcanogul.appbackend.appconfig.internal.persistence.entity;

import dev.onrcanogul.appbackend.core.api.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One server-side switch.
 *
 * <p>{@code exposedToClient} matters: some flags decide what the app draws, others decide
 * what the server does. Sending the second kind to the client tells everyone what you are
 * about to launch.
 */
@Entity
@Table(name = "feature_flag", schema = "appconfig")
public class FeatureFlagEntity extends BaseEntity {

    @Column(name = "flag_key", nullable = false, length = 128)
    private String flagKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "exposed_to_client", nullable = false)
    private boolean exposedToClient;

    @Column(name = "description", length = 512)
    private String description;

    protected FeatureFlagEntity() {
        // for JPA
    }

    /** A new flag starts off and server-only: the safe state for something unfinished. */
    public static FeatureFlagEntity create(String flagKey) {
        FeatureFlagEntity flag = new FeatureFlagEntity();
        flag.flagKey = flagKey;
        flag.enabled = false;
        flag.exposedToClient = false;
        return flag;
    }

    public void update(boolean enabled, boolean exposedToClient, String description) {
        this.enabled = enabled;
        this.exposedToClient = exposedToClient;
        this.description = description;
    }

    public String getFlagKey() {
        return flagKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isExposedToClient() {
        return exposedToClient;
    }

    public String getDescription() {
        return description;
    }
}
