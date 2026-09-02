package dev.onrcanogul.appbackend.quota.api.model;

/**
 * What is being metered, e.g. {@code "ai.requests"} or {@code "export.jobs"}.
 *
 * <p>A string rather than an enum, for the same reason entitlement ids are: a template
 * cannot know what your app meters. Declare the keys your app uses as constants in
 * {@code domain}.
 *
 * <p>Meter different units separately. One number covering "requests" and "seconds of
 * audio" means whichever is mispriced drains the budget.
 */
public record QuotaKey(String value) {

    public QuotaKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("QuotaKey must not be blank");
        }
        value = value.trim();
    }

    public static QuotaKey of(String value) {
        return new QuotaKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
