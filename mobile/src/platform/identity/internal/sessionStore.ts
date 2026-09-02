import * as SecureStore from 'expo-secure-store';
import type { Session } from '../api/types';

/**
 * Where the session lives on the device.
 *
 * SecureStore, not AsyncStorage: the refresh token is a long-lived credential. AsyncStorage
 * is a plain file, readable by anything that gets at the app's sandbox - a rooted device, a
 * backup, a forensic tool. SecureStore is the Keychain on iOS and Keystore-backed on
 * Android.
 *
 * `WHEN_UNLOCKED_THIS_DEVICE_ONLY` keeps the token out of iCloud Keychain backups. Restoring
 * a backup onto a second phone should not restore a live session with it.
 */
const KEY = 'session';

const OPTIONS: SecureStore.SecureStoreOptions = {
  keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
};

export async function loadSession(): Promise<Session | null> {
  try {
    const raw = await SecureStore.getItemAsync(KEY, OPTIONS);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    // A store that cannot be read is the same as no session: the user signs in again.
    // Throwing here would brick the app on a device with a damaged keychain entry.
    return null;
  }
}

export async function saveSession(session: Session): Promise<void> {
  await SecureStore.setItemAsync(KEY, JSON.stringify(session), OPTIONS);
}

export async function clearSession(): Promise<void> {
  await SecureStore.deleteItemAsync(KEY, OPTIONS);
}
