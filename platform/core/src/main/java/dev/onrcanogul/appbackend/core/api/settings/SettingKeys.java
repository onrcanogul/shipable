package dev.onrcanogul.appbackend.core.api.settings;

/**
 * The setting keys the platform itself understands.
 *
 * <p>Namespaced by module so the admin list reads as a tree and your app's own keys cannot
 * collide with a platform key added in a later version. Use your own prefix (for example
 * {@code app.*}) for anything you add.
 *
 * <p>These are a contract: an operator will have typed them into the admin API, and a
 * shipped client may depend on their effect. Renaming one silently reverts that setting to
 * its default.
 */
public final class SettingKeys {

    /** Requests allowed per window, per IP, before authentication. Integer. */
    public static final String RATE_LIMIT_PERMITS = "core.rate-limit.permits";

    /** The window those permits are counted over. Duration, e.g. {@code 1m}. */
    public static final String RATE_LIMIT_WINDOW = "core.rate-limit.window";

    /** Turns the per-IP rate limiter off entirely. Boolean. */
    public static final String RATE_LIMIT_ENABLED = "core.rate-limit.enabled";

    /** Rejects clients below the configured minimum version. Boolean. */
    public static final String VERSION_GATE_ENABLED = "appconfig.version-gate.enabled";

    /** Serves a maintenance response to every client. Boolean. */
    public static final String MAINTENANCE_MODE = "appconfig.maintenance.enabled";

    /** What the client shows during maintenance. String. */
    public static final String MAINTENANCE_MESSAGE = "appconfig.maintenance.message";

    /** How long a deletion request waits before erasure runs. Duration. */
    public static final String DELETION_GRACE_PERIOD = "privacy.deletion.grace-period";

    /**
     * Makes every cache read a miss and every write a no-op, without dropping the Redis
     * connection. Boolean.
     *
     * <p>Distinct from {@code app.cache.enabled}, which decides at startup whether the Redis
     * beans exist at all and therefore cannot change while running. This is the runtime
     * switch: for ruling the cache out as the cause of something, or shedding load from a
     * struggling Redis without a redeploy.
     */
    public static final String CACHE_BYPASS = "cache.bypass";

    /** TTL used by cache writes that do not name one. Duration. */
    public static final String CACHE_DEFAULT_TTL = "cache.default-ttl";

    private SettingKeys() {
    }
}
