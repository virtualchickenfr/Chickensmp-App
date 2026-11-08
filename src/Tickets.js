import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import { C } from './content';
import { API } from './Profile';

const F = { r: 'Poppins_400Regular', m: 'Poppins_500Medium', b: 'Poppins_700Bold', x: 'Poppins_800ExtraBold' };

export const CATEGORIES = [
  { id: 'support', icon: 'help-buoy', color: C.blue },
  { id: 'bug', icon: 'bug', color: C.gold },
  { id: 'report', icon: 'flag', color: C.red },
  { id: 'purchase', icon: 'cart', color: C.green },
  { id: 'appeal', icon: 'hammer', color: C.purple },
  { id: 'other', icon: 'chatbubbles', color: C.muted },
];

const when = (ms, lang) => new Date(ms).toLocaleString(lang === 'de' ? 'de-DE' : 'en-GB', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });

export default function TicketScreen({ ui, go }) {
  const { t, lang, toast, Glass, Btn, PageHead, desk } = ui;
  const [token, setToken] = useState(undefined);
  const [cat, setCat] = useState('support');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [discord, setDiscord] = useState('');
  const [target, setTarget] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  const [mine, setMine] = useState(null);
  const [sent, setSent] = useState(null);

  const loadMine = useCallback(async (tok) => {
    try {
      const r = await fetch(`${API}?action=tickets`, { headers: { Authorization: `Bearer ${tok}` } });
      if (r.ok) setMine((await r.json()).tickets);
    } catch (e) { /* ignore */ }
  }, []);

  useEffect(() => {
    AsyncStorage.multiGet(['csmp-token', 'csmp-discord']).then((pairs) => {
      const tok = pairs[0][1];
      setToken(tok || null);
      if (pairs[1][1]) setDiscord(pairs[1][1]);
      if (tok) loadMine(tok);
    }).catch(() => setToken(null));
  }, [loadMine]);

  const submit = async () => {
    if (subject.trim().length < 3) { setErr(t('tkSubjectShort')); return; }
    if (message.trim().length < 10) { setErr(t('tkMessageShort')); return; }
    setBusy(true); setErr('');
    try {
      const r = await fetch(`${API}?action=ticket`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ category: cat, subject, message, discord, target: cat === 'report' ? target : '' }),
      });
      const d = await r.json();
      if (r.status === 429) setErr(t('tkLimit'));
      else if (r.status === 401) { setErr(t('tkLoginFirst')); setToken(null); }
      else if (!r.ok) setErr(t('tkError'));
      else {
        if (discord) AsyncStorage.setItem('csmp-discord', discord).catch(() => {});
        setSent(d); setSubject(''); setMessage(''); setTarget('');
        toast(t('tkSentToast'));
        loadMine(token);
      }
    } catch (e) {
      setErr(t('noConnection'));
    }
    setBusy(false);
  };

  if (token === undefined) return <ActivityIndicator color="#fff" style={{ marginTop: 40 }} />;

  return (
    <View>
      <Pressable onPress={() => go('community')} style={st.back}>
        <Ionicons name="chevron-back" size={18} color="#fff" />
        <Text style={st.backText}>{t('back')}</Text>
      </Pressable>
      <PageHead eyebrow="Support" title={t('tkTitle').toUpperCase()} intro={t('tkIntro')} />

      <View style={desk ? { flexDirection: 'row', gap: 24, alignItems: 'flex-start' } : null}>
      <View style={desk ? { flex: 1.4 } : null}>
      {!token ? (
        <Glass style={{ padding: 20, alignItems: 'center' }}>
          <Ionicons name="lock-closed" size={30} color={C.sky} />
          <Text style={[st.p, { marginVertical: 10 }]}>{t('tkLoginFirst')}</Text>
          <Btn icon="person-circle" label={t('tkToProfile')} onPress={() => go('profile')} />
        </Glass>
      ) : sent ? (
        <Glass style={{ padding: 22, alignItems: 'center' }} accent="rgba(61,220,132,0.55)">
          <View style={st.okIcon}><Ionicons name="checkmark" size={34} color="#06210f" /></View>
          <Text style={st.h}>{t('tkSent')}</Text>
          <Text style={st.id}>#{sent.id}</Text>
          <Text style={[st.p, { marginBottom: 16 }]}>{sent.delivered ? t('tkSentText') : t('tkSentQueued')}</Text>
          <Btn kind="ghost" icon="add" label={t('tkAnother')} onPress={() => setSent(null)} />
        </Glass>
      ) : (
        <Glass style={{ padding: 18 }}>
          <Text style={st.label}>{t('tkCategory').toUpperCase()}</Text>
          <View style={st.cats}>
            {CATEGORIES.map((c) => {
              const on = c.id === cat;
              return (
                <Pressable key={c.id} onPress={() => setCat(c.id)} style={[st.cat, desk && { width: '15.2%' }, on && { borderColor: c.color, backgroundColor: c.color + '2a' }]}>
                  <Ionicons name={c.icon} size={18} color={on ? c.color : C.muted} />
                  <Text style={[st.catText, on && { color: '#fff' }]} numberOfLines={1}>{t('tkCat_' + c.id)}</Text>
                </Pressable>
              );
            })}
          </View>

          {cat === 'report' ? (
            <>
              <Text style={st.label}>{t('tkTarget').toUpperCase()}</Text>
              <TextInput value={target} onChangeText={setTarget} placeholder="Steve" placeholderTextColor="rgba(255,255,255,0.3)" autoCapitalize="none" autoCorrect={false} maxLength={32} style={st.input} />
            </>
          ) : null}

          <Text style={st.label}>{t('tkSubject').toUpperCase()}</Text>
          <TextInput value={subject} onChangeText={(v) => { setSubject(v); setErr(''); }} placeholder={t('tkSubjectPh')} placeholderTextColor="rgba(255,255,255,0.3)" maxLength={80} style={st.input} />

          <Text style={st.label}>{t('tkMessage').toUpperCase()}</Text>
          <TextInput value={message} onChangeText={(v) => { setMessage(v); setErr(''); }} placeholder={t('tkMessagePh')} placeholderTextColor="rgba(255,255,255,0.3)" maxLength={1500} multiline textAlignVertical="top" style={[st.input, { height: 130, paddingTop: 12 }]} />
          <Text style={st.count}>{message.length}/1500</Text>

          <Text style={st.label}>{t('tkDiscord').toUpperCase()}</Text>
          <TextInput value={discord} onChangeText={setDiscord} placeholder={t('tkDiscordPh')} placeholderTextColor="rgba(255,255,255,0.3)" autoCapitalize="none" autoCorrect={false} maxLength={40} style={st.input} />

          {err ? <Text style={st.err}>{err}</Text> : null}
          <View style={{ marginTop: 14 }}>
            <Btn icon={busy ? null : 'send'} label={busy ? '…' : t('tkSend')} onPress={busy ? undefined : submit} />
          </View>
          <Text style={st.hint}>{t('tkHint')}</Text>
        </Glass>
      )}
      </View>

      {token && mine && mine.length ? (
        <View style={desk ? { flex: 1 } : null}>
          <Text style={[st.section, desk && { marginTop: 0, textAlign: 'left' }]}>{t('tkMine').toUpperCase()}</Text>
          <View style={{ gap: 10 }}>
            {mine.map((x) => {
              const c = CATEGORIES.find((k) => k.id === x.category) || CATEGORIES[5];
              return (
                <Glass key={x.id} style={st.row}>
                  <View style={[st.rowIcon, { backgroundColor: c.color + '2a' }]}><Ionicons name={c.icon} size={18} color={c.color} /></View>
                  <View style={{ flex: 1 }}>
                    <Text style={st.rowTitle} numberOfLines={1}>{x.subject}</Text>
                    <Text style={st.rowSub}>#{x.id} · {t('tkCat_' + x.category)} · {when(x.createdAt, lang)}</Text>
                  </View>
                  <View style={[st.badge, { backgroundColor: x.delivered ? 'rgba(61,220,132,0.15)' : 'rgba(245,184,46,0.15)' }]}>
                    <Text style={[st.badgeText, { color: x.delivered ? C.green : C.gold }]}>{(x.delivered ? t('tkStatusSent') : t('tkStatusQueued')).toUpperCase()}</Text>
                  </View>
                </Glass>
              );
            })}
          </View>
        </View>
      ) : null}
      </View>
    </View>
  );
}

const st = StyleSheet.create({
  back: { flexDirection: 'row', alignItems: 'center', gap: 4, alignSelf: 'flex-start', paddingVertical: 6, paddingHorizontal: 10, borderRadius: 12, backgroundColor: 'rgba(59,123,255,0.18)', borderWidth: 1, borderColor: 'rgba(59,123,255,0.5)' },
  backText: { color: '#fff', fontFamily: F.b, fontSize: 13 },
  label: { color: C.muted, fontFamily: F.b, fontSize: 10.5, letterSpacing: 0.8, marginTop: 14, marginBottom: 6 },
  cats: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  cat: { width: '31.5%', alignItems: 'center', gap: 4, paddingVertical: 10, borderRadius: 14, borderWidth: 1, borderColor: 'rgba(255,255,255,0.1)', backgroundColor: 'rgba(20,20,20,0.45)' },
  catText: { color: C.muted, fontFamily: F.b, fontSize: 11 },
  input: { minHeight: 48, borderRadius: 14, borderWidth: 1, borderColor: 'rgba(255,255,255,0.14)', backgroundColor: 'rgba(20,20,20,0.6)', color: '#fff', fontFamily: F.m, fontSize: 15, paddingHorizontal: 14 },
  count: { color: C.muted, fontFamily: F.r, fontSize: 11, textAlign: 'right', marginTop: 4 },
  err: { color: C.red, fontFamily: F.b, fontSize: 13, textAlign: 'center', marginTop: 12 },
  hint: { color: C.muted, fontFamily: F.r, fontSize: 12.5, textAlign: 'center', marginTop: 12, lineHeight: 18 },
  p: { color: C.soft, fontFamily: F.r, fontSize: 14.5, textAlign: 'center', lineHeight: 21 },
  h: { color: '#fff', fontFamily: F.x, fontSize: 22, marginTop: 12 },
  id: { color: C.green, fontFamily: F.x, fontSize: 16, marginVertical: 6 },
  okIcon: { width: 64, height: 64, borderRadius: 22, backgroundColor: C.green, alignItems: 'center', justifyContent: 'center' },
  section: { color: '#fff', fontFamily: F.x, fontSize: 20, textAlign: 'center', marginTop: 24, marginBottom: 12 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 12, borderRadius: 16 },
  rowIcon: { width: 38, height: 38, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  rowTitle: { color: '#fff', fontFamily: F.b, fontSize: 14.5 },
  rowSub: { color: C.muted, fontFamily: F.r, fontSize: 12 },
  badge: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 999 },
  badgeText: { fontFamily: F.x, fontSize: 9.5 },
});
