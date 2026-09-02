package dev.onrcanogul.appbackend.core.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onrcanogul.appbackend.core.api.context.RequestContextHolder;
import dev.onrcanogul.appbackend.core.api.error.AppError;
import dev.onrcanogul.appbackend.core.api.error.ProblemBody;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Writes an error response from inside a servlet filter.
 *
 * <p>Filters run before Spring MVC, so {@code @RestControllerAdvice} cannot help them: a
 * filter that rejects a request has to serialise the body itself. This exists so all of
 * them produce the same shape as the exception handler, instead of each inventing one.
 */
public final class ProblemResponseWriter {

    private final ObjectMapper objectMapper;

    public ProblemResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, AppError error) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ProblemBody.of(status.value(), error, RequestContextHolder.requestId()));
    }
}
