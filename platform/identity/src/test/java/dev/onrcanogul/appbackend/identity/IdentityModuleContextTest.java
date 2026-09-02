package dev.onrcanogul.appbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import dev.onrcanogul.appbackend.identity.api.model.AuthProvider;
import dev.onrcanogul.appbackend.identity.api.port.AccessTokenService;
import dev.onrcanogul.appbackend.identity.api.port.AuthenticationService;
import dev.onrcanogul.appbackend.identity.api.port.IdentityTokenVerifier;
import static org.mockito.Mockito.mock;

import dev.onrcanogul.appbackend.identity.internal.persistence.repository.RefreshTokenRepository;
import dev.onrcanogul.appbackend.identity.internal.persistence.repository.UserRepository;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class IdentityModuleContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(UserRepository.class, () -> mock(UserRepository.class))
            .withBean(RefreshTokenRepository.class, () -> mock(RefreshTokenRepository.class))
            .withPropertyValues(
                    "app.identity.jwt.secret=test-secret-that-is-definitely-long-enough-32",
                    "app.identity.jwt.issuer=app-backend-template",
                    "app.identity.jwt.access-token-ttl=15m",
                    "app.identity.jwt.refresh-token-ttl=60d")
            .withUserConfiguration(IdentityModuleConfiguration.class);

    @Test
    @DisplayName("module configuration loads and exposes the auth ports")
    void contextLoads() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AuthenticationService.class);
            assertThat(context).hasSingleBean(AccessTokenService.class);
        });
    }

    @Test
    @DisplayName("one token verifier is registered per external provider")
    void everyExternalProviderHasAVerifier() {
        runner.run(context -> {
            List<AuthProvider> covered = context.getBeanProvider(IdentityTokenVerifier.class)
                    .stream()
                    .map(IdentityTokenVerifier::provider)
                    .toList();
            assertThat(covered).containsExactlyInAnyOrder(AuthProvider.APPLE, AuthProvider.GOOGLE);
        });
    }

    @Test
    @DisplayName("a signing secret that is too short stops the application from starting")
    void refusesAWeakSigningSecret() {
        runner.withPropertyValues("app.identity.jwt.secret=too-short")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasStackTraceContaining("at least 32 bytes"));
    }
}
