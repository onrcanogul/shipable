import { useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
  type StyleProp,
  type ViewStyle,
} from 'react-native';

import { ApiError, NetworkError, signInAnonymously } from '../platform';

/**
 * The first screen. Deliberately plain — restyle it, do not rebuild it.
 *
 * It lives in the app rather than in `platform/` for the same reason the backend's
 * `platform` modules ship no screens: the platform knows how to obtain a session, not what
 * signing in should look like in your product.
 *
 * "Continue as guest" is first on purpose. Making people create an account before they have
 * seen anything is the most reliable way to lose them, and the backend supports linking a
 * real account to an anonymous one later without losing their data.
 */

interface Props {
  readonly onSignedIn: () => void;
}

export function LoginScreen({ onSignedIn }: Props) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [requestId, setRequestId] = useState<string | null>(null);

  async function continueAsGuest() {
    setBusy(true);
    setError(null);
    setRequestId(null);
    try {
      await signInAnonymously();
      onSignedIn();
    } catch (cause) {
      // ApiError carries a code and a request id; NetworkError means we never got a
      // response at all. The user-facing answer differs, so they are told apart.
      if (cause instanceof ApiError) {
        setError(cause.message);
        setRequestId(cause.requestId ?? null);
      } else if (cause instanceof NetworkError) {
        setError('Could not reach the server. Check your connection and try again.');
      } else {
        setError('Something went wrong.');
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <View style={styles.screen}>
      <View style={styles.header}>
        <Text style={styles.title}>My App</Text>
        <Text style={styles.subtitle}>Start using it, sign in whenever you like.</Text>
      </View>

      <View style={styles.actions}>
        <Button label="Continue as guest" onPress={continueAsGuest} busy={busy} primary />

        {/*
          Deferred rather than hidden, so the shape of the finished screen is visible. The
          backend's Apple and Google verifiers are still TODO; wiring the buttons before
          then would produce a button that fails on tap, which is worse than one that says
          why.
        */}
        <Button label="Continue with Apple" disabled />
        <Button label="Continue with Google" disabled />
        <Text style={styles.note}>
          Provider sign-in needs the backend's Apple and Google token verifiers first.
        </Text>
      </View>

      {error ? (
        <View style={styles.error}>
          <Text style={styles.errorText}>{error}</Text>
          {requestId ? <Text style={styles.requestId}>Reference: {requestId}</Text> : null}
        </View>
      ) : null}
    </View>
  );
}

interface ButtonProps {
  readonly label: string;
  readonly onPress?: () => void;
  readonly busy?: boolean;
  readonly primary?: boolean;
  readonly disabled?: boolean;
}

function Button({ label, onPress, busy, primary, disabled }: ButtonProps) {
  const style: StyleProp<ViewStyle> = [
    styles.button,
    primary ? styles.buttonPrimary : styles.buttonSecondary,
    disabled || busy ? styles.buttonDisabled : null,
  ];

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || busy}
      accessibilityRole="button"
      accessibilityState={{ disabled: disabled || busy, busy }}
      style={style}
    >
      {busy ? (
        <ActivityIndicator color={primary ? '#fff' : '#111'} />
      ) : (
        <Text style={primary ? styles.buttonPrimaryText : styles.buttonSecondaryText}>{label}</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, justifyContent: 'center', paddingHorizontal: 24, gap: 40 },
  header: { gap: 8 },
  title: { fontSize: 32, fontWeight: '700' },
  subtitle: { fontSize: 16, opacity: 0.6 },
  actions: { gap: 12 },
  button: {
    height: 52,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonPrimary: { backgroundColor: '#111' },
  buttonSecondary: { borderWidth: 1, borderColor: '#d5d5d5' },
  buttonDisabled: { opacity: 0.4 },
  buttonPrimaryText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  buttonSecondaryText: { color: '#111', fontSize: 16, fontWeight: '600' },
  note: { fontSize: 12, opacity: 0.5, textAlign: 'center', marginTop: 4 },
  error: { gap: 4 },
  errorText: { color: '#b00020', fontSize: 14 },
  // Shown because it is what turns a screenshot into a log search.
  requestId: { fontSize: 11, opacity: 0.5 },
});
