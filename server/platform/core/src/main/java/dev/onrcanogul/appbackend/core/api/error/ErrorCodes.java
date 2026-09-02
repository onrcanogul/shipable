package dev.onrcanogul.appbackend.core.api.error;

/**
 * Error codes the platform emits.
 *
 * <p>Your app's own codes do not belong here — define them in the {@code domain} module.
 * Keeping these stable matters: shipped mobile clients branch on them, and an old client
 * will be in the wild long after you change the server.
 */
public final class ErrorCodes {

    // --- request pipeline ---
    public static final String VALIDATION_FAILED = "validation_failed";
    public static final String MALFORMED_REQUEST = "malformed_request";
    public static final String RATE_LIMITED = "rate_limited";
    public static final String DUPLICATE_REQUEST = "duplicate_request";
    public static final String INTERNAL_ERROR = "internal_error";

    // --- auth ---
    public static final String UNAUTHORIZED = "unauthorized";
    public static final String FORBIDDEN = "forbidden";
    public static final String TOKEN_EXPIRED = "token_expired";
    public static final String TOKEN_INVALID = "token_invalid";

    // --- resources ---
    public static final String NOT_FOUND = "not_found";
    public static final String CONFLICT = "conflict";

    // --- commerce ---
    public static final String QUOTA_EXCEEDED = "quota_exceeded";
    public static final String ENTITLEMENT_REQUIRED = "entitlement_required";

    // --- client lifecycle ---
    public static final String APP_VERSION_UNSUPPORTED = "app_version_unsupported";
    public static final String MAINTENANCE_MODE = "maintenance_mode";

    private ErrorCodes() {
    }
}
