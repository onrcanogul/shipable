package dev.onrcanogul.appbackend.quota.internal.service;

import dev.onrcanogul.appbackend.billing.api.model.CustomerEntitlements;
import dev.onrcanogul.appbackend.quota.api.model.QuotaLimit;
import dev.onrcanogul.appbackend.quota.api.port.QuotaPolicy;
import java.util.List;

/**
 * The fallback used when the app has not registered a policy.
 *
 * <p>It grants nothing. An app that forgets to define its limits should hit a wall on the
 * first call rather than quietly running on the template author's guess at sensible
 * numbers — which would be wrong for every app, and expensive for the ones calling an LLM.
 *
 * <p>Replace it by defining your own {@link QuotaPolicy} bean in {@code domain}.
 */
public class DenyByDefaultQuotaPolicy implements QuotaPolicy {

    @Override
    public List<QuotaLimit> limitsFor(CustomerEntitlements entitlements) {
        return List.of();
    }
}
