package dev.onrcanogul.appbackend.host;

import dev.onrcanogul.appbackend.analytics.AnalyticsModuleConfiguration;
import dev.onrcanogul.appbackend.appconfig.AppConfigModuleConfiguration;
import dev.onrcanogul.appbackend.billing.BillingModuleConfiguration;
import dev.onrcanogul.appbackend.core.CoreModuleConfiguration;
import dev.onrcanogul.appbackend.domain.DomainModuleConfiguration;
import dev.onrcanogul.appbackend.identity.IdentityModuleConfiguration;
import dev.onrcanogul.appbackend.notifications.NotificationsModuleConfiguration;
import dev.onrcanogul.appbackend.privacy.PrivacyModuleConfiguration;
import dev.onrcanogul.appbackend.quota.QuotaModuleConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The application. One process, every module.
 *
 * <p>Modules are imported by name rather than discovered by component scan. The list below
 * is therefore the complete answer to "what is running?" — and removing a module you do not
 * need is deleting one line here and one dependency in {@code pom.xml}.
 *
 * <p>{@code @SpringBootApplication} deliberately scans nothing outside this package: the
 * platform's {@code internal} packages stay closed, and beans appear only where a module
 * configuration says so.
 */
@SpringBootApplication
@EnableScheduling
@Import({
        CoreModuleConfiguration.class,
        IdentityModuleConfiguration.class,
        BillingModuleConfiguration.class,
        QuotaModuleConfiguration.class,
        NotificationsModuleConfiguration.class,
        AnalyticsModuleConfiguration.class,
        AppConfigModuleConfiguration.class,
        PrivacyModuleConfiguration.class,
        DomainModuleConfiguration.class,
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
