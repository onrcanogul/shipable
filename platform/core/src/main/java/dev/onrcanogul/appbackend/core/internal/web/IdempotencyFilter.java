package dev.onrcanogul.appbackend.core.internal.web;

import dev.onrcanogul.appbackend.core.api.error.AppError;
import dev.onrcanogul.appbackend.core.api.error.ErrorCodes;
import dev.onrcanogul.appbackend.core.api.port.IdempotencyStore;
import dev.onrcanogul.appbackend.core.api.web.ProblemResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Honours the {@code Idempotency-Key} header on state-changing requests.
 *
 * <p>Mobile clients retry: a request that timed out on a train may well have succeeded. A
 * client that sends the same key twice gets 409 for the duplicate instead of creating the
 * thing twice.
 *
 * <p>The key is optional. Requests without one behave exactly as before, so this can be
 * adopted endpoint by endpoint from the client side.
 *
 * <p><b>Known limitation:</b> a duplicate is rejected, not replayed. Full idempotency
 * would return the original response, which means storing it. That is the right thing for
 * payments; for creating a row it is usually overkill.
 *
 * <p>TODO: store the first response and replay it, for endpoints that need it.
 */
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private static final Set<String> GUARDED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Duration KEY_TTL = Duration.ofHours(24);

    private final IdempotencyStore store;
    private final ProblemResponseWriter problemWriter;

    public IdempotencyFilter(IdempotencyStore store, ProblemResponseWriter problemWriter) {
        this.store = store;
        this.problemWriter = problemWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.isBlank() || !GUARDED_METHODS.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Scope by method and path: the same key on two different endpoints is two
        // different operations, and treating them as one would reject a legitimate call.
        String scopedKey = request.getMethod() + " " + request.getRequestURI() + " " + key.trim();

        if (!store.claim(scopedKey, KEY_TTL)) {
            writeDuplicate(response);
            return;
        }

        boolean succeeded = false;
        try {
            chain.doFilter(request, response);
            succeeded = response.getStatus() < 500;
        } finally {
            // A server-side failure leaves nothing behind worth protecting, so let the
            // client retry with the same key instead of locking it out for 24 hours.
            if (!succeeded) {
                store.release(scopedKey);
            }
        }
    }

    private void writeDuplicate(HttpServletResponse response) throws IOException {
        problemWriter.write(response, HttpStatus.CONFLICT,
                AppError.of(ErrorCodes.DUPLICATE_REQUEST,
                        "This " + IDEMPOTENCY_KEY_HEADER + " was already used"));
    }
}
