package dev.onrcanogul.appbackend.quota.internal.service;

import dev.onrcanogul.appbackend.billing.api.port.BillingService;
import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.quota.api.model.QuotaDecision;
import dev.onrcanogul.appbackend.quota.api.model.QuotaKey;
import dev.onrcanogul.appbackend.quota.api.model.QuotaLimit;
import dev.onrcanogul.appbackend.quota.api.port.QuotaPolicy;
import dev.onrcanogul.appbackend.quota.api.port.QuotaService;
import java.time.Clock;
import java.util.Optional;

/**
 * Skeleton implementation of {@link QuotaService}.
 *
 * <p>The join that matters is already here and is real: entitlements come from
 * {@link BillingService}, limits come from {@link QuotaPolicy}, and this class puts them
 * together. Only the window arithmetic and the ledger writes are TODO.
 *
 * <p>A limit that is not configured <b>denies</b>. An undefined limit is a gap in
 * configuration, not permission to spend freely — and on an app whose costs are someone
 * else's API bill, that distinction is the difference between a bug and an invoice.
 */
public class DefaultQuotaService implements QuotaService {

    private final BillingService billing;
    private final QuotaPolicy policy;
    private final Clock clock;

    public DefaultQuotaService(BillingService billing, QuotaPolicy policy, Clock clock) {
        this.billing = billing;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public QuotaDecision check(UserId userId, QuotaKey key, long requestedAmount) {
        Optional<QuotaLimit> limit = policy.limitsFor(billing.entitlementsOf(userId)).stream()
                .filter(candidate -> candidate.key().equals(key))
                .findFirst();

        if (limit.isEmpty()) {
            return QuotaDecision.deny(key, clock.instant(),
                    "No limit configured for '" + key + "' at this entitlement level");
        }
        if (limit.get().isUnlimited()) {
            return QuotaDecision.allow(key, -1L, clock.instant());
        }

        // TODO: sum QuotaUsageRepository.sumInWindow over (now - window, now], allow when
        // used + requestedAmount <= limit.amount(), and set resetsAt to the window end.
        throw new UnsupportedOperationException("TODO: quota window accounting is not implemented");
    }

    @Override
    public void record(UserId userId, QuotaKey key, long consumedAmount) {
        // TODO: append a QuotaUsageEntity row. Append-only, so work that failed after
        // spending real money is still charged - otherwise a retry loop on a failing
        // upstream call is free for the user and not for you.
        throw new UnsupportedOperationException("TODO: quota recording is not implemented");
    }
}
