import type { AnalyticsEvent, AnalyticsRecorder } from '../api/types';

/**
 * Logs in development, does nothing in production.
 *
 * The default, so the app can be instrumented from day one without choosing a vendor. Swap
 * in PostHog, Amplitude or Mixpanel with `setAnalyticsRecorder`; no call site changes.
 */
export const noopRecorder: AnalyticsRecorder = {
  record(event: AnalyticsEvent) {
    if (__DEV__) {
      console.log('[analytics]', event.name, event.properties ?? {});
    }
  },
  identify(userId: string | null) {
    if (__DEV__) {
      console.log('[analytics] identify', userId);
    }
  },
};
