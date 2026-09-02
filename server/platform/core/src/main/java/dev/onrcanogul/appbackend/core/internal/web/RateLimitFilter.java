package dev.onrcanogul.appbackend.core.internal.web;

import dev.onrcanogul.appbackend.core.api.context.RequestContextHolder;
import dev.onrcanogul.appbackend.core.api.error.AppError;
import dev.onrcanogul.appbackend.core.api.error.ErrorCodes;
import dev.onrcanogul.appbackend.core.api.port.RateLimitDecision;
import dev.onrcanogul.appbackend.core.api.port.RateLimitPolicy;
import dev.onrcanogul.appbackend.core.api.port.RateLimiter;
import dev.onrcanogul.appbackend.core.api.settings.RuntimeSettings;
import dev.onrcanogul.appbackend.core.api.settings.SettingKeys;
import dev.onrcanogul.appbackend.core.api.web.ProblemResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Coarse per-caller rate limiting, applied before anything expensive runs.
 *
 * <p>Keyed by client IP because this runs before authentication — at this point in the
 * chain there is no user yet. Per-user limits belong in {@code quota}, which knows what the
 * user paid for; this filter exists to stop a stranger hammering your sign-in endpoint.
 *
 * <p>The limit is read from {@link RuntimeSettings} on every request rather than fixed at
 * startup, so it can be changed from the admin API while under attack — which is the only
 * time anyone ever wants to change it, and the worst time to need a redeploy. The values in
 * {@code application.yml} remain the defaults when no override is stored.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final List<String> BYPASS_PREFIXES =
            List.of("/actuator", "/v3/api-docs", "/swagger-ui");

    private final RateLimiter rateLimiter;
    private final RateLimitPolicy defaultPolicy;
    private final RuntimeSettings settings;
    private final ProblemResponseWriter problemWriter;

    public RateLimitFilter(
            RateLimiter rateLimiter,
            RateLimitPolicy defaultPolicy,
            RuntimeSettings settings,
            ProblemResponseWriter problemWriter) {
        this.rateLimiter = rateLimiter;
        this.defaultPolicy = defaultPolicy;
        this.settings = settings;
        this.problemWriter = problemWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return BYPASS_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!settings.getBoolean(SettingKeys.RATE_LIMIT_ENABLED, true)) {
            chain.doFilter(request, response);
            return;
        }

        String key = RequestContextHolder.current()
                .map(context -> context.clientIp())
                .orElseGet(request::getRemoteAddr);

        RateLimitDecision decision = rateLimiter.tryAcquire(key, currentPolicy());
        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(Math.max(1, decision.retryAfter().toSeconds())));
            problemWriter.write(response, HttpStatus.TOO_MANY_REQUESTS,
                    AppError.of(ErrorCodes.RATE_LIMITED, "Too many requests, slow down"));
            return;
        }

        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        chain.doFilter(request, response);
    }

    /**
     * An invalid stored value falls back to the boot default rather than throwing. A typo
     * in the admin API must not take the whole API down — and the one thing worse than a
     * wrong rate limit is no requests getting through at all.
     */
    private RateLimitPolicy currentPolicy() {
        int permits = settings.getInt(SettingKeys.RATE_LIMIT_PERMITS, defaultPolicy.permits());
        var window = settings.getDuration(SettingKeys.RATE_LIMIT_WINDOW, defaultPolicy.window());
        try {
            return new RateLimitPolicy(permits, window);
        } catch (IllegalArgumentException e) {
            return defaultPolicy;
        }
    }
}
