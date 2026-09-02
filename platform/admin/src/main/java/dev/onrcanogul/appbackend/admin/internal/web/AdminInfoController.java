package dev.onrcanogul.appbackend.admin.internal.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What is actually running here.
 *
 * <p>The first thing you want when something is wrong and you are not sure which deployment
 * you are looking at: which profile, which version, how long it has been up.
 *
 * <p>Reports the active profiles and nothing else from the environment. It would be easy to
 * dump every property here and convenient about half the time — and the other half you have
 * just published your database password to whoever holds the admin key.
 */
@RestController
@RequestMapping("/api/admin/v1/info")
@Tag(name = "Admin: info", description = "Which deployment am I looking at")
public class AdminInfoController {

    private final Environment environment;
    private final Clock clock;
    private final Instant startedAt;

    public AdminInfoController(Environment environment, Clock clock) {
        this.environment = environment;
        this.clock = clock;
        this.startedAt = clock.instant();
    }

    @GetMapping
    @Operation(summary = "Active profiles, version and uptime")
    public Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("application", environment.getProperty("spring.application.name", "app-backend"));
        info.put("activeProfiles", List.of(environment.getActiveProfiles()));
        info.put("version", environment.getProperty("app.build.version", "unknown"));
        info.put("startedAt", startedAt.toString());
        info.put("uptimeSeconds", java.time.Duration.between(startedAt, clock.instant()).toSeconds());
        info.put("serverTime", clock.instant().toString());
        return info;
    }
}
