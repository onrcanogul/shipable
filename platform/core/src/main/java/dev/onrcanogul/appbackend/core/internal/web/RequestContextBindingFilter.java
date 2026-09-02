package dev.onrcanogul.appbackend.core.internal.web;

import dev.onrcanogul.appbackend.core.api.context.ClientPlatform;
import dev.onrcanogul.appbackend.core.api.context.RequestContext;
import dev.onrcanogul.appbackend.core.api.context.RequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Builds the {@link RequestContext} and binds it for the rest of the chain.
 *
 * <p>Also pushes the request id into the logging MDC, so every log line from this request
 * carries it. That single detail is the difference between "a user reports a bug" and
 * "here is exactly what happened".
 *
 * <p>Named {@code ...BindingFilter} rather than {@code RequestContextFilter} because Spring
 * already has a class and an auto-configured bean by that name. Sharing it makes the
 * application fail to start with a bean definition conflict, and sharing the class name
 * makes every import a coin toss.
 */
public class RequestContextBindingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String PLATFORM_HEADER = "X-Client-Platform";
    public static final String APP_VERSION_HEADER = "X-App-Version";

    private static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestId = header(request, REQUEST_ID_HEADER, UUID.randomUUID().toString());
        RequestContext context = new RequestContext(
                requestId,
                ClientPlatform.parse(request.getHeader(PLATFORM_HEADER)),
                header(request, APP_VERSION_HEADER, null),
                clientIp(request));

        try {
            RequestContextHolder.set(context);
            MDC.put(MDC_KEY, requestId);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            chain.doFilter(request, response);
        } finally {
            // Pooled threads outlive requests; leaving either of these set leaks one
            // request's identity into the next one.
            MDC.remove(MDC_KEY);
            RequestContextHolder.clear();
        }
    }

    private static String header(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * Best effort caller address.
     *
     * <p>Only trustworthy behind a proxy you control — the template runs behind Caddy,
     * which sets {@code X-Forwarded-For}. Exposed directly to the internet, a client can
     * forge this header, so do not use it for anything but coarse rate limiting.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
