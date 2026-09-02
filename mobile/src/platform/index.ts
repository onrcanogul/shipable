/**
 * The platform layer: everything an app needs before it does anything interesting.
 *
 * Mirrors the backend's `platform/` in responsibility, not in file layout - the client has
 * no tables and no SPI, so some server modules collapse to a couple of functions here.
 * Notably: account deletion and export live in `identity` rather than their own module, and
 * quota needs nothing beyond the `quota_exceeded` error code that `core` already exposes.
 */
export * from './core';
export * from './identity';
export * from './billing';
export * from './appconfig';
export * from './notifications';
export * from './analytics';
export { bootstrap, type BootstrapOptions, type BootstrapResult } from './bootstrap';
