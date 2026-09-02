package dev.onrcanogul.appbackend.analytics.internal.service;

import dev.onrcanogul.appbackend.analytics.api.model.AnalyticsEvent;
import dev.onrcanogul.appbackend.analytics.api.port.AnalyticsRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs instead of recording.
 *
 * <p>The default, so the app can be instrumented from day one without picking a vendor.
 * Swap in PostHog, Amplitude, Mixpanel or a database table by defining your own
 * {@link AnalyticsRecorder} bean.
 *
 * <p>DEBUG rather than INFO: unlike a push, an analytics event in the log is pure noise in
 * production.
 *
 * <p>TODO for a real implementation: buffer and send in batches on a background thread, and
 * drop events when the buffer is full. Blocking a request on an analytics call is how a
 * vendor's bad afternoon becomes yours.
 */
public class NoopAnalyticsRecorder implements AnalyticsRecorder {

    private static final Logger log = LoggerFactory.getLogger(NoopAnalyticsRecorder.class);

    @Override
    public void record(AnalyticsEvent event) {
        log.debug("[no-op analytics] {} user={} props={}", event.name(), event.userId(), event.properties());
    }
}
