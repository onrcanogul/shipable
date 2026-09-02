package dev.onrcanogul.appbackend.admin.internal.security;

import dev.onrcanogul.appbackend.admin.AdminProperties;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Guards everything under {@code /api/admin/**}.
 *
 * <p>Two independent checks, because this API can change a rate limit, force every client
 * to update, or put the app into maintenance:
 *
 * <p><b>The API key</b>, compared in constant time. String equality returns as soon as two
 * bytes differ, which leaks the key one character at a time to anyone willing to measure —
 * a real attack on an endpoint someone can call as often as they like.
 *
 * <p><b>The IP allowlist</b>, when configured. A leaked key is then not enough on its own.
 *
 * <p>Every rejection is logged with the caller's address. Nobody should be probing this
 * endpoint, so a rejection here is worth noticing.
 */
public class AdminAuthenticationFilter extends OncePerRequestFilter {

    public static final String ADMIN_KEY_HEADER = "X-Admin-Key";
    public static final String ADMIN_PATH_PREFIX = "/api/admin/";

    private static final Logger log = LoggerFactory.getLogger(AdminAuthenticationFilter.class);

    private final byte[] expectedKey;
    private final AdminProperties properties;
    private final ProblemResponseWriter problemWriter;

    public AdminAuthenticationFilter(AdminProperties properties, ProblemResponseWriter problemWriter) {
        this.properties = properties;
        this.problemWriter = problemWriter;
        this.expectedKey = properties.apiKey() == null
                ? new byte[0]
                : properties.apiKey().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ADMIN_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String callerIp = callerIp(request);

        if (!hasValidKey(request)) {
            log.warn("Rejected an admin request from {} with a missing or wrong key: {} {}",
                    callerIp, request.getMethod(), request.getRequestURI());
            // 401 with nothing specific. Telling a caller whether the key was absent or
            // merely wrong is free information for whoever is probing.
            problemWriter.write(response, HttpStatus.UNAUTHORIZED,
                    AppError.of(ErrorCodes.UNAUTHORIZED, "Admin authentication required"));
            return;
        }

        if (!IpAllowList.permits(properties.allowedIps(), callerIp)) {
            log.warn("Rejected an admin request from {}: address is not on the allowlist", callerIp);
            problemWriter.write(response, HttpStatus.FORBIDDEN,
                    AppError.of(ErrorCodes.FORBIDDEN, "This address is not allowed to use the admin API"));
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean hasValidKey(HttpServletRequest request) {
        String provided = request.getHeader(ADMIN_KEY_HEADER);
        if (provided == null || provided.isBlank() || expectedKey.length == 0) {
            return false;
        }
        return MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expectedKey);
    }

    private static String callerIp(HttpServletRequest request) {
        return RequestContextHolder.current()
                .map(RequestContext::clientIp)
                .orElseGet(request::getRemoteAddr);
    }
}
