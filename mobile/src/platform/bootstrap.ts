import { configureCore, setLifecycleListener, type LifecycleListener } from './core';
import { configureRevenueCat, identifyForPurchases, type RevenueCatKeys } from './billing';
import { fetchRemoteConfig } from './appconfig';
import { identifyUser } from './analytics';
import {
  currentSession,
  installAuthTokenProvider,
  restoreSession,
  signInAnonymously,
} from './identity';

/**
 * Everything that has to happen before the first screen renders, in the order it has to
 * happen in.
 *
 * The mirror of the host module on the server: one place that wires the modules together,
 * so "what runs at launch" is one file rather than a trail through the component tree.
 */

export interface BootstrapOptions {
  readonly apiBaseUrl: string;
  readonly revenueCatKeys?: RevenueCatKeys;
  /**
   * Sign in anonymously when there is no stored session, so the app is usable before
   * anyone signs up. Turn it off if your app has nothing to show a signed-out user.
   */
  readonly signInAnonymouslyOnLaunch?: boolean;
  readonly lifecycle?: LifecycleListener;
  readonly requestTimeoutMs?: number;
}

export interface BootstrapResult {
  readonly forceUpdate: boolean;
  readonly maintenanceMode: boolean;
  readonly maintenanceMessage: string | null;
  readonly signedIn: boolean;
}

/**
 * Never throws.
 *
 * A launch path that can fail is a launch path that bricks the app on a bad network. Each
 * step degrades on its own: no config means permissive defaults, no session means signed
 * out, no RevenueCat means purchases unavailable. The screen decides what to do with the
 * result.
 */
export async function bootstrap(options: BootstrapOptions): Promise<BootstrapResult> {
  configureCore({
    baseUrl: options.apiBaseUrl,
    requestTimeoutMs: options.requestTimeoutMs ?? 15_000,
  });

  // Before any request that might get a 401: core needs somewhere to ask for a token.
  installAuthTokenProvider();
  setLifecycleListener(options.lifecycle ?? {});

  // First, because it is the one call that works without a session and the one whose
  // answer can stop everything else - an app below the minimum version should be told to
  // update rather than sent to sign in.
  const config = await fetchRemoteConfig();
  if (config.forceUpdate || config.maintenanceMode) {
    return {
      forceUpdate: config.forceUpdate,
      maintenanceMode: config.maintenanceMode,
      maintenanceMessage: config.maintenanceMessage,
      signedIn: false,
    };
  }

  if (options.revenueCatKeys) {
    configureRevenueCat(options.revenueCatKeys, __DEV__);
  }

  let session = await restoreSession();
  if (!session && options.signInAnonymouslyOnLaunch) {
    try {
      session = await signInAnonymously();
    } catch {
      // Offline on a first launch. The app opens signed out; the screen can retry.
      session = null;
    }
  }

  if (session) {
    // Same id on both sides, or a webhook arriving at the backend has no row to match.
    await identifyForPurchases(session.userId).catch(() => undefined);
    identifyUser(session.userId);
  }

  return {
    forceUpdate: false,
    maintenanceMode: false,
    maintenanceMessage: null,
    signedIn: currentSession() !== null,
  };
}
