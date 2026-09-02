/** Mirrors RegisterDeviceRequest on the server. */
export type PushPlatform = 'APNS' | 'FCM';

export interface DeviceRegistration {
  readonly deviceId: string;
  readonly token: string;
  readonly platform: PushPlatform;
  readonly locale?: string;
}
