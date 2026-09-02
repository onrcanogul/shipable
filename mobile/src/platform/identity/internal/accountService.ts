import { request } from '../../core';

/**
 * Account deletion and data export.
 *
 * Not optional: Apple and Google both require an app that lets people create an account to
 * let them delete it from inside the app. "Email us" is a review rejection.
 *
 * On the server these live in their own module because it owns tables and the contributor
 * SPI. Here they are two calls about the signed-in account, so they sit with the rest of the
 * account lifecycle.
 */

export interface DeletionStatus {
  readonly status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  readonly requestedAt: string;
  readonly scheduledFor: string;
  /** True while the grace period is still running - show a "cancel deletion" button. */
  readonly cancellable: boolean;
}

/** Schedules deletion after a grace period, during which the user can change their mind. */
export async function requestAccountDeletion(): Promise<DeletionStatus> {
  return request<DeletionStatus>('/api/v1/account/deletion', { method: 'POST' });
}

export async function cancelAccountDeletion(): Promise<DeletionStatus> {
  return request<DeletionStatus>('/api/v1/account/deletion', { method: 'DELETE' });
}

/** @returns null when no deletion has been requested (the server answers 204). */
export async function accountDeletionStatus(): Promise<DeletionStatus | null> {
  return (await request<DeletionStatus | undefined>('/api/v1/account/deletion')) ?? null;
}

/** Everything the backend holds about this user, assembled from every module. */
export async function exportAccountData(): Promise<unknown> {
  return request<unknown>('/api/v1/account/export');
}
