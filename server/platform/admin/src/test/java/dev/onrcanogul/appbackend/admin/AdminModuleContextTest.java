package dev.onrcanogul.appbackend.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onrcanogul.appbackend.admin.internal.web.AdminSettingsController;
import dev.onrcanogul.appbackend.appconfig.api.port.FeatureFlagAdminService;
import dev.onrcanogul.appbackend.appconfig.api.port.PlatformConfigAdminService;
import dev.onrcanogul.appbackend.appconfig.api.port.SettingsAdminService;
import dev.onrcanogul.appbackend.core.api.web.ProblemResponseWriter;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The two properties this module must have: it is absent unless enabled, and it refuses to
 * start unprotected.
 */
class AdminModuleContextTest {

    private static final String STRONG_KEY = "a-long-enough-admin-key-for-tests-1234";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(ProblemResponseWriter.class, () -> new ProblemResponseWriter(new ObjectMapper()))
            .withBean(SettingsAdminService.class, () -> mock(SettingsAdminService.class))
            .withBean(FeatureFlagAdminService.class, () -> mock(FeatureFlagAdminService.class))
            .withBean(PlatformConfigAdminService.class, () -> mock(PlatformConfigAdminService.class))
            .withUserConfiguration(AdminModuleConfiguration.class);

    @Test
    @DisplayName("the admin API does not exist unless it is switched on")
    void disabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AdminSettingsController.class);
        });
    }

    @Test
    @DisplayName("enabled with a strong key, the controllers are registered")
    void enabledWithAStrongKey() {
        runner.withPropertyValues("app.admin.enabled=true", "app.admin.api-key=" + STRONG_KEY)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AdminSettingsController.class);
                });
    }

    @Test
    @DisplayName("enabled with no key, the application refuses to start")
    void refusesToStartWithoutAKey() {
        runner.withPropertyValues("app.admin.enabled=true")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasStackTraceContaining("app.admin.api-key"));
    }

    @Test
    @DisplayName("enabled with a short key, the application refuses to start")
    void refusesToStartWithAWeakKey() {
        runner.withPropertyValues("app.admin.enabled=true", "app.admin.api-key=short")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasStackTraceContaining("32 characters"));
    }

    @Test
    @DisplayName("the guard filter only covers the admin path")
    void filterCoversOnlyAdminPaths() {
        runner.withPropertyValues("app.admin.enabled=true", "app.admin.api-key=" + STRONG_KEY)
                .run(context -> {
                    var registration = context.getBean(
                            "adminAuthenticationFilter",
                            org.springframework.boot.web.servlet.FilterRegistrationBean.class);
                    assertThat(registration.getUrlPatterns()).containsExactly("/api/admin/*");
                });
    }
}
