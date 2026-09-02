/**
 * How the client is configured. Set once, at startup.
 *
 * Mirrors the split on the server: this is boot configuration, not something that changes
 * while the app runs. Remote config that *can* change lives in the `appconfig` module and
 * comes from the API.
 */
export interface CoreConfig {
  /** Base URL of the API, without a trailing slash. */
  readonly baseUrl: string;
  /** How long to wait for a response before giving up. */
  readonly requestTimeoutMs: number;
}

let current: CoreConfig | null = null;

export function configureCore(config: CoreConfig): void {
  current = {
    ...config,
    baseUrl: config.baseUrl.replace(/\/+$/, ''),
  };
}

export function coreConfig(): CoreConfig {
  if (!current) {
    // Loud on purpose. A request firing before configuration would silently hit the wrong
    // host, which is far harder to notice than a crash on the first call.
    throw new Error('configureCore() must be called before the first API call');
  }
  return current;
}
