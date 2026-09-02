/**
 * Everything identity hands to the rest of the app.
 *
 * Mirrors the backend module: obtaining a session, keeping it fresh, ending it. Other
 * modules read `currentSession()`; nothing outside imports from `internal/`.
 */
export type { AuthProvider, AuthenticationResponse, Session } from './api/types';
export {
  currentSession,
  installAuthTokenProvider,
  linkAccount,
  restoreSession,
  signInAnonymously,
  signInWithProvider,
  signOut,
  subscribe as subscribeToSession,
} from './internal/authService';
export { deviceId } from './internal/deviceId';
export {
  accountDeletionStatus,
  cancelAccountDeletion,
  exportAccountData,
  requestAccountDeletion,
  type DeletionStatus,
} from './internal/accountService';
