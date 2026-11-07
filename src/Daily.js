import { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Animated, StyleSheet, Text, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import { C } from './content';
import { API } from './Profile';

const F = { r: 'Poppins_400Regular', m: 'Poppins_500Medium', b: 'Poppins_700Bold', x: 'Poppins_800ExtraBold' };
const KEY_COLOR = { basic: '#b8b8b8', plus: C.green, core: C.sky, supreme: C.gold, crimson: '#e0364a', cherry: '#ff8fc8' };
const cap = (s) => s.charAt(0).toUpperCase() + s.slice(1);

function untilMidnightBerlin() {
  const now = new Date();
  const berlin = new Date(now.toLocaleString('en-US', { timeZone: 'Europe/Berlin' }));
  const next = new Date(berlin); next.setHours(24, 0, 0, 0);
  const m = Math.max(0, Math.round((next - berlin) / 60000));
  return `${Math.floor(m / 60)}h ${String(m % 60).padStart(2, '0')}m`;
}

export default function DailyReward({ ui, go, compact }) {
  const { t, toast, Glass, Btn } = ui;
  const [token, setToken] = useState(undefined);
  const [d, setD] = useState(null);
  const [busy, setBusy] = useState(false);
  const pop = useRef(new Animated.Value(1)).current;

  const load = useCallback(async (tok) => {
    try {
      const r = await fetch(`${API}?action=daily`, { headers: { Authorization: `Bearer ${tok}` } });
      if (r.status === 401) { setToken(null); return; }
      if (r.ok) { const j = await r.json(); if (Array.isArray(j.rewards)) setD(j); }
    } catch (e) { /* offline */ }
  }, []);

  useEffect(() => {
    AsyncStorage.getItem('csmp-token').then((tok) => { setToken(tok || null); if (tok) load(tok); }).catch(() => setToken(null));
  }, [load]);

  const claim = async () => {
    setBusy(true);
    try {
      const r = await fetch(`${API}?action=daily`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } });
      const j = await r.json();
      if (r.ok) {
        Animated.sequence([
          Animated.spring(pop, { toValue: 1.12, useNativeDriver: true, speed: 30, bounciness: 14 }),
          Animated.spring(pop, { toValue: 1, useNativeDriver: true, speed: 14, bounciness: 10 }),
        ]).start();
        toast(`${t('dlGot')} ${j.reward.amount}x ${cap(j.reward.key)} Key`);
      }
      await load(token);
    } catch (e) { toast(t('noConnection')); }
    setBusy(false);
  };

  if (token === undefined) return null;
  if (!token) {
    if (compact) return null;
    return (
      <Glass style={{ padding: 18, marginTop: 14, flexDirection: 'row', alignItems: 'center', gap: 14 }}>
        <View style={[st.gift, { backgroundColor: 'rgba(245,184,46,0.18)' }]}><Ionicons name="gift" size={24} color={C.gold} /></View>
        <View style={{ flex: 1 }}>
          <Text style={st.title}>{t('dlTitle').toUpperCase()}</Text>
          <Text style={st.sub}>{t('dlLoginFirst')}</Text>
        </View>
        <Btn small label={t('login')} onPress={() => go('profile')} />
      </Glass>
    );
  }
  if (!d) return <Glass style={{ padding: 18, marginTop: 14 }}><ActivityIndicator color="#fff" /></Glass>;

  return (
    <Animated.View style={{ transform: [{ scale: pop }] }}>
      <Glass style={{ padding: 18, marginTop: 14 }} accent={d.claimedToday ? null : 'rgba(245,184,46,0.6)'}>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 14 }}>
          <View style={[st.gift, { backgroundColor: 'rgba(245,184,46,0.18)' }]}><Ionicons name="gift" size={24} color={C.gold} /></View>
          <View style={{ flex: 1 }}>
            <Text style={st.title}>{t('dlTitle').toUpperCase()}</Text>
            <Text style={st.sub}>{t('dlStreak').replace('{n}', String(d.streak))}</Text>
          </View>
          <View style={st.flame}><Ionicons name="flame" size={14} color="#fff" /><Text style={st.flameText}>{d.streak}</Text></View>
        </View>
        <View style={st.days}>
          {d.rewards.map((r, i) => {
            const done = d.claimedToday ? i <= d.nextIndex : i < d.nextIndex;
            const next = !d.claimedToday && i === d.nextIndex;
            const col = KEY_COLOR[r.key] || C.sky;
            return (
              <View key={i} style={[st.day, done && { backgroundColor: col + '22', borderColor: col + '66' }, next && { borderColor: C.gold, backgroundColor: 'rgba(245,184,46,0.16)' }]}>
                <Text style={st.dayLabel}>{t('dlDay')} {i + 1}</Text>
                <Ionicons name={done ? 'checkmark-circle' : 'key'} size={20} color={done ? col : next ? C.gold : col} />
                <Text style={[st.dayAmount, { color: col }]}>{r.amount}x</Text>
              </View>
            );
          })}
        </View>
        {d.claimedToday ? (
          <View style={st.doneBox}>
            <Ionicons name="time-outline" size={16} color={C.muted} />
            <Text style={st.doneText}>{t('dlCome')} {untilMidnightBerlin()}</Text>
          </View>
        ) : (
          <View style={{ marginTop: 14 }}>
            <Btn icon={busy ? null : 'gift'} label={busy ? '…' : `${t('dlClaim')} · ${d.rewards[d.nextIndex].amount}x ${cap(d.rewards[d.nextIndex].key)} Key`} onPress={busy ? undefined : claim} />
          </View>
        )}
        {d.undelivered > 0 ? <Text style={st.note}>{t('dlDeliver')}</Text> : null}
      </Glass>
    </Animated.View>
  );
}

const st = StyleSheet.create({
  gift: { width: 46, height: 46, borderRadius: 14, alignItems: 'center', justifyContent: 'center' },
  title: { color: '#fff', fontFamily: F.x, fontSize: 16 },
  sub: { color: C.muted, fontFamily: F.m, fontSize: 12.5 },
  flame: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 10, paddingVertical: 5, borderRadius: 999, backgroundColor: '#ff6a24' },
  flameText: { color: '#fff', fontFamily: F.x, fontSize: 13 },
  days: { flexDirection: 'row', gap: 6 },
  day: { flex: 1, alignItems: 'center', gap: 4, paddingVertical: 10, borderRadius: 12, borderWidth: 1, borderColor: 'rgba(255,255,255,0.1)', backgroundColor: 'rgba(20,20,20,0.45)' },
  dayLabel: { color: C.muted, fontFamily: F.b, fontSize: 9 },
  dayAmount: { fontFamily: F.x, fontSize: 11 },
  doneBox: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 6, marginTop: 14, paddingVertical: 12, borderRadius: 14, backgroundColor: 'rgba(255,255,255,0.05)' },
  doneText: { color: C.soft, fontFamily: F.b, fontSize: 13.5 },
  note: { color: C.muted, fontFamily: F.r, fontSize: 12, textAlign: 'center', marginTop: 10 },
});
