package dev.onrcanogul.appbackend.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.core.api.port.UserDataContributor;
import dev.onrcanogul.appbackend.privacy.api.model.DataExport;
import dev.onrcanogul.appbackend.privacy.api.port.AccountDeletionService;
import dev.onrcanogul.appbackend.privacy.api.port.DataExportService;
import dev.onrcanogul.appbackend.privacy.internal.persistence.repository.DeletionRequestRepository;
import java.time.Clock;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PrivacyModuleContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(DeletionRequestRepository.class, () -> mock(DeletionRequestRepository.class))
            .withPropertyValues("app.privacy.deletion-grace-period=7d")
            .withUserConfiguration(PrivacyModuleConfiguration.class);

    @Test
    @DisplayName("module configuration loads and exposes both privacy ports")
    void contextLoads() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AccountDeletionService.class);
            assertThat(context).hasSingleBean(DataExportService.class);
        });
    }

    @Test
    @DisplayName("the export collects one section per contributor, keyed by data set name")
    void exportFansOutOverContributors() {
        runner.withBean("orders", UserDataContributor.class, () -> contributor("orders"))
                .withBean("devices", UserDataContributor.class, () -> contributor("devices"))
                .run(context -> {
                    DataExport export = context.getBean(DataExportService.class).exportFor(UserId.newId());

                    assertThat(export.dataSets()).containsOnlyKeys("orders", "devices");
                    assertThat(export.generatedAt()).isNotNull();
                });
    }

    @Test
    @DisplayName("an export with no contributors is an empty document, not a failure")
    void exportWithoutContributorsIsEmpty() {
        runner.run(context -> {
            DataExport export = context.getBean(DataExportService.class).exportFor(UserId.newId());
            assertThat(export.dataSets()).isEmpty();
        });
    }

    private static UserDataContributor contributor(String name) {
        return new UserDataContributor() {
            @Override
            public String dataSetName() {
                return name;
            }

            @Override
            public Map<String, Object> exportFor(UserId userId) {
                return Map.of("rows", 0);
            }

            @Override
            public void eraseFor(UserId userId) {
                // nothing to erase in this test double
            }
        };
    }
}
