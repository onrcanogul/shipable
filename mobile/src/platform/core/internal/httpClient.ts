import { coreConfig } from '../api/config';
import { ApiError, ErrorCodes, NetworkError, ProblemBody } from '../api/errors';
import { getAuthTokenProvider, getLifecycleListener } from '../api/ports';
import { contextHeaders, HEADER_IDEMPOTENCY_KEY, newRequestId } from './requestContext';

/**
 * The one place every API call goes through.
 *
 * Centralising it is what makes the cross-cutting behaviour possible at all: a token
 * refresh that happens once rather than in every screen, an update gate that catches a 426
 * from any endpoint, and an idempotency key on every mutation. Scattering `fetch` calls
 * through the app would mean re-implementing each of those, badly, several times.
 */

const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export interface RequestOptions {
  readonly method?: string;
  readonly body?: unknown;
  /** Send no Authorization header even if a session exists. */
  readonly anonymous?: boolean;
  /**
   * Reuse of this key makes a retry safe. Generated per request by default, which protects
   * against the network layer retrying underneath us but not against the user tapping
   * twice - pass a stable key for that.
   */
  readonly idempotencyKey?: string;
}

/**
 * Refresh is single-flight.
 *
 * When a token expires, every in-flight request fails at once. Without this, each of them
 * would start its own refresh, and all but one would be rotating a token another request
 * had already replaced - which on a backend that revokes the old token on rotation logs
 * the user out.
 */
let refreshInFlight: Promise<boolean> | null = null;

function refreshOnce(): Promise<boolean> {
  const provider = getAuthTokenProvider();
  if (!provider) {
    return Promise.resolve(false);
  }
  if (!refreshInFlight) {
    refreshInFlight = provider.refresh().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await send(path, options);

  // 401 with an expired token is the one case worth retrying: refresh, then go again.
  // A token that is invalid rather than expired will not be fixed by refreshing.
  if (response.status === 401 && !options.anonymous) {
    const problem = await readProblem(response);
    if (problem.code === ErrorCodes.TOKEN_EXPIRED && (await refreshOnce())) {
      const retried = await send(path, options);
      return handle<T>(retried);
    }
    await endSession();
    throw new ApiError(problem);
  }

  return handle<T>(response);
}

async function send(path: string, options: RequestOptions): Promise<Response> {
  const { baseUrl, requestTimeoutMs } = coreConfig();
  const method = options.method ?? 'GET';

  const headers: Record<string, string> = {
    Accept: 'application/json',
    ...contextHeaders(),
  };

  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  if (MUTATING_METHODS.has(method)) {
    headers[HEADER_IDEMPOTENCY_KEY] = options.idempotencyKey ?? newRequestId();
  }

  if (!options.anonymous) {
    const token = await getAuthTokenProvider()?.accessToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  // An explicit timeout, because fetch on React Native has none: a request to a host that
  // silently drops packets otherwise hangs until the OS gives up, which can be minutes.
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), requestTimeoutMs);

  try {
    return await fetch(`${baseUrl}${path}`, {
      method,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: controller.signal,
    });
  } catch (cause) {
    throw new NetworkError(cause);
  } finally {
    clearTimeout(timeout);
  }
}

async function handle<T>(response: Response): Promise<T> {
  if (response.ok) {
    // 204 and friends have no body; asking for JSON would throw.
    if (response.status === 204 || response.headers.get('Content-Length') === '0') {
      return undefined as T;
    }
    return (await response.json()) as T;
  }

  const problem = await readProblem(response);
  notify(problem, response);
  throw new ApiError(problem);
}

function notify(problem: ProblemBody, response: Response): void {
  const listener = getLifecycleListener();

  if (problem.code === ErrorCodes.APP_VERSION_UNSUPPORTED) {
    listener.onUpdateRequired?.();
  }
  if (problem.code === ErrorCodes.RATE_LIMITED) {
    const retryAfter = Number(response.headers.get('Retry-After') ?? '60');
    listener.onRateLimited?.(Number.isFinite(retryAfter) ? retryAfter : 60);
  }
}

async function endSession(): Promise<void> {
  await getAuthTokenProvider()?.onSessionEnded();
  getLifecycleListener().onSessionEnded?.();
}

/**
 * Reads the error body, tolerating one that is not in the expected shape.
 *
 * A proxy or a load balancer can answer before the app is reached - an HTML 502, say - and
 * that must surface as a normal error rather than a JSON parse exception thrown from
 * inside the error path.
 */
async function readProblem(response: Response): Promise<ProblemBody> {
  try {
    const body = (await response.json()) as Partial<ProblemBody>;
    if (typeof body?.code === 'string') {
      return {
        status: body.status ?? response.status,
        code: body.code,
        message: body.message ?? 'Request failed',
        requestId: body.requestId,
        fieldErrors: body.fieldErrors,
      };
    }
  } catch {
    // fall through to the generic shape
  }
  return {
    status: response.status,
    code: ErrorCodes.INTERNAL_ERROR,
    message: `Request failed with status ${response.status}`,
  };
}
