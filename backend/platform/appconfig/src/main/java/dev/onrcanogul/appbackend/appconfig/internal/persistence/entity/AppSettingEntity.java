package dev.onrcanogul.appbackend.appconfig.internal.persistence.entity;

import dev.onrcanogul.appbackend.core.api.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One stored setting override.
 *
 * <p>Only overrides live here. A setting nobody has changed has no row, which keeps the
 * table small and makes "what has actually been touched in production" a single query.
 */
@Entity
@Table(name = "app_setting", schema = "appconfig")
public class AppSettingEntity extends BaseEntity {

    @Column(name = "setting_key", nullable = false, length = 190)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 2048)
    private String settingValue;

    /** Who changed it. An untraceable change to a production limit is a bad afternoon. */
    @Column(name = "updated_by", length = 190)
    private String updatedBy;

    protected AppSettingEntity() {
        // for JPA
    }

    public AppSettingEntity(String settingKey, String settingValue, String updatedBy) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.updatedBy = updatedBy;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void update(String settingValue, String updatedBy) {
        this.settingValue = settingValue;
        this.updatedBy = updatedBy;
    }
}
