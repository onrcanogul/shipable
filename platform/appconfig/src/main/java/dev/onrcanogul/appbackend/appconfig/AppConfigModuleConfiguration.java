package dev.onrcanogul.appbackend.appconfig;

import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlags;
import dev.onrcanogul.appbackend.appconfig.api.port.RemoteConfigService;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.FeatureFlagRepository;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.PlatformConfigRepository;
import dev.onrcanogul.appbackend.appconfig.internal.service.DatabaseFeatureFlags;
import dev.onrcanogul.appbackend.appconfig.internal.service.DefaultRemoteConfigService;
import dev.onrcanogul.appbackend.appconfig.internal.web.MinimumVersionFilter;
import dev.onrcanogul.appbackend.appconfig.internal.web.RemoteConfigController;
import dev.onrcanogul.appbackend.core.api.web.ProblemResponseWriter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Everything the appconfig module hands to the outside world. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppConfigProperties.class)
public class AppConfigModuleConfiguration {

    @Bean
    public FeatureFlags featureFlags(FeatureFlagRepository repository) {
        return new DatabaseFeatureFlags(repository);
    }

    @Bean
    public RemoteConfigService remoteConfigService(
            PlatformConfigRepository repository, FeatureFlags featureFlags, AppConfigProperties properties) {
        return new DefaultRemoteConfigService(repository, featureFlags, properties);
    }

    /**
     * Runs after the request context is built (it needs the version header) but before
     * authentication: an unsupported client should be told to update rather than told its
     * token is bad.
     */
    @Bean
    public FilterRegistrationBean<MinimumVersionFilter> minimumVersionFilter(
            RemoteConfigService remoteConfigService,
            ProblemResponseWriter problemWriter,
            AppConfigProperties properties) {
        FilterRegistrationBean<MinimumVersionFilter> registration = new FilterRegistrationBean<>(
                new MinimumVersionFilter(remoteConfigService, problemWriter, properties.versionGateEnabled()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 35);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public RemoteConfigController remoteConfigController(RemoteConfigService remoteConfigService) {
        return new RemoteConfigController(remoteConfigService);
    }
}
