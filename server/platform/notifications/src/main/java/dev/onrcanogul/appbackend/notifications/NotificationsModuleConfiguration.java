package dev.onrcanogul.appbackend.notifications;

import dev.onrcanogul.appbackend.core.api.port.UserDataContributor;
import dev.onrcanogul.appbackend.notifications.api.port.DeviceTokenService;
import dev.onrcanogul.appbackend.notifications.api.port.EmailSender;
import dev.onrcanogul.appbackend.notifications.api.port.PushSender;
import dev.onrcanogul.appbackend.notifications.internal.persistence.repository.DeviceTokenRepository;
import dev.onrcanogul.appbackend.notifications.internal.service.DefaultDeviceTokenService;
import dev.onrcanogul.appbackend.notifications.internal.service.NoopEmailSender;
import dev.onrcanogul.appbackend.notifications.internal.service.NoopPushSender;
import dev.onrcanogul.appbackend.notifications.internal.service.NotificationsUserDataContributor;
import dev.onrcanogul.appbackend.notifications.internal.web.DeviceController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Everything the notifications module hands to the outside world.
 *
 * <p>Both senders are no-ops by default, guarded by {@code @ConditionalOnMissingBean}:
 * define your own {@link PushSender} or {@link EmailSender} bean and it takes over, with no
 * change to any calling code.
 */
@Configuration(proxyBeanMethods = false)
public class NotificationsModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean(PushSender.class)
    public PushSender pushSender() {
        return new NoopPushSender();
    }

    @Bean
    @ConditionalOnMissingBean(EmailSender.class)
    public EmailSender emailSender() {
        return new NoopEmailSender();
    }

    @Bean
    public DeviceTokenService deviceTokenService(DeviceTokenRepository repository) {
        return new DefaultDeviceTokenService(repository);
    }

    @Bean
    public UserDataContributor notificationsUserDataContributor(DeviceTokenRepository repository) {
        return new NotificationsUserDataContributor(repository);
    }

    @Bean
    public DeviceController deviceController(DeviceTokenService deviceTokenService) {
        return new DeviceController(deviceTokenService);
    }
}
