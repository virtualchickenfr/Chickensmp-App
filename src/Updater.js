import { useEffect, useRef, useState } from 'react';
import { Animated, AppState, Linking, Modal, Platform, StyleSheet, Text, View } from 'react-native';
import Constants from 'expo-constants';
import { Ionicons } from '@expo/vector-icons';
import { C } from './content';

const REPO = 'virtualchickenfr/Chickensmp-App';
const LATEST_API = `https://api.github.com/repos/${REPO}/releases/latest`;
const F = { r: 'Poppins_400Regular', b: 'Poppins_700Bold', x: 'Poppins_800ExtraBold' };

export function currentBuild() {
  const cfg = Constants.expoConfig || Constants.manifest || {};
  return Number((cfg.android && cfg.android.versionCode) || 0);
}

export default function Updater({ t, Btn }) {
  const [update, setUpdate] = useState(null);
  const dismissed = useRef(0);
  const scale = useRef(new Animated.Value(0.85)).current;

  const check = async () => {
    if (Platform.OS !== 'android') return;
    try {
      const r = await fetch(LATEST_API, { headers: { Accept: 'application/vnd.github+json' } });
      if (!r.ok) return;
      const d = await r.json();
      const m = /^v1\.0\.(\d+)$/.exec(d.tag_name || '');
      const latest = m ? Number(m[1]) : 0;
      const asset = (d.assets || []).find((a) => a.name === 'ChickenSMP.apk');
      if (latest > currentBuild() && asset && latest !== dismissed.current) {
        setUpdate({ build: latest, version: d.tag_name.slice(1), url: asset.browser_download_url, sizeMb: Math.round(asset.size / 1e6) });
      }
    } catch (e) { /* offline: try again later */ }
  };

  useEffect(() => {
    check();
    const sub = AppState.addEventListener('change', (s) => { if (s === 'active') check(); });
    return () => sub.remove();
  }, []);

  useEffect(() => {
    if (update) Animated.spring(scale, { toValue: 1, useNativeDriver: true, speed: 14, bounciness: 12 }).start();
    else scale.setValue(0.85);
  }, [update, scale]);

  if (!update) return null;
  const later = () => { dismissed.current = update.build; setUpdate(null); };
  const now = () => { Linking.openURL(update.url); later(); };

  return (
    <Modal transparent animationType="fade" visible onRequestClose={later}>
      <View style={st.backdrop}>
        <Animated.View style={[st.card, { transform: [{ scale }] }]}>
          <View style={st.shine} />
          <View style={st.icon}><Ionicons name="sparkles" size={30} color={C.sky} /></View>
          <Text style={st.title}>{t('updTitle')}</Text>
          <Text style={st.text}>{t('updText').replace('{v}', update.version)}</Text>
          <Text style={st.small}>{t('updHow').replace('{mb}', String(update.sizeMb))}</Text>
          <View style={{ alignSelf: 'stretch', gap: 10, marginTop: 18 }}>
            <Btn icon="download" label={t('updNow')} onPress={now} />
            <Btn kind="ghost" label={t('updLater')} onPress={later} />
          </View>
        </Animated.View>
      </View>
    </Modal>
  );
}

const st = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', alignItems: 'center', justifyContent: 'center', padding: 24 },
  card: {
    width: '100%', maxWidth: 380, alignItems: 'center', padding: 24, borderRadius: 26, overflow: 'hidden',
    backgroundColor: 'rgba(36,36,40,0.96)', borderWidth: 1, borderColor: 'rgba(255,255,255,0.16)',
  },
  shine: { position: 'absolute', top: 0, left: 24, right: 24, height: 1, backgroundColor: 'rgba(255,255,255,0.3)' },
  icon: { width: 64, height: 64, borderRadius: 20, backgroundColor: 'rgba(79,182,255,0.18)', alignItems: 'center', justifyContent: 'center', marginBottom: 14 },
  title: { color: '#fff', fontFamily: F.x, fontSize: 22, textAlign: 'center' },
  text: { color: C.soft, fontFamily: F.r, fontSize: 15, textAlign: 'center', marginTop: 6, lineHeight: 22 },
  small: { color: C.muted, fontFamily: F.r, fontSize: 12.5, textAlign: 'center', marginTop: 10, lineHeight: 18 },
});
