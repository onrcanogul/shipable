package dev.onrcanogul.appbackend.appconfig.api.model;

/**
 * A setting the system knows about: its key, type, default and what it does.
 *
 * <p>The catalog is what makes the admin API usable rather than a raw key-value editor.
 * Without it, whoever is on call at 3am has to already know that
 * {@code core.rate-limit.window} takes {@code 1m} and not {@code 60}.
 *
 * @param bootDefault the value from {@code application.yml}, shown so an operator can see
 *                    what they are overriding and what "reset" would restore
 */
public record SettingDefinition(
        String key,
        SettingType type,
        String bootDefault,
        String description) {

    public static SettingDefinition of(String key, SettingType type, String bootDefault, String description) {
        return new SettingDefinition(key, type, bootDefault, description);
    }
}
