package dev.onrcanogul.appbackend.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * RevenueCat settings, bound from {@code app.billing.*}.
 *
 * <p>Both secrets come from the environment. The webhook secret in particular is the only
 * thing standing between a public endpoint and a stranger granting themselves a
 * subscription.
 */
@Validated
@ConfigurationProperties(prefix = "app.billing")
public record BillingProperties(@Valid @NotNull RevenueCat revenueCat) {

    /**
     * @param apiKey        RevenueCat v2 secret API key, from APP_REVENUECAT_API_KEY.
     *                      A <b>secret</b> key, never the public SDK key the mobile app
     *                      ships with
     * @param apiBaseUrl    overridable so tests can point at a stub
     * @param webhookSecret the value RevenueCat sends in the Authorization header; you
     *                      choose it in the dashboard. From APP_REVENUECAT_WEBHOOK_SECRET
     * @param requestTimeout keep it short - a paywall check must not hang on a third party
     */
    public record RevenueCat(
            @NotBlank String apiKey,
            @NotBlank String apiBaseUrl,
            @NotBlank String webhookSecret,
            @NotNull Duration requestTimeout) {
    }
}
