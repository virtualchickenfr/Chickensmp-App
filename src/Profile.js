import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Image, Linking, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import { C, DISCORD } from './content';

export const API = 'https://store.chickensmp.net/api/app';
const TOKEN_KEY = 'csmp-token';

const F = { r: 'Poppins_400Regular', m: 'Poppins_500Medium', b: 'Poppins_700Bold', x: 'Poppins_800ExtraBold' };

function fmtHours(ticks) {
  const h = (ticks || 0) / 20 / 3600;
  return h >= 10 ? `${Math.round(h)} h` : `${h.toFixed(1)} h`;
}
const fmtNum = (n) => (n == null ? '–' : Math.round(n).toLocaleString('de-DE'));
const fmtDate = (ms, lang) => (ms ? new Date(ms).toLocaleDateString(lang === 'de' ? 'de-DE' : 'en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : '–');

function ago(ms, t) {
  if (!ms) return '–';
  const m = Math.round((Date.now() - ms) / 60000);
  if (m < 2) return t('justNow');
  if (m < 60) return `${m} min`;
  const h = Math.round(m / 60);
  if (h < 48) return `${h} h`;
  return `${Math.round(h / 24)} d`;
}

export default function ProfileScreen({ ui }) {
  const { t, lang, toast, Glass, Btn, Segmented, PageHead } = ui;
  const [token, setToken] = useState(null);
  const [ready, setReady] = useState(false);
  const [me, setMe] = useState(null);
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  const [board, setBoard] = useState(null);
  const [boardKey, setBoardKey] = useState('playtime');

  const loadMe = useCallback(async (tok) => {
    try {
      const r = await fetch(`${API}?action=me`, { headers: { Authorization: `Bearer ${tok}` } });
      if (r.status === 401) { await AsyncStorage.removeItem(TOKEN_KEY); setToken(null); setMe(null); return; }
      setMe(await r.json());
    } catch (e) { /* offline: keep old */ }
  }, []);

  useEffect(() => {
    AsyncStorage.getItem(TOKEN_KEY).then((tok) => {
      if (tok) { setToken(tok); loadMe(tok); }
      setReady(true);
    }).catch(() => setReady(true));
    fetch(`${API}?action=leaderboard`).then((r) => r.json()).then(setBoard).catch(() => {});
  }, [loadMe]);

  const login = async () => {
    const c = code.toUpperCase().replace(/[^A-Z0-9]/g, '');
    if (c.length < 8) { setErr(t('codeShort')); return; }
    setBusy(true); setErr('');
    try {
      const r = await fetch(`${API}?action=login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ code: c }) });
      const d = await r.json();
      if (!r.ok || !d.token) { setErr(t('codeInvalid')); setBusy(false); return; }
      await AsyncStorage.setItem(TOKEN_KEY, d.token);
      setToken(d.token); setCode('');
      await loadMe(d.token);
      toast(t('loggedIn'));
    } catch (e) {
      setErr(t('noConnection'));
    }
    setBusy(false);
  };

  const logout = async () => {
    const tok = token;
    await AsyncStorage.removeItem(TOKEN_KEY);
    setToken(null); setMe(null);
    toast(t('loggedOut'));
    fetch(`${API}?action=logout`, { method: 'POST', headers: { Authorization: `Bearer ${tok}` } }).catch(() => {});
  };

  if (!ready) return <ActivityIndicator color="#fff" style={{ marginTop: 40 }} />;

  const s = me && me.stats;
  const kd = s ? (s.deaths ? (s.playerKills / s.deaths).toFixed(2) : String(s.playerKills || 0)) : '–';
  const items = s ? [
    { icon: 'time', color: C.sky, value: fmtHours(s.playtime), label: t('stPlaytime') },
    { icon: 'flash', color: C.red, value: fmtNum(s.playerKills), label: t('stKills') },
    { icon: 'bug', color: C.green, value: fmtNum(s.mobKills), label: t('stMobKills') },
    { icon: 'skull', color: C.muted, value: fmtNum(s.deaths), label: t('stDeaths') },
    { icon: 'stats-chart', color: C.purple, value: kd, label: 'K/D' },
    { icon: 'walk', color: C.gold, value: `${fmtNum((s.walkCm || 0) / 100000)} km`, label: t('stDistance') },
    { icon: 'arrow-up-circle', color: C.sky, value: fmtNum(s.jumps), label: t('stJumps') },
    { icon: 'log-in', color: C.green, value: fmtNum(s.joins), label: t('stJoins') },
    ...(s.balance != null ? [{ icon: 'cash', color: C.gold, value: fmtNum(s.balance), label: t('stBalance') }] : []),
  ] : [];

  const list = board && board[boardKey] ? board[boardKey] : [];
  const fmtBoard = (v) => (boardKey === 'playtime' ? fmtHours(v) : fmtNum(v));

  return (
    <View>
      {!token ? (
        <>
          <PageHead eyebrow="ChickenSMP" title={t('profileTitle').toUpperCase()} intro={t('profileIntro')} />
          <Glass style={{ padding: 20 }}>
            <View style={{ gap: 12 }}>
              {[t('l1'), t('l2'), t('l3')].map((x, i) => (
                <View key={i} style={{ flexDirection: 'row', gap: 12, alignItems: 'center' }}>
                  <View style={st.num}><Text style={st.numText}>{i + 1}</Text></View>
                  <Text style={st.step}>{x}</Text>
                </View>
              ))}
            </View>
            <TextInput
              value={code}
              onChangeText={(v) => { setCode(v.toUpperCase()); setErr(''); }}
              placeholder="XXXX-XXXX"
              placeholderTextColor="rgba(255,255,255,0.3)"
              autoCapitalize="characters"
              autoCorrect={false}
              maxLength={9}
              style={[st.input, err ? { borderColor: C.red } : null]}
              onSubmitEditing={login}
              returnKeyType="go"
            />
            {err ? <Text style={st.err}>{err}</Text> : null}
            <Btn icon={busy ? null : 'log-in'} label={busy ? '…' : t('login')} onPress={login} />
            <Text style={st.hint}>{t('loginHint')}</Text>
          </Glass>
        </>
      ) : (
        <>
          <Glass style={{ padding: 20, alignItems: 'center' }}>
            <Image source={{ uri: `https://mc-heads.net/body/${me ? me.uuid : 'MHF_Steve'}/140` }} style={st.body} resizeMode="contain" />
            <Text style={st.name}>{me ? me.name : '…'}</Text>
            <View style={{ flexDirection: 'row', gap: 8, marginTop: 8, flexWrap: 'wrap', justifyContent: 'center' }}>
              {s && s.rank ? <View style={[st.pill, { backgroundColor: 'rgba(245,184,46,0.18)' }]}><Text style={[st.pillText, { color: C.gold }]}>{s.rank.toUpperCase()}</Text></View> : null}
              {s && s.bedrock ? <View style={[st.pill, { backgroundColor: 'rgba(61,220,132,0.15)' }]}><Text style={[st.pillText, { color: C.green }]}>BEDROCK</Text></View> : null}
              <View style={[st.pill, { backgroundColor: s && s.online ? 'rgba(61,220,132,0.15)' : 'rgba(255,255,255,0.08)' }]}>
                <Text style={[st.pillText, { color: s && s.online ? C.green : C.muted }]}>
                  {s && s.online ? t('onlineNowCap') : `${t('lastSeen')} ${ago(s && s.lastSeen, t)}`}
                </Text>
              </View>
            </View>
            {s ? <Text style={st.since}>{t('memberSince')} {fmtDate(s.firstPlayed, lang)}</Text> : null}
          </Glass>

          {s ? (
            <View style={st.grid}>
              {items.map((it) => (
                <Glass key={it.label} style={st.stat}>
                  <View style={[st.statIcon, { backgroundColor: it.color + '26' }]}><Ionicons name={it.icon} size={18} color={it.color} /></View>
                  <Text style={st.statValue} numberOfLines={1} adjustsFontSizeToFit>{it.value}</Text>
                  <Text style={st.statLabel} numberOfLines={1}>{it.label.toUpperCase()}</Text>
                </Glass>
              ))}
            </View>
          ) : (
            <Glass style={{ padding: 18, marginTop: 14 }}><Text style={st.hint}>{t('noStatsYet')}</Text></Glass>
          )}
        </>
      )}

      <Text style={st.section}>{t('leaderboard').toUpperCase()}</Text>
      <Segmented
        items={[{ id: 'playtime', label: t('stPlaytime') }, { id: 'playerKills', label: t('stKills') }, { id: 'mobKills', label: t('stMobs') }]}
        value={boardKey}
        onChange={setBoardKey}
      />
      <Glass style={{ marginTop: 12, paddingVertical: 6 }}>
        {!board ? <ActivityIndicator color="#fff" style={{ margin: 18 }} /> : list.length === 0 ? (
          <Text style={[st.hint, { margin: 16 }]}>{t('boardEmpty')}</Text>
        ) : list.map((p, i) => (
          <View key={p.uuid} style={[st.row, me && me.uuid === p.uuid && { backgroundColor: 'rgba(59,123,255,0.16)' }]}>
            <Text style={[st.rank, i < 3 && { color: [C.gold, '#d0d0d0', '#cd7f32'][i] }]}>{i + 1}</Text>
            <Image source={{ uri: `https://mc-heads.net/avatar/${p.uuid}/40` }} style={st.head} />
            <Text style={st.rowName} numberOfLines={1}>{p.name}</Text>
            <Text style={st.rowValue}>{fmtBoard(p.value)}</Text>
          </View>
        ))}
      </Glass>

      {token ? (
        <View style={{ marginTop: 18 }}>
          <Btn kind="ghost" icon="log-out-outline" label={t('logout')} onPress={logout} />
        </View>
      ) : (
        <Pressable onPress={() => Linking.openURL(DISCORD)} style={{ marginTop: 16, alignItems: 'center' }}>
          <Text style={{ color: C.sky, fontFamily: F.b, fontSize: 13 }}>{t('loginHelp')}</Text>
        </Pressable>
      )}
    </View>
  );
}

const st = StyleSheet.create({
  num: { width: 28, height: 28, borderRadius: 9, backgroundColor: 'rgba(255,255,255,0.1)', alignItems: 'center', justifyContent: 'center' },
  numText: { color: '#fff', fontFamily: F.x, fontSize: 13 },
  step: { flex: 1, color: C.soft, fontFamily: F.r, fontSize: 15 },
  input: {
    marginTop: 18, marginBottom: 10, height: 60, borderRadius: 16, borderWidth: 1, borderColor: 'rgba(255,255,255,0.16)',
    backgroundColor: 'rgba(20,20,20,0.6)', color: '#fff', fontFamily: F.x, fontSize: 26, letterSpacing: 4, textAlign: 'center',
  },
  err: { color: C.red, fontFamily: F.b, fontSize: 13, textAlign: 'center', marginBottom: 10 },
  hint: { color: C.muted, fontFamily: F.r, fontSize: 13, textAlign: 'center', marginTop: 12, lineHeight: 19 },
  body: { width: 120, height: 190, marginBottom: 8 },
  name: { color: '#fff', fontFamily: F.x, fontSize: 24 },
  pill: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 999 },
  pillText: { fontFamily: F.x, fontSize: 11, letterSpacing: 0.4 },
  since: { color: C.muted, fontFamily: F.m, fontSize: 12.5, marginTop: 10 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10, marginTop: 14 },
  stat: { width: '31.4%', alignItems: 'center', paddingVertical: 14, paddingHorizontal: 6, borderRadius: 16 },
  statIcon: { width: 34, height: 34, borderRadius: 10, alignItems: 'center', justifyContent: 'center', marginBottom: 6 },
  statValue: { color: '#fff', fontFamily: F.x, fontSize: 16 },
  statLabel: { color: C.muted, fontFamily: F.b, fontSize: 9.5, letterSpacing: 0.3, marginTop: 1 },
  section: { color: '#fff', fontFamily: F.x, fontSize: 22, textAlign: 'center', marginTop: 26, marginBottom: 12 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 9, paddingHorizontal: 14, borderRadius: 12, marginHorizontal: 6 },
  rank: { width: 22, color: C.muted, fontFamily: F.x, fontSize: 15, textAlign: 'center' },
  head: { width: 30, height: 30, borderRadius: 7 },
  rowName: { flex: 1, color: '#fff', fontFamily: F.b, fontSize: 15 },
  rowValue: { color: C.sky, fontFamily: F.x, fontSize: 14 },
});
