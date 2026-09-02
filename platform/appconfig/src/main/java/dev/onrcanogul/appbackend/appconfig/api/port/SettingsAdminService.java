package dev.onrcanogul.appbackend.appconfig.api.port;

import dev.onrcanogul.appbackend.appconfig.api.model.SettingView;
import java.util.List;

/**
 * Reading and changing settings at runtime. Used by the admin module.
 *
 * <p>Separate from {@code RuntimeSettings}, which is read-only and lives in {@code core}:
 * a module that reads a setting must not be able to change it.
 */
public interface SettingsAdminService {

    /** Every known setting, whether or not it has been overridden. */
    List<SettingView> list();

    SettingView get(String key);

    /**
     * Stores an override.
     *
     * @param updatedBy who made the change, for the audit trail. An untraceable change to
     *                  a production limit is the kind of thing nobody remembers making
     * @throws dev.onrcanogul.appbackend.core.api.error.AppException 404 for an unknown key,
     *         400 when the value does not fit the declared type
     */
    SettingView set(String key, String value, String updatedBy);

    /** Drops the override, so the boot default applies again. */
    SettingView reset(String key, String updatedBy);
}
