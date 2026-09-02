package dev.onrcanogul.appbackend.billing.internal.client;

import dev.onrcanogul.appbackend.billing.BillingProperties;
import dev.onrcanogul.appbackend.billing.api.model.CustomerEntitlements;
import dev.onrcanogul.appbackend.billing.api.port.BillingProvider;
import dev.onrcanogul.appbackend.core.api.model.Result;
import dev.onrcanogul.appbackend.core.api.model.UserId;
import org.springframework.web.client.RestClient;

/**
 * Reads subscription state from the RevenueCat REST API.
 *
 * <p>TODO:
 * <ol>
 *   <li>{@code GET /v2/projects/{project_id}/customers/{app_user_id}} with
 *       {@code Authorization: Bearer <secret api key>}. Note this is the v2 API - the v1
 *       {@code /subscribers} endpoint is deprecated and shaped differently.</li>
 *   <li>Map the entitlements array to {@link CustomerEntitlements}. Trust
 *       {@code expires_date} and the grace period flag rather than computing dates
 *       yourself; the stores have their own opinions about renewal timing.</li>
 *   <li>404 means "no purchases", not an error - return
 *       {@link CustomerEntitlements#none}.</li>
 *   <li>5xx and timeouts return an error {@code Result} so the caller can fall back on the
 *       stored snapshot instead of telling a paying user they are not subscribed.</li>
 * </ol>
 *
 * <p>The RestClient is built with a short timeout on purpose: this is called on sign-in and
 * on restore, and a slow third party must not turn into a slow app.
 */
public class RevenueCatBillingProvider implements BillingProvider {

    private final RestClient restClient;
    private final BillingProperties properties;

    public RevenueCatBillingProvider(RestClient restClient, BillingProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public Result<CustomerEntitlements> fetchEntitlements(UserId userId) {
        throw new UnsupportedOperationException("TODO: RevenueCat customer lookup is not implemented");
    }
}
