package dev.onrcanogul.appbackend.appconfig.internal.service;

import dev.onrcanogul.appbackend.appconfig.api.model.AppVersion;
import dev.onrcanogul.appbackend.appconfig.api.model.PlatformConfigView;
import dev.onrcanogul.appbackend.appconfig.api.port.PlatformConfigAdminService;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.entity.PlatformConfigEntity;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.PlatformConfigRepository;
import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import dev.onrcanogul.appbackend.core.api.error.ValidationException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sets the minimum and latest client version per platform.
 *
 * <p>This is the most destructive thing the admin API can do: raising the minimum version
 * locks out every user on an older build until they update, and there is no gradual
 * rollout. So both values are parsed as versions before anything is written, and a minimum
 * above the latest is refused — that combination locks out everyone including people who
 * just updated.
 */
public class DefaultPlatformConfigAdminService implements PlatformConfigAdminService {

    private static final Logger log = LoggerFactory.getLogger(DefaultPlatformConfigAdminService.class);

    private final PlatformConfigRepository repository;
    private final DefaultRemoteConfigService readSide;

    public DefaultPlatformConfigAdminService(
            PlatformConfigRepository repository, DefaultRemoteConfigService readSide) {
        this.repository = repository;
        this.readSide = readSide;
    }

    @Override
    public List<PlatformConfigView> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(PlatformConfigEntity::getPlatform))
                .map(DefaultPlatformConfigAdminService::toView)
                .toList();
    }

    @Override
    @Transactional
    public PlatformConfigView update(
            ClientPlatform platform, String minimumSupportedVersion, String latestVersion, String updateUrl) {

        AppVersion minimum = parse(minimumSupportedVersion, "minimumSupportedVersion");
        AppVersion latest = parse(latestVersion, "latestVersion");

        if (latest.isOlderThan(minimum)) {
            throw new ValidationException(
                    "latestVersion (" + latest + ") is older than minimumSupportedVersion (" + minimum
                            + "), which would lock out every client including up-to-date ones");
        }

        PlatformConfigEntity config = repository.findByPlatform(platform)
                .orElseGet(() -> repository.save(PlatformConfigEntity.create(platform)));
        config.updateVersions(minimum.toString(), latest.toString(), updateUrl);

        log.info("Version gate for {} set to minimum={} latest={}", platform, minimum, latest);

        readSide.reload();
        return toView(config);
    }

    private static AppVersion parse(String raw, String field) {
        AppVersion version = AppVersion.parseOrNull(raw);
        if (version == null) {
            throw new ValidationException(field + " is not a version: " + raw);
        }
        return version;
    }

    private static PlatformConfigView toView(PlatformConfigEntity entity) {
        return new PlatformConfigView(
                entity.getPlatform(),
                entity.getMinimumSupportedVersion(),
                entity.getLatestVersion(),
                entity.getUpdateUrl(),
                entity.getUpdatedAt());
    }
}
