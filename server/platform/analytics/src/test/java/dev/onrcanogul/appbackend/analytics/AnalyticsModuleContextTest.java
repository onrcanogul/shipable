package dev.onrcanogul.appbackend.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import dev.onrcanogul.appbackend.analytics.api.model.AnalyticsEvent;
import dev.onrcanogul.appbackend.analytics.api.port.AnalyticsRecorder;
import dev.onrcanogul.appbackend.core.api.model.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AnalyticsModuleContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AnalyticsModuleConfiguration.class);

    @Test
    @DisplayName("module configuration loads with the no-op recorder in place")
    void contextLoads() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AnalyticsRecorder.class);
        });
    }

    @Test
    @DisplayName("recording never throws, including for an event with no user")
    void recordingIsHarmless() {
        runner.run(context -> {
            AnalyticsRecorder recorder = context.getBean(AnalyticsRecorder.class);
            assertThatCode(() -> recorder.record(AnalyticsEvent.of("app_opened", UserId.newId())))
                    .doesNotThrowAnyException();
            assertThatCode(() -> recorder.record(AnalyticsEvent.of("app_opened", null)))
                    .doesNotThrowAnyException();
        });
    }

    @Test
    @DisplayName("an app-supplied recorder replaces the no-op")
    void appRecorderOverridesTheNoop() {
        AnalyticsRecorder custom = event -> { };
        runner.withBean(AnalyticsRecorder.class, () -> custom)
                .run(context -> assertThat(context.getBean(AnalyticsRecorder.class)).isSameAs(custom));
    }
}
