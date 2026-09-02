import { request } from '../../core';
import type { RemoteConfig } from '../api/types';

/**
 * The first call the app makes, before sign-in.
 *
 * Public on the server for exactly this reason: a client that must update, or that has hit
 * a maintenance window, needs to find that out without being able to authenticate.
 */

let cached: RemoteConfig | null = null;

/**
 * Deliberately permissive when the call fails.
 *
 * A config fetch that cannot reach the server must not block launch - that would turn a
 * flaky network into an unusable app. Nothing here is a security control: the server
 * enforces the version gate itself and answers 426 on any endpoint, so the worst case is
 * that the update screen appears one call later than it could have.
 */
const PERMISSIVE_FALLBACK: RemoteConfig = {
  minimumSupportedVersion: '0.0.0',
  latestVersion: '0.0.0',
  updateUrl: null,
  forceUpdate: false,
  maintenanceMode: false,
  maintenanceMessage: null,
  featureFlags: {},
};

export async function fetchRemoteConfig(): Promise<RemoteConfig> {
  try {
    cached = await request<RemoteConfig>('/api/v1/config', { anonymous: true });
    return cached;
  } catch {
    return cached ?? PERMISSIVE_FALLBACK;
  }
}

export function remoteConfig(): RemoteConfig {
  return cached ?? PERMISSIVE_FALLBACK;
}

/**
 * @param defaultValue used when the flag is unknown, so a typo fails predictably rather
 *                     than crashing the screen that checked it
 */
export function isFeatureEnabled(flag: string, defaultValue = false): boolean {
  return remoteConfig().featureFlags[flag] ?? defaultValue;
}
