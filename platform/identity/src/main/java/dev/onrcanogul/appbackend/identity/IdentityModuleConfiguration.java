package dev.onrcanogul.appbackend.identity;

import dev.onrcanogul.appbackend.identity.api.port.AccessTokenService;
import dev.onrcanogul.appbackend.identity.api.port.AuthenticationService;
import dev.onrcanogul.appbackend.identity.api.port.IdentityTokenVerifier;
import dev.onrcanogul.appbackend.identity.internal.client.AppleIdentityTokenVerifier;
import dev.onrcanogul.appbackend.identity.internal.client.GoogleIdentityTokenVerifier;
import dev.onrcanogul.appbackend.identity.internal.service.DefaultAuthenticationService;
import dev.onrcanogul.appbackend.identity.internal.service.JwtAccessTokenService;
import dev.onrcanogul.appbackend.identity.internal.web.AuthController;
import dev.onrcanogul.appbackend.identity.internal.web.JwtAuthenticationFilter;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Everything the identity module hands to the outside world.
 *
 * <p>Other modules should inject {@code AuthenticationService} or read
 * {@code CurrentUserHolder}; nothing else here is meant for them.
 *
 * <p>Adding a sign-in provider is two lines: an implementation of
 * {@link IdentityTokenVerifier} and a {@code @Bean} method below.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityModuleConfiguration {

    @Bean
    public IdentityTokenVerifier appleIdentityTokenVerifier() {
        return new AppleIdentityTokenVerifier();
    }

    @Bean
    public IdentityTokenVerifier googleIdentityTokenVerifier() {
        return new GoogleIdentityTokenVerifier();
    }

    @Bean
    public AccessTokenService accessTokenService(IdentityProperties properties, Clock clock) {
        return new JwtAccessTokenService(properties, clock);
    }

    @Bean
    public AuthenticationService authenticationService(
            List<IdentityTokenVerifier> verifiers, AccessTokenService accessTokenService) {
        return new DefaultAuthenticationService(verifiers, accessTokenService);
    }

    /**
     * Runs after core's pipeline filters (order 10-30) so a flood is rate-limited before
     * we spend anything verifying its tokens.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilter(
            AccessTokenService accessTokenService) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(new JwtAuthenticationFilter(accessTokenService));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 40);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public AuthController authController(AuthenticationService authenticationService) {
        return new AuthController(authenticationService);
    }
}
