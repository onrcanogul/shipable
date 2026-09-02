package dev.onrcanogul.appbackend.appconfig.internal.web;

import dev.onrcanogul.appbackend.appconfig.api.model.AppVersion;
import dev.onrcanogul.appbackend.appconfig.api.port.RemoteConfigService;
import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import dev.onrcanogul.appbackend.core.api.context.RequestContext;
import dev.onrcanogul.appbackend.core.api.context.RequestContextHolder;
import dev.onrcanogul.appbackend.core.api.error.AppError;
import dev.onrcanogul.appbackend.core.api.error.ErrorCodes;
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
 * Turns away clients below the minimum supported version.
 *
 * <p>Belt and braces: a well-behaved client checks {@code GET /api/v1/config} at launch and
 * shows its own update screen. This filter is for the ones that do not — an old build that
 * never learned to ask, calling an endpoint whose contract has since changed.
 *
 * <p>Answers <b>426 Upgrade Required</b> rather than 400, so the client can tell "you must
 * update" apart from "your request was wrong" without parsing a message.
 *
 * <p>Endpoints that must stay reachable are exempt: the config endpoint itself (or the
 * client can never learn what to do), and the billing webhook (RevenueCat is not a client
 * and sends no version).
 */
public class MinimumVersionFilter extends OncePerRequestFilter {

    private static final List<String> BYPASS_PREFIXES = List.of(
            "/api/v1/config",
            "/api/v1/billing/webhooks",
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui");

    private final RemoteConfigService remoteConfigService;
    private final ProblemResponseWriter problemWriter;

    public MinimumVersionFilter(
            RemoteConfigService remoteConfigService, ProblemResponseWriter problemWriter) {
        this.remoteConfigService = remoteConfigService;
        this.problemWriter = problemWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Whether the gate is on at all is decided inside isSupported, from RuntimeSettings,
        // so it can be switched off without a redeploy.
        String path = request.getRequestURI();
        return BYPASS_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        RequestContext context = RequestContextHolder.current().orElse(null);
        ClientPlatform platform = context == null ? ClientPlatform.UNKNOWN : context.platform();
        AppVersion version = context == null ? null : AppVersion.parseOrNull(context.appVersion());

        if (!remoteConfigService.isSupported(platform, version)) {
            problemWriter.write(response, HttpStatus.UPGRADE_REQUIRED,
                    AppError.of(ErrorCodes.APP_VERSION_UNSUPPORTED,
                            "This version of the app is no longer supported. Please update."));
            return;
        }

        chain.doFilter(request, response);
    }
}
