package dev.onrcanogul.appbackend.quota;

import dev.onrcanogul.appbackend.billing.api.port.BillingService;
import dev.onrcanogul.appbackend.quota.api.port.QuotaPolicy;
import dev.onrcanogul.appbackend.quota.api.port.QuotaService;
import dev.onrcanogul.appbackend.quota.internal.service.DefaultQuotaService;
import dev.onrcanogul.appbackend.quota.internal.service.DenyByDefaultQuotaPolicy;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Everything the quota module hands to the outside world. */
@Configuration(proxyBeanMethods = false)
public class QuotaModuleConfiguration {

    /**
     * Your app overrides this by defining its own {@link QuotaPolicy} bean in
     * {@code domain}. The fallback grants nothing, so a missing policy fails loudly on the
     * first call rather than silently on the invoice.
     */
    @Bean
    @ConditionalOnMissingBean(QuotaPolicy.class)
    public QuotaPolicy quotaPolicy() {
        return new DenyByDefaultQuotaPolicy();
    }

    @Bean
    public QuotaService quotaService(BillingService billing, QuotaPolicy policy, Clock clock) {
        return new DefaultQuotaService(billing, policy, clock);
    }
}
