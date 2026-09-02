package dev.onrcanogul.appbackend.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.onrcanogul.appbackend.billing.api.model.CustomerEntitlements;
import dev.onrcanogul.appbackend.billing.api.port.BillingService;
import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.quota.api.model.QuotaKey;
import dev.onrcanogul.appbackend.quota.api.model.QuotaLimit;
import dev.onrcanogul.appbackend.quota.api.port.QuotaPolicy;
import dev.onrcanogul.appbackend.quota.api.port.QuotaService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class QuotaModuleContextTest {

    private static final QuotaKey KEY = QuotaKey.of("ai.requests");

    private final BillingService billing = mock(BillingService.class);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(BillingService.class, () -> billing)
            .withUserConfiguration(QuotaModuleConfiguration.class);

    @Test
    @DisplayName("module configuration loads with a fallback policy in place")
    void contextLoads() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(QuotaService.class);
            assertThat(context).hasSingleBean(QuotaPolicy.class);
        });
    }

    @Test
    @DisplayName("an unconfigured limit denies rather than allows")
    void unconfiguredLimitDenies() {
        UserId userId = UserId.newId();
        when(billing.entitlementsOf(any())).thenReturn(CustomerEntitlements.none(userId, Instant.now()));

        runner.run(context -> {
            QuotaService quota = context.getBean(QuotaService.class);
            assertThat(quota.check(userId, KEY, 1).allowed()).isFalse();
        });
    }

    @Test
    @DisplayName("an app-supplied policy replaces the fallback")
    void appPolicyOverridesTheFallback() {
        UserId userId = UserId.newId();
        when(billing.entitlementsOf(any())).thenReturn(CustomerEntitlements.none(userId, Instant.now()));

        runner.withBean(QuotaPolicy.class, () -> entitlements -> List.of(QuotaLimit.unlimited(KEY)))
                .run(context -> {
                    QuotaService quota = context.getBean(QuotaService.class);
                    assertThat(quota.check(userId, KEY, 1).allowed()).isTrue();
                });
    }
}
