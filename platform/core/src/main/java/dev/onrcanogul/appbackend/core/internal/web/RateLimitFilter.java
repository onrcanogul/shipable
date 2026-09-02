package dev.onrcanogul.appbackend.core.internal.web;

import dev.onrcanogul.appbackend.core.api.context.RequestContextHolder;
import dev.onrcanogul.appbackend.core.api.error.AppError;
import dev.onrcanogul.appbackend.core.api.error.ErrorCodes;
import dev.onrcanogul.appbackend.core.api.port.RateLimitDecision;
import dev.onrcanogul.appbackend.core.api.port.RateLimitPolicy;
import dev.onrcanogul.appbackend.core.api.port.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import dev.onrcanogul.appbackend.core.api.web.ProblemResponseWriter;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Coarse per-caller rate limiting, applied before anything expensive runs.
 *
 * <p>Keyed by client IP because this runs before authentication — at this point in the
 * chain there is no user yet. Per-user limits belong in {@code quota}, which knows what
 * the user paid for; this filter exists to stop a stranger hammering your sign-in
 * endpoint.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final List<String> BYPASS_PREFIXES =
            List.of("/actuator", "/v3/api-docs", "/swagger-ui");

    private final RateLimiter rateLimiter;
    private final RateLimitPolicy policy;
    private final ProblemResponseWriter problemWriter;

    public RateLimitFilter(RateLimiter rateLimiter, RateLimitPolicy policy,
            ProblemResponseWriter problemWriter) {
        this.rateLimiter = rateLimiter;
        this.policy = policy;
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

        String key = RequestContextHolder.current()
                .map(context -> context.clientIp())
                .orElseGet(request::getRemoteAddr);

        RateLimitDecision decision = rateLimiter.tryAcquire(key, policy);
        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(Math.max(1, decision.retryAfter().toSeconds())));
            problemWriter.write(response, HttpStatus.TOO_MANY_REQUESTS,
                    AppError.of(ErrorCodes.RATE_LIMITED, "Too many requests, slow down"));
            return;
        }

        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        chain.doFilter(request, response);
    }
}
