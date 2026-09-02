/**
 * Everything core hands to the rest of the app.
 *
 * The mirror of the backend's `core`: the request pipeline, the error contract, and the
 * ports other modules plug into. Nothing outside this module imports from `internal/`.
 */
export { configureCore, type CoreConfig } from './api/config';
export {
  ApiError,
  ErrorCodes,
  NetworkError,
  type ErrorCode,
  type FieldError,
  type ProblemBody,
} from './api/errors';
export {
  setAuthTokenProvider,
  setLifecycleListener,
  type AuthTokenProvider,
  type LifecycleListener,
} from './api/ports';
export { request, type RequestOptions } from './internal/httpClient';
