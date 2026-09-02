import { request } from '../../core';
import { NO_ENTITLEMENTS, type Entitlements } from '../api/types';

/**
 * What the user has paid for, according to **our** backend.
 *
 * Not from the RevenueCat SDK. The SDK knows what the store told this device; the backend
 * knows what it verified server-side, and that is what gates a paid feature. Trusting the
 * device means trusting a jailbroken one.
 *
 * The SDK is still used - for the paywall, the offerings and the purchase itself. It is the
 * *entitlement check* that comes from here.
 */
export async function fetchEntitlements(): Promise<Entitlements> {
  return request<Entitlements>('/api/v1/billing/entitlements');
}

/**
 * Asks the backend to re-read from RevenueCat.
 *
 * For restore-purchases and immediately after a purchase completes. Webhooks keep the
 * backend current the rest of the time, so this should not be polled.
 */
export async function refreshEntitlements(): Promise<Entitlements> {
  return request<Entitlements>('/api/v1/billing/entitlements/refresh', { method: 'POST' });
}

/** Safe default for a screen that renders before the first fetch resolves. */
export function noEntitlements(): Entitlements {
  return NO_ENTITLEMENTS;
}
