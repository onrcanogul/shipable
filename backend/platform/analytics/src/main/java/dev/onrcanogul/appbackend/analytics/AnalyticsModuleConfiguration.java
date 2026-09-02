package dev.onrcanogul.appbackend.analytics;

import dev.onrcanogul.appbackend.analytics.api.port.AnalyticsRecorder;
import dev.onrcanogul.appbackend.analytics.internal.service.NoopAnalyticsRecorder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Everything the analytics module hands to the outside world: one port and a no-op.
 *
 * <p>Define your own {@link AnalyticsRecorder} bean and it takes over.
 */
@Configuration(proxyBeanMethods = false)
public class AnalyticsModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean(AnalyticsRecorder.class)
    public AnalyticsRecorder analyticsRecorder() {
        return new NoopAnalyticsRecorder();
    }
}
