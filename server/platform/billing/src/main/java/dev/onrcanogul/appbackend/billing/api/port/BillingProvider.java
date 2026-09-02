package dev.onrcanogul.appbackend.billing.api.port;

import dev.onrcanogul.appbackend.billing.api.model.CustomerEntitlements;
import dev.onrcanogul.appbackend.core.api.model.Result;
import dev.onrcanogul.appbackend.core.api.model.UserId;

/**
 * The outbound side: fetching subscription state from the billing provider.
 *
 * <p>An interface so RevenueCat is replaceable. It is a good default — it absorbs the App
 * Store Server API and Google Play Developer API so you do not have to — but a template
 * that hard-codes a vendor ages badly.
 */
public interface BillingProvider {

    /**
     * @param userId used as RevenueCat's {@code app_user_id}, which is why {@code UserId}
     *               must stay stable for the life of an account
     */
    Result<CustomerEntitlements> fetchEntitlements(UserId userId);
}
