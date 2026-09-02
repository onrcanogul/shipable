package dev.onrcanogul.appbackend.privacy;

import dev.onrcanogul.appbackend.core.api.port.UserDataContributor;
import dev.onrcanogul.appbackend.privacy.api.port.AccountDeletionService;
import dev.onrcanogul.appbackend.privacy.api.port.DataExportService;
import dev.onrcanogul.appbackend.privacy.internal.persistence.repository.DeletionRequestRepository;
import dev.onrcanogul.appbackend.privacy.internal.service.DefaultAccountDeletionService;
import dev.onrcanogul.appbackend.privacy.internal.service.DefaultDataExportService;
import dev.onrcanogul.appbackend.privacy.internal.web.AccountPrivacyController;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Everything the privacy module hands to the outside world.
 *
 * <p>Note how deletion and export are wired: Spring injects <i>every</i>
 * {@link UserDataContributor} bean in the application. A module added later joins both
 * flows by publishing one bean, which is what stops deletion from quietly going stale.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PrivacyProperties.class)
public class PrivacyModuleConfiguration {

    @Bean
    public AccountDeletionService accountDeletionService(
            DeletionRequestRepository repository,
            List<UserDataContributor> contributors,
            PrivacyProperties properties,
            Clock clock) {
        return new DefaultAccountDeletionService(repository, contributors, properties, clock);
    }

    @Bean
    public DataExportService dataExportService(List<UserDataContributor> contributors, Clock clock) {
        return new DefaultDataExportService(contributors, clock);
    }

    @Bean
    public AccountPrivacyController accountPrivacyController(
            AccountDeletionService deletionService, DataExportService exportService) {
        return new AccountPrivacyController(deletionService, exportService);
    }
}
