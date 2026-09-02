import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { request } from '../../core';
import { deviceId } from '../../identity';
import type { DeviceRegistration, PushPlatform } from '../api/types';

/**
 * Registers this device for push, and retires it on sign-out.
 *
 * Two rules the server relies on:
 *
 * - **Register on every launch, not just the first.** Push tokens rotate; a token stored
 *   once goes stale and the user quietly stops receiving anything.
 * - **Unregister on sign-out**, so the next person to use the phone does not get their
 *   notifications.
 */

function pushPlatform(): PushPlatform {
  return Platform.OS === 'ios' ? 'APNS' : 'FCM';
}

/**
 * @returns the token, or null when the user declined - which is a normal outcome, not an
 *          error. Never prompt again automatically; iOS only asks once anyway.
 */
export async function requestPushPermission(): Promise<string | null> {
  const existing = await Notifications.getPermissionsAsync();
  const granted =
    existing.granted || (await Notifications.requestPermissionsAsync()).granted;

  if (!granted) {
    return null;
  }
  const token = await Notifications.getDevicePushTokenAsync();
  return String(token.data);
}

export async function registerForPush(): Promise<boolean> {
  const token = await requestPushPermission();
  if (!token) {
    return false;
  }

  const registration: DeviceRegistration = {
    deviceId: await deviceId(),
    token,
    platform: pushPlatform(),
  };

  await request<void>('/api/v1/devices', { method: 'POST', body: registration });
  return true;
}

export async function unregisterFromPush(): Promise<void> {
  const id = await deviceId();
  try {
    await request<void>(`/api/v1/devices/${id}`, { method: 'DELETE' });
  } catch {
    // Sign-out must not fail because the device could not be deregistered. The server
    // invalidates dead tokens when a send bounces.
  }
}
