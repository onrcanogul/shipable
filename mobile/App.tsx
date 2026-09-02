import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { ActivityIndicator, Linking, Pressable, StyleSheet, Text, View } from 'react-native';

import {
  bootstrap,
  currentSession,
  fetchEntitlements,
  noEntitlements,
  remoteConfig,
  type BootstrapResult,
  type Entitlements,
} from './src/platform';

/**
 * A shell, not a design.
 *
 * It exists to prove the platform layer is wired: config fetched, session restored or
 * created, entitlements read from the backend. Replace it with your app — the interesting
 * part is `bootstrap()` and what it hands back.
 */

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

// Public SDK keys, which are meant to ship in the app. The secret key stays on the backend.
const REVENUECAT_KEYS = {
  ios: process.env.EXPO_PUBLIC_REVENUECAT_IOS_KEY ?? '',
  android: process.env.EXPO_PUBLIC_REVENUECAT_ANDROID_KEY ?? '',
};

export default function App() {
  const [result, setResult] = useState<BootstrapResult | null>(null);
  const [entitlements, setEntitlements] = useState<Entitlements>(noEntitlements());
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    bootstrap({
      apiBaseUrl: API_BASE_URL,
      revenueCatKeys: REVENUECAT_KEYS,
      signInAnonymouslyOnLaunch: true,
      lifecycle: {
        // Handled here rather than per screen: the server can answer 426 to any call.
        onUpdateRequired: () => setResult((it) => (it ? { ...it, forceUpdate: true } : it)),
        onSessionEnded: () => setResult((it) => (it ? { ...it, signedIn: false } : it)),
      },
    }).then(async (booted) => {
      if (cancelled) {
        return;
      }
      setResult(booted);

      if (booted.signedIn) {
        try {
          setEntitlements(await fetchEntitlements());
        } catch (cause) {
          // Expected until the backend's billing integration is implemented.
          setError(cause instanceof Error ? cause.message : String(cause));
        }
      }
    });

    return () => {
      cancelled = true;
    };
  }, []);

  if (!result) {
    return (
      <View style={styles.centred}>
        <ActivityIndicator />
        <StatusBar style="auto" />
      </View>
    );
  }

  if (result.forceUpdate) {
    const url = remoteConfig().updateUrl;
    return (
      <View style={styles.centred}>
        <Text style={styles.title}>Update required</Text>
        <Text style={styles.body}>This version is no longer supported.</Text>
        {url ? (
          <Pressable onPress={() => Linking.openURL(url)}>
            <Text style={styles.link}>Open the store</Text>
          </Pressable>
        ) : null}
      </View>
    );
  }

  if (result.maintenanceMode) {
    return (
      <View style={styles.centred}>
        <Text style={styles.title}>Back shortly</Text>
        <Text style={styles.body}>{result.maintenanceMessage ?? 'We are doing some work.'}</Text>
      </View>
    );
  }

  return (
    <View style={styles.centred}>
      <Text style={styles.title}>Platform is up</Text>
      <Text style={styles.body}>API: {API_BASE_URL}</Text>
      <Text style={styles.body}>
        Session: {result.signedIn ? (currentSession()?.userId ?? '-') : 'signed out'}
      </Text>
      <Text style={styles.body}>Paying: {entitlements.paying ? 'yes' : 'no'}</Text>
      {error ? <Text style={styles.error}>{error}</Text> : null}
      <StatusBar style="auto" />
    </View>
  );
}

const styles = StyleSheet.create({
  centred: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24, gap: 8 },
  title: { fontSize: 20, fontWeight: '600' },
  body: { fontSize: 14, opacity: 0.7, textAlign: 'center' },
  link: { fontSize: 15, marginTop: 12, textDecorationLine: 'underline' },
  error: { fontSize: 12, opacity: 0.6, textAlign: 'center', marginTop: 12 },
});
