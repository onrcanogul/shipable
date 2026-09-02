package dev.onrcanogul.appbackend.appconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onrcanogul.appbackend.appconfig.api.model.AppVersion;
import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlagAdminService;
import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlags;
import dev.onrcanogul.appbackend.appconfig.api.port.PlatformConfigAdminService;
import dev.onrcanogul.appbackend.appconfig.api.port.RemoteConfigService;
import dev.onrcanogul.appbackend.appconfig.api.port.SettingsAdminService;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.AppSettingRepository;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.FeatureFlagRepository;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.PlatformConfigRepository;
import dev.onrcanogul.appbackend.core.CoreProperties;
import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import dev.onrcanogul.appbackend.core.api.settings.RuntimeSettings;
import dev.onrcanogul.appbackend.core.api.web.ProblemResponseWriter;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * With empty repositories and no scheduled refresh, every snapshot in this module is empty.
 * That is exactly the state worth testing: it is what a fresh deployment looks like before
 * anyone has configured anything, and nothing about it should lock a user out.
 */
class AppConfigModuleContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ProblemResponseWriter.class, () -> new ProblemResponseWriter(new ObjectMapper()))
            .withBean(CoreProperties.class,
                    () -> new CoreProperties(new CoreProperties.RateLimit(120, Duration.ofMinutes(1))))
            .withBean(AppSettingRepository.class, () -> mock(AppSettingRepository.class))
            .withBean(PlatformConfigRepository.class, () -> mock(PlatformConfigRepository.class))
            .withBean(FeatureFlagRepository.class, () -> mock(FeatureFlagRepository.class))
            .withPropertyValues("app.config.default-minimum-version=0.0.0", "app.config.version-gate-enabled=true")
            .withUserConfiguration(AppConfigModuleConfiguration.class);

    @Test
    @DisplayName("module configuration loads and exposes the config and admin ports")
    void contextLoads() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RemoteConfigService.class);
            assertThat(context).hasSingleBean(FeatureFlags.class);
            assertThat(context).hasSingleBean(SettingsAdminService.class);
            assertThat(context).hasSingleBean(FeatureFlagAdminService.class);
            assertThat(context).hasSingleBean(PlatformConfigAdminService.class);
        });
    }

    @Test
    @DisplayName("it supplies the database-backed RuntimeSettings core falls back to a no-op for")
    void providesRuntimeSettings() {
        runner.run(context -> {
            RuntimeSettings settings = context.getBean(RuntimeSettings.class);
            // Nothing stored, so every caller keeps its boot default.
            assertThat(settings.find("core.rate-limit.permits")).isEmpty();
            assertThat(settings.getInt("core.rate-limit.permits", 120)).isEqualTo(120);
        });
    }

    @Test
    @DisplayName("a client that sends no version is treated as supported")
    void unknownVersionIsNotLockedOut() {
        runner.run(context -> {
            RemoteConfigService service = context.getBean(RemoteConfigService.class);
            assertThat(service.isSupported(ClientPlatform.IOS, null)).isTrue();
        });
    }

    @Test
    @DisplayName("with nothing configured, every client is supported")
    void emptyConfigDoesNotLockAnyoneOut() {
        runner.run(context -> {
            RemoteConfigService service = context.getBean(RemoteConfigService.class);
            assertThat(service.isSupported(ClientPlatform.IOS, AppVersion.of("0.0.1"))).isTrue();
            assertThat(service.isSupported(ClientPlatform.ANDROID, AppVersion.of("9.9.9"))).isTrue();
        });
    }

    @Test
    @DisplayName("an unknown feature flag falls back to the caller's default")
    void unknownFlagUsesTheDefault() {
        runner.run(context -> {
            FeatureFlags flags = context.getBean(FeatureFlags.class);
            assertThat(flags.isEnabled("nope")).isFalse();
            assertThat(flags.isEnabled("nope", true)).isTrue();
            assertThat(flags.clientFacingFlags()).isEmpty();
        });
    }

    @Test
    @DisplayName("the platform settings catalog lists the knobs an operator can turn")
    void catalogListsPlatformSettings() {
        runner.run(context -> {
            var keys = context.getBean(SettingsAdminService.class).list().stream()
                    .map(view -> view.key())
                    .toList();

            assertThat(keys).contains(
                    "core.rate-limit.permits",
                    "core.rate-limit.window",
                    "core.rate-limit.enabled",
                    "appconfig.version-gate.enabled",
                    "appconfig.maintenance.enabled");
        });
    }

    @Test
    @DisplayName("the catalog reports this deployment's boot defaults, not hard-coded ones")
    void catalogShowsRealBootDefaults() {
        runner.run(context -> {
            var permits = context.getBean(SettingsAdminService.class).get("core.rate-limit.permits");

            assertThat(permits.bootDefault()).isEqualTo("120");
            assertThat(permits.overridden()).isFalse();
            assertThat(permits.effectiveValue()).isEqualTo("120");
        });
    }
}
