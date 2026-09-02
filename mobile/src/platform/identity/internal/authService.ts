import { request, setAuthTokenProvider, type AuthTokenProvider } from '../../core';
import type { AuthProvider, AuthenticationResponse, Session } from '../api/types';
import { toSession } from '../api/types';
import { deviceId } from './deviceId';
import { clearSession, loadSession, saveSession } from './sessionStore';

/**
 * Owns the session: obtaining it, storing it, refreshing it, ending it.
 *
 * Held in a module-level variable rather than React state because the HTTP client needs it
 * outside the component tree — a request fired from a background task still has to be
 * authenticated. The React layer subscribes for rendering; it does not own the value.
 */

type Listener = (session: Session | null) => void;

let session: Session | null = null;
let listeners: Listener[] = [];

/**
 * Refresh this long before the token actually expires.
 *
 * Waiting for a 401 works, but costs the user a visible round trip on the first action
 * after a token lapses. A minute of slack avoids that without meaningfully shortening the
 * token's life.
 */
const REFRESH_SKEW_MS = 60_000;

function publish(): void {
  listeners.forEach((listener) => listener(session));
}

export function currentSession(): Session | null {
  return session;
}

export function subscribe(listener: Listener): () => void {
  listeners.push(listener);
  listener(session);
  return () => {
    listeners = listeners.filter((entry) => entry !== listener);
  };
}

/** Reads any stored session at launch, before the first screen renders. */
export async function restoreSession(): Promise<Session | null> {
  session = await loadSession();
  publish();
  return session;
}

async function adopt(response: AuthenticationResponse): Promise<Session> {
  session = toSession(response);
  await saveSession(session);
  publish();
  return session;
}

/**
 * Starts or resumes an anonymous session for this device.
 *
 * The app is usable before anyone signs up, which is the point. Call
 * {@link linkAccount} later to keep their data when they do.
 */
export async function signInAnonymously(): Promise<Session> {
  const response = await request<AuthenticationResponse>('/api/v1/auth/anonymous', {
    method: 'POST',
    anonymous: true,
    body: { deviceId: await deviceId() },
    // A stable key: two taps, or a retry after a timeout, must not create two accounts.
    idempotencyKey: `anonymous-sign-in:${await deviceId()}`,
  });
  return adopt(response);
}

export async function signInWithProvider(
  provider: AuthProvider,
  identityToken: string,
): Promise<Session> {
  const response = await request<AuthenticationResponse>('/api/v1/auth/social', {
    method: 'POST',
    anonymous: true,
    body: { provider, identityToken },
  });
  return adopt(response);
}

/**
 * Attaches a real account to the current anonymous session.
 *
 * Sent authenticated on purpose: the server takes the anonymous user from the bearer token,
 * never from the body. A client that could name the account to absorb could absorb someone
 * else's.
 */
export async function linkAccount(
  provider: AuthProvider,
  identityToken: string,
): Promise<Session> {
  const response = await request<AuthenticationResponse>('/api/v1/auth/link', {
    method: 'POST',
    body: { provider, identityToken },
  });
  return adopt(response);
}

export async function signOut(): Promise<void> {
  const refreshToken = session?.refreshToken;
  session = null;
  await clearSession();
  publish();

  if (refreshToken) {
    try {
      await request<void>('/api/v1/auth/signout', {
        method: 'POST',
        anonymous: true,
        body: { refreshToken },
      });
    } catch {
      // Local state is already cleared, which is what the user asked for. A server that
      // cannot be reached leaves a token to expire on its own; failing the sign-out would
      // leave the user apparently still signed in.
    }
  }
}

/**
 * The provider core uses. Installed once, at startup.
 *
 * This is the seam that lets `core` handle 401s without knowing what a session is.
 */
export const authTokenProvider: AuthTokenProvider = {
  async accessToken() {
    if (!session) {
      return null;
    }
    // Refresh slightly early so the next call does not have to fail first.
    if (Date.now() >= session.accessTokenExpiresAt - REFRESH_SKEW_MS) {
      return (await this.refresh()) ? (session?.accessToken ?? null) : null;
    }
    return session.accessToken;
  },

  async refresh() {
    const refreshToken = session?.refreshToken;
    if (!refreshToken) {
      return false;
    }
    try {
      const response = await request<AuthenticationResponse>('/api/v1/auth/refresh', {
        method: 'POST',
        anonymous: true,
        body: { refreshToken },
      });
      await adopt(response);
      return true;
    } catch {
      // The refresh token is gone or revoked. Nothing left to try.
      await this.onSessionEnded();
      return false;
    }
  },

  async onSessionEnded() {
    session = null;
    await clearSession();
    publish();
  },
};

export function installAuthTokenProvider(): void {
  setAuthTokenProvider(authTokenProvider);
}
