package dev.onrcanogul.appbackend.appconfig.api.model;

/**
 * Version gating for one client platform.
 *
 * @param minimumSupportedVersion below this, the client must update before it can be used
 * @param latestVersion           what is in the store, for a soft "update available" prompt
 * @param updateUrl               where to send the user; App Store and Play differ
 */
public record PlatformConfig(
        AppVersion minimumSupportedVersion,
        AppVersion latestVersion,
        String updateUrl) {
}
