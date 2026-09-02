package dev.onrcanogul.appbackend.admin;

import dev.onrcanogul.appbackend.admin.internal.security.AdminAuthenticationFilter;
import dev.onrcanogul.appbackend.admin.internal.web.AdminFeatureFlagController;
import dev.onrcanogul.appbackend.admin.internal.web.AdminInfoController;
import dev.onrcanogul.appbackend.admin.internal.web.AdminPlatformConfigController;
import dev.onrcanogul.appbackend.admin.internal.web.AdminSettingsController;
import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlagAdminService;
import dev.onrcanogul.appbackend.appconfig.api.port.PlatformConfigAdminService;
import dev.onrcanogul.appbackend.appconfig.api.port.SettingsAdminService;
import dev.onrcanogul.appbackend.core.api.web.ProblemResponseWriter;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * Everything the admin module hands to the outside world.
 *
 * <p><b>Off unless you turn it on.</b> {@code app.admin.enabled} defaults to false, so a
 * clone that never needs an operator API never exposes one. An endpoint that does not exist
 * cannot be attacked.
 *
 * <p>When it is on, a startup check refuses a weak key. Booting with
 * {@code app.admin.enabled=true} and a blank key would leave the most dangerous surface in
 * the application wide open, and it would look fine in the logs.
 *
 * <p>The whole module can be removed: drop it from {@code @Import} in the host and delete
 * the dependency. Nothing else refers to it.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminProperties.class)
@ConditionalOnProperty(prefix = "app.admin", name = "enabled", havingValue = "true")
public class AdminModuleConfiguration {

    private static final int MINIMUM_KEY_LENGTH = 32;

    private final AdminProperties properties;

    public AdminModuleConfiguration(AdminProperties properties) {
        this.properties = properties;
    }

    /**
     * Fails startup rather than logging a warning. A warning about an unprotected admin API
     * scrolls past; a refusal to start does not.
     */
    @PostConstruct
    void verifyTheKeyIsUsable() {
        String key = properties.apiKey();
        if (key == null || key.trim().length() < MINIMUM_KEY_LENGTH) {
            throw new IllegalStateException(
                    "app.admin.enabled is true but app.admin.api-key is missing or shorter than "
                            + MINIMUM_KEY_LENGTH + " characters. This key is the only thing protecting "
                            + "an API that can change rate limits and force every client to update. "
                            + "Generate one with: openssl rand -base64 32");
        }
    }

    /**
     * Runs before authentication and before the version gate. An operator must be able to
     * reach the admin API even when the app is in maintenance or every client is being
     * turned away — those are exactly the states you need it in.
     */
    @Bean
    public FilterRegistrationBean<AdminAuthenticationFilter> adminAuthenticationFilter(
            ProblemResponseWriter problemWriter) {
        FilterRegistrationBean<AdminAuthenticationFilter> registration =
                new FilterRegistrationBean<>(new AdminAuthenticationFilter(properties, problemWriter));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 15);
        registration.addUrlPatterns(AdminAuthenticationFilter.ADMIN_PATH_PREFIX + "*");
        return registration;
    }

    @Bean
    public AdminSettingsController adminSettingsController(SettingsAdminService settings) {
        return new AdminSettingsController(settings);
    }

    @Bean
    public AdminFeatureFlagController adminFeatureFlagController(FeatureFlagAdminService flags) {
        return new AdminFeatureFlagController(flags);
    }

    @Bean
    public AdminPlatformConfigController adminPlatformConfigController(PlatformConfigAdminService platformConfig) {
        return new AdminPlatformConfigController(platformConfig);
    }

    @Bean
    public AdminInfoController adminInfoController(Environment environment, Clock clock) {
        return new AdminInfoController(environment, clock);
    }
}
