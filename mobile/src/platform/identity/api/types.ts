/** Mirrors the backend's identity model. */

export type AuthProvider = 'APPLE' | 'GOOGLE' | 'ANONYMOUS_DEVICE';

/** What every auth endpoint returns. Mirrors AuthenticationResponse on the server. */
export interface AuthenticationResponse {
  readonly userId: string;
  readonly anonymous: boolean;
  readonly accessToken: string;
  readonly refreshToken: string;
  readonly accessTokenExpiresAt: string;
  readonly refreshTokenExpiresAt: string;
}

/** The session as this app holds it. */
export interface Session {
  readonly userId: string;
  readonly anonymous: boolean;
  readonly accessToken: string;
  readonly refreshToken: string;
  /** Epoch millis. Used to refresh slightly early rather than waiting for a 401. */
  readonly accessTokenExpiresAt: number;
}

export function toSession(response: AuthenticationResponse): Session {
  return {
    userId: response.userId,
    anonymous: response.anonymous,
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    accessTokenExpiresAt: Date.parse(response.accessTokenExpiresAt),
  };
}
