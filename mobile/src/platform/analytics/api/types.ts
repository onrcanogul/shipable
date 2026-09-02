/**
 * A product event.
 *
 * Keep properties free of personal data. Whatever goes in here ends up at whichever vendor
 * is plugged in later, and you will not remember what you put in it.
 */
export interface AnalyticsEvent {
  readonly name: string;
  readonly properties?: Readonly<Record<string, string | number | boolean>>;
}

/**
 * Records product events.
 *
 * Must never throw and never block. Analytics is the least important thing in any screen;
 * a vendor's bad afternoon must not become a crash.
 */
export interface AnalyticsRecorder {
  record(event: AnalyticsEvent): void;
  identify(userId: string | null): void;
}
