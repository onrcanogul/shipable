/** Mirrors RemoteConfigResponse on the server. */
export interface RemoteConfig {
  readonly minimumSupportedVersion: string;
  readonly latestVersion: string;
  readonly updateUrl: string | null;
  /** True when this build is below the minimum. Show a blocking screen. */
  readonly forceUpdate: boolean;
  readonly maintenanceMode: boolean;
  readonly maintenanceMessage: string | null;
  readonly featureFlags: Readonly<Record<string, boolean>>;
}
