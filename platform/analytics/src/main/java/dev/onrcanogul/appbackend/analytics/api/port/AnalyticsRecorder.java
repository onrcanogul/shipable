package dev.onrcanogul.appbackend.analytics.api.port;

import dev.onrcanogul.appbackend.analytics.api.model.AnalyticsEvent;

/**
 * Records a product event.
 *
 * <p>Must never throw and never block for long. Analytics is the least important thing in
 * any request; an outage at your analytics vendor must not become an outage in your app.
 */
public interface AnalyticsRecorder {

    void record(AnalyticsEvent event);
}
