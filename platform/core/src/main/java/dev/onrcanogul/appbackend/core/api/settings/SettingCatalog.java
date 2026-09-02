package dev.onrcanogul.appbackend.core.api.settings;

import dev.onrcanogul.appbackend.core.api.settings.SettingDefinition;
import java.util.List;

/**
 * The settings a module declares as changeable at runtime.
 *
 * <p>Each module publishes one of these as a bean; the admin API concatenates them. Your
 * app declares its own by publishing another, and its settings appear in the admin list
 * with no change to the admin module.
 *
 * <p>A key that is not in any catalog cannot be written. That is deliberate: without it the
 * settings table becomes a junk drawer of typos, each one silently doing nothing.
 */
public interface SettingCatalog {

    List<SettingDefinition> definitions();
}
