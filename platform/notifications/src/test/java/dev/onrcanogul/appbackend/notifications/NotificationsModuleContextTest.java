package dev.onrcanogul.appbackend.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import dev.onrcanogul.appbackend.core.api.model.UserId;
import dev.onrcanogul.appbackend.core.api.port.UserDataContributor;
import dev.onrcanogul.appbackend.notifications.api.model.PushMessage;
import dev.onrcanogul.appbackend.notifications.api.port.EmailSender;
import dev.onrcanogul.appbackend.notifications.api.port.PushSender;
import dev.onrcanogul.appbackend.notifications.internal.persistence.repository.DeviceTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NotificationsModuleContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(DeviceTokenRepository.class, () -> mock(DeviceTokenRepository.class))
            .withUserConfiguration(NotificationsModuleConfiguration.class);

    @Test
    @DisplayName("module configuration loads with no-op senders in place")
    void contextLoads() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(PushSender.class);
            assertThat(context).hasSingleBean(EmailSender.class);
        });
    }

    @Test
    @DisplayName("the no-op sender does nothing and, crucially, does not throw")
    void noopSenderIsHarmless() {
        runner.run(context -> {
            PushSender sender = context.getBean(PushSender.class);
            assertThatCode(() -> sender.sendToUser(UserId.newId(), PushMessage.of("hi", "there")))
                    .doesNotThrowAnyException();
        });
    }

    @Test
    @DisplayName("the module registers itself with the deletion and export flows")
    void takesPartInAccountDeletion() {
        runner.run(context -> {
            UserDataContributor contributor = context.getBean(UserDataContributor.class);
            assertThat(contributor.dataSetName()).isEqualTo("devices");
        });
    }

    @Test
    @DisplayName("an app-supplied sender replaces the no-op")
    void appSenderOverridesTheNoop() {
        PushSender custom = (userId, message) -> { };
        runner.withBean(PushSender.class, () -> custom)
                .run(context -> assertThat(context.getBean(PushSender.class)).isSameAs(custom));
    }
}
