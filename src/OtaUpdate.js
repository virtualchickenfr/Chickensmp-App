// Over-the-air updates: the app downloads new code itself when opened – no new APK needed.
import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, AppState, Platform, StyleSheet, Text, View } from 'react-native';
import * as Updates from 'expo-updates';
import { C } from './content';

export default function OtaUpdate({ t }) {
  const [busy, setBusy] = useState(false);
  const running = useRef(false);

  const run = async (foreground) => {
    if (Platform.OS === 'web' || __DEV__ || !Updates.isEnabled || running.current) return;
    running.current = true;
    try {
      const res = await Updates.checkForUpdateAsync();
      if (res.isAvailable) {
        if (foreground) setBusy(true);
        await Updates.fetchUpdateAsync();
        await Updates.reloadAsync();
      }
    } catch (e) {
      setBusy(false);
    }
    running.current = false;
  };

  useEffect(() => {
    run(true);
    const sub = AppState.addEventListener('change', (s) => { if (s === 'active') run(true); });
    return () => sub.remove();
  }, []);

  if (!busy) return null;
  return (
    <View style={st.overlay}>
      <View style={st.card}>
        <ActivityIndicator color={C.sky} size="large" />
        <Text style={st.title}>{t('otaTitle')}</Text>
        <Text style={st.text}>{t('otaText')}</Text>
      </View>
    </View>
  );
}

const st = StyleSheet.create({
  overlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(10,10,12,0.85)', alignItems: 'center', justifyContent: 'center' },
  card: { alignItems: 'center', padding: 28, borderRadius: 24, backgroundColor: 'rgba(40,40,44,0.95)', borderWidth: 1, borderColor: 'rgba(255,255,255,0.14)', gap: 10, width: 280 },
  title: { color: '#fff', fontFamily: 'Poppins_800ExtraBold', fontSize: 18 },
  text: { color: C.soft, fontFamily: 'Poppins_400Regular', fontSize: 13.5, textAlign: 'center' },
});
