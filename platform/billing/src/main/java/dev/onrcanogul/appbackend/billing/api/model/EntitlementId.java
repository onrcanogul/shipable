package dev.onrcanogul.appbackend.billing.api.model;

/**
 * The identifier of a RevenueCat entitlement, e.g. {@code "pro"}.
 *
 * <p>A string rather than an enum on purpose: entitlement ids are configured in the
 * RevenueCat dashboard and differ per app. Baking them into an enum here would mean this
 * module knows what your app sells, which is exactly what a template must not do.
 *
 * <p>Your {@code domain} module is the right place to declare the ids it cares about as
 * constants.
 */
public record EntitlementId(String value) {

    public EntitlementId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EntitlementId must not be blank");
        }
        value = value.trim();
    }

    public static EntitlementId of(String value) {
        return new EntitlementId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
