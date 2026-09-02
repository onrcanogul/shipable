package dev.onrcanogul.appbackend.appconfig.internal.service;

import dev.onrcanogul.appbackend.appconfig.api.model.SettingDefinition;
import dev.onrcanogul.appbackend.appconfig.api.model.SettingView;
import dev.onrcanogul.appbackend.appconfig.api.port.SettingCatalog;
import dev.onrcanogul.appbackend.appconfig.api.port.SettingsAdminService;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.entity.AppSettingEntity;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.AppSettingRepository;
import dev.onrcanogul.appbackend.core.api.error.NotFoundException;
import dev.onrcanogul.appbackend.core.api.error.ValidationException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and writes setting overrides for the admin API.
 *
 * <p>Two rules hold the whole thing together:
 *
 * <p><b>Only catalogued keys can be written.</b> An unknown key is a 404, not a new row.
 * Without that the table fills with typos, each one a setting somebody believes they
 * changed and which is silently doing nothing.
 *
 * <p><b>The value is validated against the declared type before it is stored.</b> A bad
 * value written now is read on every request afterwards, long after whoever typed it has
 * moved on.
 */
public class DefaultSettingsAdminService implements SettingsAdminService {

    private static final Logger log = LoggerFactory.getLogger(DefaultSettingsAdminService.class);

    private final AppSettingRepository repository;
    private final DatabaseRuntimeSettings runtimeSettings;
    private final Map<String, SettingDefinition> catalog;

    public DefaultSettingsAdminService(
            AppSettingRepository repository,
            DatabaseRuntimeSettings runtimeSettings,
            List<SettingCatalog> catalogs) {
        this.repository = repository;
        this.runtimeSettings = runtimeSettings;
        this.catalog = catalogs.stream()
                .flatMap(entry -> entry.definitions().stream())
                .sorted(Comparator.comparing(SettingDefinition::key))
                .collect(LinkedHashMap::new,
                        (map, definition) -> map.put(definition.key(), definition),
                        LinkedHashMap::putAll);
    }

    @Override
    public List<SettingView> list() {
        return catalog.values().stream().map(this::toView).toList();
    }

    @Override
    public SettingView get(String key) {
        return toView(definitionOf(key));
    }

    @Override
    @Transactional
    public SettingView set(String key, String value, String updatedBy) {
        SettingDefinition definition = definitionOf(key);

        if (value == null || value.isBlank()) {
            throw new ValidationException("Value must not be blank. Use reset to restore the default.");
        }
        try {
            definition.type().validate(value);
        } catch (RuntimeException e) {
            throw new ValidationException(
                    "'" + value + "' is not a valid " + definition.type() + " for " + key
                            + " (" + e.getMessage() + ")");
        }

        repository.findBySettingKey(key)
                .ifPresentOrElse(
                        existing -> existing.update(value.trim(), updatedBy),
                        () -> repository.save(new AppSettingEntity(key, value.trim(), updatedBy)));

        // Logged at INFO on purpose: a change to a production limit should be findable
        // afterwards without anyone having thought to enable debug logging first.
        log.info("Setting '{}' changed to '{}' by {}", key, value.trim(), updatedBy);

        runtimeSettings.reload();
        return toView(definition);
    }

    @Override
    @Transactional
    public SettingView reset(String key, String updatedBy) {
        SettingDefinition definition = definitionOf(key);
        repository.deleteBySettingKey(key);
        log.info("Setting '{}' reset to its default by {}", key, updatedBy);

        runtimeSettings.reload();
        return toView(definition);
    }

    private SettingDefinition definitionOf(String key) {
        SettingDefinition definition = catalog.get(key);
        if (definition == null) {
            throw new NotFoundException("Setting '" + key + "'");
        }
        return definition;
    }

    private SettingView toView(SettingDefinition definition) {
        return repository.findBySettingKey(definition.key())
                .map(stored -> new SettingView(
                        definition.key(),
                        definition.type(),
                        stored.getSettingValue(),
                        definition.bootDefault(),
                        true,
                        definition.description(),
                        stored.getUpdatedAt(),
                        stored.getUpdatedBy()))
                .orElseGet(() -> new SettingView(
                        definition.key(),
                        definition.type(),
                        definition.bootDefault(),
                        definition.bootDefault(),
                        false,
                        definition.description(),
                        null,
                        null));
    }
}
