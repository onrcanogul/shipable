package dev.onrcanogul.appbackend.billing;

import dev.onrcanogul.appbackend.billing.api.port.BillingProvider;
import dev.onrcanogul.appbackend.billing.api.port.BillingService;
import dev.onrcanogul.appbackend.billing.api.port.WebhookAuthenticator;
import dev.onrcanogul.appbackend.billing.internal.client.RevenueCatBillingProvider;
import dev.onrcanogul.appbackend.billing.internal.client.RevenueCatWebhookAuthenticator;
import dev.onrcanogul.appbackend.billing.internal.persistence.repository.EntitlementSnapshotRepository;
import dev.onrcanogul.appbackend.billing.internal.persistence.repository.ProcessedWebhookEventRepository;
import dev.onrcanogul.appbackend.billing.internal.service.DefaultBillingService;
import dev.onrcanogul.appbackend.billing.internal.service.RevenueCatWebhookProcessor;
import dev.onrcanogul.appbackend.billing.internal.web.BillingController;
import dev.onrcanogul.appbackend.billing.internal.web.BillingWebhookController;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Everything the billing module hands to the outside world.
 *
 * <p>Other modules should inject {@code BillingService}. {@code BillingProvider} is the
 * outbound seam: swap the bean to replace RevenueCat.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BillingProperties.class)
public class BillingModuleConfiguration {

    /**
     * A RestClient scoped to this module, with a deliberately short timeout: a paywall
     * check must never hang on a third party.
     */
    @Bean
    public RestClient revenueCatRestClient(BillingProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.revenueCat().requestTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.revenueCat().requestTimeout().toMillis());

        return RestClient.builder()
                .baseUrl(properties.revenueCat().apiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.revenueCat().apiKey())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public BillingProvider billingProvider(RestClient revenueCatRestClient, BillingProperties properties) {
        return new RevenueCatBillingProvider(revenueCatRestClient, properties);
    }

    @Bean
    public WebhookAuthenticator webhookAuthenticator(BillingProperties properties) {
        return new RevenueCatWebhookAuthenticator(properties);
    }

    @Bean
    public BillingService billingService(
            BillingProvider provider, EntitlementSnapshotRepository snapshots, Clock clock) {
        return new DefaultBillingService(provider, snapshots, clock);
    }

    @Bean
    public RevenueCatWebhookProcessor revenueCatWebhookProcessor(ProcessedWebhookEventRepository processedEvents) {
        return new RevenueCatWebhookProcessor(processedEvents);
    }

    @Bean
    public BillingController billingController(BillingService billingService) {
        return new BillingController(billingService);
    }

    @Bean
    public BillingWebhookController billingWebhookController(
            WebhookAuthenticator authenticator, RevenueCatWebhookProcessor processor) {
        return new BillingWebhookController(authenticator, processor);
    }
}
