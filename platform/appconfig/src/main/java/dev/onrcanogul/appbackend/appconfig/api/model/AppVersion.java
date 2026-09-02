package dev.onrcanogul.appbackend.appconfig.api.model;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A semantic-ish client version: {@code MAJOR.MINOR.PATCH}, patch optional.
 *
 * <p>Comparing versions as strings is the classic bug here — "1.10.0" sorts before "1.9.0"
 * and your force-update gate locks out the newest release. Hence a real type with a real
 * comparator.
 *
 * <p>Anything after the patch number (a build suffix, a {@code -beta}) is ignored rather
 * than rejected: a version gate should not be the thing that breaks a TestFlight build.
 */
public record AppVersion(int major, int minor, int patch) implements Comparable<AppVersion> {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*$");

    private static final Comparator<AppVersion> ORDER = Comparator
            .comparingInt(AppVersion::major)
            .thenComparingInt(AppVersion::minor)
            .thenComparingInt(AppVersion::patch);

    /** @return the parsed version, or null when the string is not a version at all */
    public static AppVersion parseOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher matcher = PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            return null;
        }
        return new AppVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3)));
    }

    public static AppVersion of(String raw) {
        AppVersion parsed = parseOrNull(raw);
        if (parsed == null) {
            throw new IllegalArgumentException("Not a version: " + raw);
        }
        return parsed;
    }

    public boolean isOlderThan(AppVersion other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(AppVersion other) {
        return ORDER.compare(this, other);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
