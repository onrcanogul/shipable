/**
 * The error contract, mirrored from the backend.
 *
 * Every failure from every endpoint arrives in this shape, which is what lets this app
 * have one error handler instead of one per screen. See `ProblemBody` and `ErrorCodes` on
 * the server: these two files are a pair, and changing one without the other is how a
 * shipped client starts mishandling errors.
 */

/** One rejected input field. Present on validation failures. */
export interface FieldError {
  readonly field: string;
  readonly message: string;
}

/** The JSON body the backend returns for every error. */
export interface ProblemBody {
  readonly status: number;
  readonly code: string;
  readonly message: string;
  readonly requestId?: string;
  readonly fieldErrors?: readonly FieldError[];
}

/**
 * Error codes the backend emits.
 *
 * Branch on these, never on `message` - message text is free to change and may end up
 * localised. Keep this in step with `ErrorCodes.java`.
 */
export const ErrorCodes = {
  // request pipeline
  VALIDATION_FAILED: 'validation_failed',
  MALFORMED_REQUEST: 'malformed_request',
  RATE_LIMITED: 'rate_limited',
  DUPLICATE_REQUEST: 'duplicate_request',
  INTERNAL_ERROR: 'internal_error',

  // auth
  UNAUTHORIZED: 'unauthorized',
  FORBIDDEN: 'forbidden',
  TOKEN_EXPIRED: 'token_expired',
  TOKEN_INVALID: 'token_invalid',

  // resources
  NOT_FOUND: 'not_found',
  CONFLICT: 'conflict',

  // commerce
  QUOTA_EXCEEDED: 'quota_exceeded',
  ENTITLEMENT_REQUIRED: 'entitlement_required',

  // client lifecycle
  APP_VERSION_UNSUPPORTED: 'app_version_unsupported',
  MAINTENANCE_MODE: 'maintenance_mode',
} as const;

export type ErrorCode = (typeof ErrorCodes)[keyof typeof ErrorCodes];

/**
 * Anything the API rejected.
 *
 * Carries the `requestId` because that is what turns a user's screenshot into a log
 * search. Show it on error screens; the support conversation gets much shorter.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly requestId?: string;
  readonly fieldErrors: readonly FieldError[];

  constructor(problem: ProblemBody) {
    super(problem.message);
    this.name = 'ApiError';
    this.status = problem.status;
    this.code = problem.code;
    this.requestId = problem.requestId;
    this.fieldErrors = problem.fieldErrors ?? [];
  }

  is(code: ErrorCode): boolean {
    return this.code === code;
  }

  /** Signing in again will not help; the user needs to update or pay. */
  get isTerminal(): boolean {
    return (
      this.code === ErrorCodes.APP_VERSION_UNSUPPORTED ||
      this.code === ErrorCodes.ENTITLEMENT_REQUIRED ||
      this.code === ErrorCodes.FORBIDDEN
    );
  }
}

/**
 * The network could not be reached at all - no response, so no `ProblemBody`.
 *
 * Kept distinct from {@link ApiError} because the user-facing answer differs: "check your
 * connection" rather than anything about the request.
 */
export class NetworkError extends Error {
  constructor(cause?: unknown) {
    super('Could not reach the server');
    this.name = 'NetworkError';
    this.cause = cause;
  }
}
