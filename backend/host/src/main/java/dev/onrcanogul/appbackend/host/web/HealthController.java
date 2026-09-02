package dev.onrcanogul.appbackend.host.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A liveness endpoint the mobile client can call.
 *
 * <p>Separate from {@code /actuator/health}: that one is for your monitoring and can report
 * details you would not want public, so it lives outside {@code /api} and is exempt from
 * the request pipeline. This one is a plain public "yes, I am here".
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Liveness check")
public class HealthController {

    private final Clock clock;

    public HealthController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Is the API up")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "time", Instant.now(clock).toString());
    }
}
