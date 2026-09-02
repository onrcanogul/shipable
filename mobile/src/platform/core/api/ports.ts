/**
 * What core needs from other modules, without depending on them.
 *
 * The same inversion the backend uses: core owns the HTTP pipeline and knows nothing about
 * how a session is obtained. `identity` installs the token provider at startup; if it never
 * does, every request simply goes out unauthenticated.
 */

export interface AuthTokenProvider {
  /** The current access token, or null when there is no session. */
  accessToken(): Promise<string | null>;

  /**
   * Exchanges the refresh token for a new pair.
   *
   * @returns true when a fresh access token is now available and the request is worth
   *          retrying; false when the session is gone for good
   */
  refresh(): Promise<boolean>;

  /** Called when the server rejects the session outright. Clears local state. */
  onSessionEnded(): Promise<void>;
}

/**
 * Conditions the whole app has to react to, not any one screen.
 *
 * The server can answer 426 to *any* call, so handling it per call site would mean handling
 * it in every call site. It is surfaced here once instead.
 */
export interface LifecycleListener {
  /** This build is below the minimum supported version. Show a blocking update screen. */
  onUpdateRequired?(): void;
  /** The API is rate limiting this device. */
  onRateLimited?(retryAfterSeconds: number): void;
  /** The session ended and could not be refreshed. Send the user back to sign-in. */
  onSessionEnded?(): void;
}

let authTokenProvider: AuthTokenProvider | null = null;
let lifecycleListener: LifecycleListener = {};

export function setAuthTokenProvider(provider: AuthTokenProvider | null): void {
  authTokenProvider = provider;
}

export function getAuthTokenProvider(): AuthTokenProvider | null {
  return authTokenProvider;
}

export function setLifecycleListener(listener: LifecycleListener): void {
  lifecycleListener = listener;
}

export function getLifecycleListener(): LifecycleListener {
  return lifecycleListener;
}
