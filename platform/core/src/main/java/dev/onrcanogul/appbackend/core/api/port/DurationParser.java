package dev.onrcanogul.appbackend.core.api.port;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the short duration forms people actually type: {@code 30s}, {@code 5m},
 * {@code 2h}, {@code 7d}.
 *
 * <p>Also accepts ISO-8601 ({@code PT5M}), so a value copied out of {@code application.yml}
 * works unchanged. Spring understands both in configuration; settings stored at runtime go
 * through this so they behave the same.
 */
public final class DurationParser {

    private static final Pattern SHORT_FORM = Pattern.compile("^(\\d+)\\s*(ms|s|m|h|d)$");

    private DurationParser() {
    }

    public static Duration parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Duration must not be blank");
        }
        String value = raw.trim();

        Matcher matcher = SHORT_FORM.matcher(value.toLowerCase(java.util.Locale.ROOT));
        if (matcher.matches()) {
            long amount = Long.parseLong(matcher.group(1));
            return switch (matcher.group(2)) {
                case "ms" -> Duration.ofMillis(amount);
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                default -> throw new IllegalArgumentException("Unsupported unit in: " + raw);
            };
        }
        return Duration.parse(value);
    }
}
