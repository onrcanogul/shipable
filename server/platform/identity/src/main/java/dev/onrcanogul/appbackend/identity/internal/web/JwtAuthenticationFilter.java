package dev.onrcanogul.appbackend.identity.internal.web;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.identity.api.context.CurrentUser;
import dev.onrcanogul.appbackend.identity.api.context.CurrentUserHolder;
import dev.onrcanogul.appbackend.identity.api.port.AccessTokenService;
import dev.onrcanogul.appbackend.identity.api.port.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the {@code Authorization: Bearer} header and binds the caller for this request.
 *
 * <p><b>Permissive by design.</b> A request with no token, or a bad one, is not rejected
 * here — it simply arrives with no current user, and the endpoint decides. That keeps the
 * public/private split in the endpoints, where you can see it, rather than in a path
 * pattern list that drifts out of sync with the controllers.
 *
 * <p>Endpoints call {@code CurrentUserHolder.require()} (or {@code requireRegistered()})
 * and get a 401/403 from the exception handler if there is no session. A new endpoint
 * therefore defaults to being unreachable-as-a-user rather than accidentally public.
 *
 * <p>TODO: swap in Spring Security if you outgrow this — roles, method security, OAuth2
 * resource server. For a single mobile backend, this is less machinery to reason about.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenService accessTokenService;
    private final AuthenticationService authenticationService;

    public JwtAuthenticationFilter(
            AccessTokenService accessTokenService, AuthenticationService authenticationService) {
        this.accessTokenService = accessTokenService;
        this.authenticationService = authenticationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            extractToken(request)
                    .flatMap(token -> accessTokenService.validate(token).asOptional())
                    .ifPresent(this::bind);
            chain.doFilter(request, response);
        } finally {
            // Pooled threads: leaving this set would hand the next request the previous
            // caller's identity.
            CurrentUserHolder.clear();
        }
    }

    /**
     * Whether the caller is anonymous decides what they may do, so it cannot be assumed.
     *
     * <p>TODO: put the flag in the token as a claim. It only changes when an anonymous
     * account is linked, and issuing a fresh token then is cheaper than a lookup on every
     * request.
     */
    private void bind(UserId userId) {
        boolean anonymous = authenticationService.findById(userId)
                .map(user -> user.anonymous())
                .orElse(true);
        CurrentUserHolder.set(new CurrentUser(userId, anonymous));
    }

    private static java.util.Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return java.util.Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(token);
    }
}
