import Purchases, { LOG_LEVEL } from 'react-native-purchases';
import { Platform } from 'react-native';

/**
 * The RevenueCat SDK, for buying things. Entitlement *checks* come from our backend.
 *
 * The one rule that makes the two agree: `logIn` with the same user id the backend knows.
 * RevenueCat calls it `app_user_id`, and it is how a webhook arriving at the server maps to
 * a row in our database. Get it wrong and purchases land on an anonymous RevenueCat
 * customer that nothing can be matched to.
 */

export interface RevenueCatKeys {
  readonly ios: string;
  readonly android: string;
}

let configured = false;

export function configureRevenueCat(keys: RevenueCatKeys, debug = false): void {
  if (configured) {
    return;
  }
  const apiKey = Platform.select({ ios: keys.ios, android: keys.android, default: '' });
  if (!apiKey) {
    // No key for this platform - web, or a build that never sells anything. Purchases stay
    // unavailable rather than the app failing to start.
    return;
  }
  if (debug) {
    Purchases.setLogLevel(LOG_LEVEL.DEBUG);
  }
  Purchases.configure({ apiKey });
  configured = true;
}

/**
 * Ties this device's purchases to our user. Call it right after sign-in, every time.
 *
 * Also call it after linking an anonymous account to a real one, so purchases made while
 * anonymous follow the user.
 */
export async function identifyForPurchases(userId: string): Promise<void> {
  if (!configured) {
    return;
  }
  await Purchases.logIn(userId);
}

/** Call on sign-out, so the next person on this device does not inherit the subscription. */
export async function forgetPurchaseIdentity(): Promise<void> {
  if (!configured) {
    return;
  }
  await Purchases.logOut();
}
