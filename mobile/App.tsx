import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Linking, Pressable, StyleSheet, Text, View } from 'react-native';

import { bootstrap, remoteConfig, subscribeToSession, type BootstrapResult } from './src/platform';
import { HomeScreen } from './src/screens/HomeScreen';
import { LoginScreen } from './src/screens/LoginScreen';

/**
 * What the app does at launch, and which screen that leads to.
 *
 * No navigation library: there are four states and one of them is a spinner. Add a router
 * when you have screens to route between — before that it is a dependency that only makes
 * this file longer.
 */

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

// Public SDK keys, which are meant to ship in the app. The secret key stays on the backend.
const REVENUECAT_KEYS = {
  ios: process.env.EXPO_PUBLIC_REVENUECAT_IOS_KEY ?? '',
  android: process.env.EXPO_PUBLIC_REVENUECAT_ANDROID_KEY ?? '',
};

export default function App() {
  const [booted, setBooted] = useState<BootstrapResult | null>(null);
  const [signedIn, setSignedIn] = useState(false);

  useEffect(() => {
    let cancelled = false;

    bootstrap({
      apiBaseUrl: API_BASE_URL,
      revenueCatKeys: REVENUECAT_KEYS,
      // Off, so the login screen is what greets you. Turn it on and the app opens straight
      // into a guest session instead - both are reasonable; this one makes the choice
      // visible to the user.
      signInAnonymouslyOnLaunch: false,
      lifecycle: {
        // Handled here rather than per screen: the server can answer 426 to any call.
        onUpdateRequired: () => setBooted((it) => (it ? { ...it, forceUpdate: true } : it)),
      },
    }).then((result) => {
      if (!cancelled) {
        setBooted(result);
      }
    });

    // The session can end without a screen asking - a refresh token that was revoked, say.
    // Subscribing means the UI follows it wherever it changes.
    const unsubscribe = subscribeToSession((session) => setSignedIn(session !== null));

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, []);

  const handleSignedIn = useCallback(() => setSignedIn(true), []);
  const handleSignedOut = useCallback(() => setSignedIn(false), []);

  if (!booted) {
    return (
      <Centred>
        <ActivityIndicator />
      </Centred>
    );
  }

  if (booted.forceUpdate) {
    const url = remoteConfig().updateUrl;
    return (
      <Centred>
        <Text style={styles.title}>Update required</Text>
        <Text style={styles.body}>This version is no longer supported.</Text>
        {url ? (
          <Pressable onPress={() => Linking.openURL(url)} accessibilityRole="button">
            <Text style={styles.link}>Open the store</Text>
          </Pressable>
        ) : null}
      </Centred>
    );
  }

  if (booted.maintenanceMode) {
    return (
      <Centred>
        <Text style={styles.title}>Back shortly</Text>
        <Text style={styles.body}>{booted.maintenanceMessage ?? 'We are doing some work.'}</Text>
      </Centred>
    );
  }

  return (
    <View style={styles.root}>
      {signedIn ? (
        <HomeScreen onSignedOut={handleSignedOut} />
      ) : (
        <LoginScreen onSignedIn={handleSignedIn} />
      )}
      <StatusBar style="auto" />
    </View>
  );
}

function Centred({ children }: { readonly children: React.ReactNode }) {
  return (
    <View style={[styles.root, styles.centred]}>
      {children}
      <StatusBar style="auto" />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#fff' },
  centred: { alignItems: 'center', justifyContent: 'center', padding: 24, gap: 8 },
  title: { fontSize: 20, fontWeight: '600' },
  body: { fontSize: 14, opacity: 0.7, textAlign: 'center' },
  link: { fontSize: 15, marginTop: 12, textDecorationLine: 'underline' },
});
