package dev.onrcanogul.appbackend.appconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onrcanogul.appbackend.appconfig.api.model.AppVersion;
import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlags;
import dev.onrcanogul.appbackend.appconfig.api.port.RemoteConfigService;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.FeatureFlagRepository;
import dev.onrcanogul.appbackend.appconfig.internal.persistence.repository.PlatformConfigRepository;
import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import dev.onrcanogul.appbackend.core.api.web.ProblemResponseWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AppConfigModuleContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ProblemResponseWriter.class, () -> new ProblemResponseWriter(new ObjectMapper()))
            .withBean(PlatformConfigRepository.class, () -> mock(PlatformConfigRepository.class))
            .withBean(FeatureFlagRepository.class, () -> mock(FeatureFlagRepository.class))
            .withPropertyValues("app.config.default-minimum-version=0.0.0", "app.config.version-gate-enabled=true")
            .withUserConfiguration(AppConfigModuleConfiguration.class);

    @Test
    @DisplayName("module configuration loads and exposes the config ports")
    void contextLoads() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RemoteConfigService.class);
            assertThat(context).hasSingleBean(FeatureFlags.class);
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
    @DisplayName("with no configured minimum, every client is supported")
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
        });
    }
}
