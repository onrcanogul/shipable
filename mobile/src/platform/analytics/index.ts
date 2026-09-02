import type { AnalyticsEvent, AnalyticsRecorder } from './api/types';
import { noopRecorder } from './internal/noopRecorder';

export type { AnalyticsEvent, AnalyticsRecorder } from './api/types';

let recorder: AnalyticsRecorder = noopRecorder;

export function setAnalyticsRecorder(next: AnalyticsRecorder): void {
  recorder = next;
}

/** Never throws, whatever the recorder does. Analytics cannot be allowed to break a screen. */
export function track(name: string, properties?: AnalyticsEvent['properties']): void {
  try {
    recorder.record({ name, properties });
  } catch {
    // swallowed on purpose
  }
}

export function identifyUser(userId: string | null): void {
  try {
    recorder.identify(userId);
  } catch {
    // swallowed on purpose
  }
}
