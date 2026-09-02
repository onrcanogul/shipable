# platform/analytics

One port and a no-op, so you can instrument the app before choosing a vendor.

## Responsibility

Record product events. That is the entire module.

## Tables it owns

None. Nothing is stored until you plug in a real recorder.

## Interfaces it exposes

- `AnalyticsRecorder` — `record(AnalyticsEvent)`.
- `AnalyticsEvent`.

Spring wiring: `AnalyticsModuleConfiguration`.

## Decisions worth knowing

- **`@ConditionalOnMissingBean`.** Define your own `AnalyticsRecorder` — PostHog, Amplitude,
  Mixpanel, or a table — and it takes over. Nothing that calls it changes.
- **Recording must never throw and never block for long.** Analytics is the least important
  thing in any request; a vendor's bad afternoon must not become yours.

## What it explicitly does NOT do

- **No sending, no storage, no batching.** `NoopAnalyticsRecorder` logs at DEBUG. Buffering
  and background dispatch are TODO for whoever writes the real one.
- **No PII handling.** Nothing scrubs event properties. Whatever you put in them ends up at
  whichever third party you plug in later.
- **No client-side analytics.** Mobile SDKs talk to the vendor directly; this is for events
  only the server knows about.
