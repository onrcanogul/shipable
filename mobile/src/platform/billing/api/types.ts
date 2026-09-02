/** Mirrors EntitlementsResponse on the server. */

export interface ActiveEntitlement {
  readonly id: string;
  readonly productId: string | null;
  readonly store: string;
  readonly expiresAt: string | null;
  /** False once cancelled but before the period ends - the moment for a win-back offer. */
  readonly willRenew: boolean;
  /** Payment failed but the store is still retrying. The user keeps access. */
  readonly inGracePeriod: boolean;
}

export interface Entitlements {
  readonly paying: boolean;
  readonly entitlements: readonly ActiveEntitlement[];
  readonly checkedAt: string;
}

export const NO_ENTITLEMENTS: Entitlements = {
  paying: false,
  entitlements: [],
  checkedAt: new Date(0).toISOString(),
};

export function hasEntitlement(entitlements: Entitlements, id: string): boolean {
  return entitlements.entitlements.some((entitlement) => entitlement.id === id);
}
