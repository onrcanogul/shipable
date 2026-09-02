package dev.onrcanogul.appbackend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import dev.onrcanogul.appbackend.billing.api.model.EntitlementId;
import dev.onrcanogul.appbackend.billing.api.port.BillingProvider;
import dev.onrcanogul.appbackend.billing.api.port.BillingService;
import dev.onrcanogul.appbackend.billing.api.port.WebhookAuthenticator;
import dev.onrcanogul.appbackend.billing.internal.persistence.repository.EntitlementSnapshotRepository;
import dev.onrcanogul.appbackend.billing.internal.persistence.repository.ProcessedWebhookEventRepository;
import dev.onrcanogul.appbackend.core.api.error.AppException;
import dev.onrcanogul.appbackend.core.api.error.ErrorCodes;
import dev.onrcanogul.appbackend.core.api.model.UserId;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class BillingModuleContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(EntitlementSnapshotRepository.class, () -> mock(EntitlementSnapshotRepository.class))
            .withBean(ProcessedWebhookEventRepository.class, () -> mock(ProcessedWebhookEventRepository.class))
            .withPropertyValues(
                    "app.billing.revenue-cat.api-key=test-key",
                    "app.billing.revenue-cat.api-base-url=http://localhost:9",
                    "app.billing.revenue-cat.webhook-secret=test-webhook-secret",
                    "app.billing.revenue-cat.request-timeout=3s")
            .withUserConfiguration(BillingModuleConfiguration.class);

    @Test
    @DisplayName("module configuration loads and exposes BillingService")
    void contextLoads() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(BillingService.class);
            assertThat(context).hasSingleBean(BillingProvider.class);
            assertThat(context).hasSingleBean(WebhookAuthenticator.class);
        });
    }

    @Test
    @DisplayName("a user with no recorded purchase holds no entitlements")
    void unknownUsersHoldNothing() {
        runner.run(context -> {
            BillingService billing = context.getBean(BillingService.class);
            UserId userId = UserId.newId();

            assertThat(billing.entitlementsOf(userId).isPaying()).isFalse();
            assertThat(billing.hasEntitlement(userId, EntitlementId.of("pro"))).isFalse();
        });
    }

    @Test
    @DisplayName("requireEntitlement refuses with entitlement_required rather than passing silently")
    void requireEntitlementRefuses() {
        runner.run(context -> {
            BillingService billing = context.getBean(BillingService.class);

            assertThatThrownBy(() -> billing.requireEntitlement(UserId.newId(), EntitlementId.of("pro")))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).error().code())
                    .isEqualTo(ErrorCodes.ENTITLEMENT_REQUIRED);
        });
    }

    @Test
    @DisplayName("a webhook without the shared secret is not authentic")
    void webhookAuthenticationRejectsStrangers() {
        runner.run(context -> {
            WebhookAuthenticator authenticator = context.getBean(WebhookAuthenticator.class);

            assertThat(authenticator.isAuthentic("test-webhook-secret")).isTrue();
            assertThat(authenticator.isAuthentic("wrong")).isFalse();
            assertThat(authenticator.isAuthentic("")).isFalse();
            assertThat(authenticator.isAuthentic(null)).isFalse();
        });
    }
}
