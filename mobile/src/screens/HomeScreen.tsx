import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import {
  currentSession,
  fetchEntitlements,
  noEntitlements,
  signOut,
  type Entitlements,
} from '../platform';

/**
 * What you see once there is a session. A status page, not a product.
 *
 * It exists to show the whole chain working: a token was issued, it authenticates a
 * protected call, and the answer came from the backend rather than from the store SDK.
 * Delete it and build your app.
 */

interface Props {
  readonly onSignedOut: () => void;
}

export function HomeScreen({ onSignedOut }: Props) {
  const session = currentSession();
  const [entitlements, setEntitlements] = useState<Entitlements>(noEntitlements());
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchEntitlements()
      .then((result) => !cancelled && setEntitlements(result))
      .catch((cause) => !cancelled && setError(cause instanceof Error ? cause.message : String(cause)));
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleSignOut() {
    await signOut();
    onSignedOut();
  }

  return (
    <View style={styles.screen}>
      <Text style={styles.title}>Signed in</Text>

      <View style={styles.rows}>
        <Row label="User" value={session?.userId ?? '-'} />
        <Row label="Account" value={session?.anonymous ? 'Guest' : 'Registered'} />
        <Row label="Paying" value={entitlements.paying ? 'Yes' : 'No'} />
      </View>

      {session?.anonymous ? (
        <Text style={styles.hint}>
          A guest account lives on this device. Signing in later keeps everything — the
          backend links it rather than starting over.
        </Text>
      ) : null}

      {error ? <Text style={styles.error}>{error}</Text> : null}

      <Pressable onPress={handleSignOut} accessibilityRole="button" style={styles.signOut}>
        <Text style={styles.signOutText}>Sign out</Text>
      </Pressable>
    </View>
  );
}

function Row({ label, value }: { readonly label: string; readonly value: string }) {
  return (
    <View style={styles.row}>
      <Text style={styles.rowLabel}>{label}</Text>
      <Text style={styles.rowValue} numberOfLines={1} ellipsizeMode="middle">
        {value}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, justifyContent: 'center', paddingHorizontal: 24, gap: 24 },
  title: { fontSize: 28, fontWeight: '700' },
  rows: { gap: 12 },
  row: { flexDirection: 'row', justifyContent: 'space-between', gap: 16 },
  rowLabel: { fontSize: 15, opacity: 0.5 },
  rowValue: { fontSize: 15, fontWeight: '500', flexShrink: 1 },
  hint: { fontSize: 13, opacity: 0.6, lineHeight: 19 },
  error: { color: '#b00020', fontSize: 13 },
  signOut: { marginTop: 8 },
  signOutText: { fontSize: 15, color: '#b00020', fontWeight: '600' },
});
