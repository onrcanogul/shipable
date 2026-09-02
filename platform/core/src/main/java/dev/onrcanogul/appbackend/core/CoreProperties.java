package dev.onrcanogul.appbackend.core;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings for the request pipeline, bound from {@code app.core.*}.
 *
 * <p>Validated at startup: a typo in a limit should stop the application, not surface as
 * odd behaviour under load a week later.
 */
@Validated
@ConfigurationProperties(prefix = "app.core")
public record CoreProperties(@NotNull RateLimit rateLimit) {

    public CoreProperties {
        if (rateLimit == null) {
            rateLimit = new RateLimit(120, Duration.ofMinutes(1));
        }
    }

    /**
     * The coarse per-IP limit applied to every request before authentication.
     *
     * @param permits requests allowed per window
     * @param window  the window itself
     */
    public record RateLimit(@Min(1) int permits, @NotNull Duration window) {
    }
}
