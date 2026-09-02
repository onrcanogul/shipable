package dev.onrcanogul.appbackend.core.api.context;

import java.util.Optional;

/**
 * Carries the {@link RequestContext} for the duration of a request.
 *
 * <p>Only {@code RequestContextBindingFilter} writes here; everyone else reads.
 *
 * <p>TODO: register a {@code TaskDecorator} so the context survives being handed to an
 * {@code @Async} executor.
 */
public final class RequestContextHolder {

    private static final ThreadLocal<RequestContext> CURRENT = new ThreadLocal<>();

    private RequestContextHolder() {
    }

    public static Optional<RequestContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static String requestId() {
        return current().map(RequestContext::requestId).orElse("no-request");
    }

    /** Called by RequestContextBindingFilter only. */
    public static void set(RequestContext context) {
        CURRENT.set(context);
    }

    /** Called by RequestContextBindingFilter only, from a finally block. */
    public static void clear() {
        CURRENT.remove();
    }
}
