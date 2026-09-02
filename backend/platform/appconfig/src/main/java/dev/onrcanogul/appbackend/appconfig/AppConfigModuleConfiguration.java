package dev.onrcanogul.appbackend.appconfig;

import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlagAdminService;
import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlags;
import dev.onrcanogul.appbackend.appconfig.api.port.PlatformConfigAdminService;
import dev.onrcanogul.appbackend.appconfig.api.port.RemoteConfigService;
import dev.onrcanogul.appbackend.core.api.settings.SettingCatalog;
import dev.onrcanogul.appbackend.appconfig.api.port.SettingsAdminService;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.AppSettingRepository;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.FeatureFlagRepository;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.PlatformConfigRepository;
import dev.onrcanogul.appbackend.appconfig.internal.service.DatabaseFeatureFlags;
import dev.onrcanogul.appbackend.appconfig.internal.service.DatabaseRuntimeSettings;
import dev.onrcanogul.appbackend.appconfig.internal.service.DefaultFeatureFlagAdminService;
import dev.onrcanogul.appbackend.appconfig.internal.service.DefaultPlatformConfigAdminService;
import dev.onrcanogul.appbackend.appconfig.internal.service.DefaultRemoteConfigService;
import dev.onrcanogul.appbackend.appconfig.internal.service.DefaultSettingsAdminService;
import dev.onrcanogul.appbackend.appconfig.internal.service.PlatformSettingCatalog;
import dev.onrcanogul.appbackend.appconfig.internal.web.MinimumVersionFilter;
import dev.onrcanogul.appbackend.appconfig.internal.web.RemoteConfigController;
import dev.onrcanogul.appbackend.core.CoreProperties;
import dev.onrcanogul.appbackend.core.api.settings.RuntimeSettings;
import dev.onrcanogul.appbackend.core.api.web.ProblemResponseWriter;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;

/**
 * Everything the appconfig module hands to the outside world.
 *
 * <p>It also supplies the database-backed {@link RuntimeSettings} that {@code core} falls
 * back to a no-op for. That is the inversion that makes runtime settings work: core defines
 * the port and reads through it, this module persists the values, and neither knows about
 * the admin API that writes them.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppConfigProperties.class)
public class AppConfigModuleConfiguration {

    /**
     * Marked {@code @Primary} so it wins over core's no-op fallback without core having to
     * know this module exists.
     */
    @Bean
    @Primary
    public DatabaseRuntimeSettings databaseRuntimeSettings(AppSettingRepository repository) {
        return new DatabaseRuntimeSettings(repository);
    }

    @Bean
    public SettingCatalog platformSettingCatalog(
            CoreProperties coreProperties, AppConfigProperties appConfigProperties) {
        return new PlatformSettingCatalog(coreProperties, appConfigProperties);
    }

    @Bean
    public SettingsAdminService settingsAdminService(
            AppSettingRepository repository,
            DatabaseRuntimeSettings runtimeSettings,
            List<SettingCatalog> catalogs) {
        return new DefaultSettingsAdminService(repository, runtimeSettings, catalogs);
    }

    @Bean
    public DatabaseFeatureFlags featureFlags(FeatureFlagRepository repository) {
        return new DatabaseFeatureFlags(repository);
    }

    @Bean
    public DefaultRemoteConfigService remoteConfigService(
            PlatformConfigRepository repository,
            FeatureFlags featureFlags,
            RuntimeSettings settings,
            AppConfigProperties properties) {
        return new DefaultRemoteConfigService(repository, featureFlags, settings, properties);
    }

    /**
     * Runs after the request context is built (it needs the version header) but before
     * authentication: an unsupported client should be told to update rather than told its
     * token is bad.
     *
     * <p>Whether the gate is on is decided per request from {@link RuntimeSettings}, so it
     * can be switched off from the admin API if it ever misfires.
     */
    @Bean
    public FilterRegistrationBean<MinimumVersionFilter> minimumVersionFilter(
            RemoteConfigService remoteConfigService, ProblemResponseWriter problemWriter) {
        FilterRegistrationBean<MinimumVersionFilter> registration =
                new FilterRegistrationBean<>(new MinimumVersionFilter(remoteConfigService, problemWriter));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 35);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FeatureFlagAdminService featureFlagAdminService(
            FeatureFlagRepository repository, DatabaseFeatureFlags readSide) {
        return new DefaultFeatureFlagAdminService(repository, readSide);
    }

    @Bean
    public PlatformConfigAdminService platformConfigAdminService(
            PlatformConfigRepository repository, DefaultRemoteConfigService readSide) {
        return new DefaultPlatformConfigAdminService(repository, readSide);
    }

    @Bean
    public RemoteConfigController remoteConfigController(RemoteConfigService remoteConfigService) {
        return new RemoteConfigController(remoteConfigService);
    }
}
