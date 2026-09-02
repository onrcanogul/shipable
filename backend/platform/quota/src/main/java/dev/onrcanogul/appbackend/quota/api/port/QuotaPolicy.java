package dev.onrcanogul.appbackend.quota.api.port;

import dev.onrcanogul.appbackend.billing.api.model.CustomerEntitlements;
import dev.onrcanogul.appbackend.quota.api.model.QuotaLimit;
import java.util.List;

/**
 * Maps what a user has paid for onto what they may use.
 *
 * <p>This is the seam your app implements. The platform knows how to count and enforce;
 * only your app knows that "pro" means 500 requests a day.
 *
 * <p>Takes the whole {@link CustomerEntitlements} rather than a single plan id, because
 * users can hold several entitlements at once and the answer is usually "the most generous
 * one wins".
 */
public interface QuotaPolicy {

    List<QuotaLimit> limitsFor(CustomerEntitlements entitlements);
}
