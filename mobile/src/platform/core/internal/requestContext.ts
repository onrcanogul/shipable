import * as Application from 'expo-application';
import * as Crypto from 'expo-crypto';
import { Platform } from 'react-native';

/**
 * The headers the backend's RequestContextBindingFilter reads.
 *
 * Sending them costs nothing and buys a great deal: the request id ties a user's report to
 * every log line the server wrote, and the platform and version drive the force-update
 * gate. A client that omits the version is treated as supported, so forgetting these is
 * silent - which is exactly why they are set centrally rather than per call.
 */
export const HEADER_REQUEST_ID = 'X-Request-Id';
export const HEADER_CLIENT_PLATFORM = 'X-Client-Platform';
export const HEADER_APP_VERSION = 'X-App-Version';
export const HEADER_IDEMPOTENCY_KEY = 'Idempotency-Key';

function clientPlatform(): string {
  switch (Platform.OS) {
    case 'ios':
      return 'IOS';
    case 'android':
      return 'ANDROID';
    case 'web':
      return 'WEB';
    default:
      return 'UNKNOWN';
  }
}

/** The version the stores know about, which is what the version gate compares against. */
function appVersion(): string | undefined {
  return Application.nativeApplicationVersion ?? undefined;
}

export function newRequestId(): string {
  return Crypto.randomUUID();
}

export function contextHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    [HEADER_REQUEST_ID]: newRequestId(),
    [HEADER_CLIENT_PLATFORM]: clientPlatform(),
  };
  const version = appVersion();
  if (version) {
    headers[HEADER_APP_VERSION] = version;
  }
  return headers;
}
