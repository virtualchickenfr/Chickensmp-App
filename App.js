import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import {
  Animated, Easing, Image, Linking, Pressable, RefreshControl, ScrollView,
  StyleSheet, Text, View, useWindowDimensions,
} from 'react-native';
import { SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import * as Clipboard from 'expo-clipboard';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import {
  useFonts, Poppins_400Regular, Poppins_500Medium, Poppins_600SemiBold, Poppins_700Bold, Poppins_800ExtraBold,
} from '@expo-google-fonts/poppins';
import {
  BEDROCK_PORT, C, DISCORD, DISCORD_INVITE_API, EXTRAS, IP, KEYS, PIXELS, RANKS, RULES,
  STATUS_API, STRINGS, TEAM, VERSION, WEBSITE,
} from './src/content';
import ProfileScreen from './src/Profile';
import Updater from './src/Updater';
import OtaUpdate from './src/OtaUpdate';
import TicketScreen from './src/Tickets';
import { API } from './src/Profile';

const F = {
  r: 'Poppins_400Regular', m: 'Poppins_500Medium', s: 'Poppins_600SemiBold', b: 'Poppins_700Bold', x: 'Poppins_800ExtraBold',
};
const GLASS = 'rgba(255,255,255,0.06)';
const GLASS_STRONG = 'rgba(255,255,255,0.10)';
const EDGE = 'rgba(255,255,255,0.14)';
const SHINE = 'rgba(255,255,255,0.28)';

/* ---------------- app state ---------------- */
const Ctx = createContext(null);
const useApp = () => useContext(Ctx);

function useLiveData() {
  const [status, setStatus] = useState({ state: 'loading', players: null, max: null });
  const [discord, setDiscord] = useState({ members: null, online: null });
  const load = useCallback(async () => {
    try {
      const r = await fetch(STATUS_API);
      const d = await r.json();
      if (d && d.online) setStatus({ state: 'online', players: d.players ? d.players.online : 0, max: d.players ? d.players.max : 0 });
      else setStatus({ state: 'offline', players: null, max: null });
    } catch (e) {
      setStatus({ state: 'offline', players: null, max: null });
    }
    try {
      const r = await fetch(DISCORD_INVITE_API);
      const d = await r.json();
      setDiscord({ members: d.approximate_member_count ?? null, online: d.approximate_presence_count ?? null });
    } catch (e) { /* keep last */ }
  }, []);
  useEffect(() => {
    load();
    const id = setInterval(load, 60000);
    return () => clearInterval(id);
  }, [load]);
  return { status, discord, reload: load };
}

/* ---------------- liquid background ---------------- */
const BLOBS = {
  blue: require('./assets/blob-blue.png'),
  sky: require('./assets/blob-sky.png'),
  purple: require('./assets/blob-purple.png'),
  green: require('./assets/blob-green.png'),
};

function Blob({ src, size, x, y, dur, delay, opacity }) {
  const v = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const loop = Animated.loop(Animated.sequence([
      Animated.timing(v, { toValue: 1, duration: dur, delay, easing: Easing.inOut(Easing.sin), useNativeDriver: true }),
      Animated.timing(v, { toValue: 0, duration: dur, easing: Easing.inOut(Easing.sin), useNativeDriver: true }),
    ]));
    loop.start();
    return () => loop.stop();
  }, [v, dur, delay]);
  const transform = [
    { translateX: v.interpolate({ inputRange: [0, 1], outputRange: [0, size * 0.18] }) },
    { translateY: v.interpolate({ inputRange: [0, 1], outputRange: [0, -size * 0.14] }) },
    { rotate: v.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '40deg'] }) },
    { scaleX: v.interpolate({ inputRange: [0, 0.5, 1], outputRange: [1, 1.25, 0.9] }) },
    { scaleY: v.interpolate({ inputRange: [0, 0.5, 1], outputRange: [1, 0.85, 1.15] }) },
  ];
  return (
    <Animated.Image
      source={src}
      style={{ position: 'absolute', left: x, top: y, width: size, height: size, opacity, transform }}
      resizeMode="contain"
    />
  );
}

function LiquidBackground() {
  const { width, height } = useWindowDimensions();
  const s = Math.max(width, 360) * 1.1;
  return (
    <View style={[StyleSheet.absoluteFill, { overflow: 'hidden' }]} pointerEvents="none">
      <View style={[StyleSheet.absoluteFill, { backgroundColor: C.bg }]} />
      <Blob src={BLOBS.blue} size={s} x={-s * 0.45} y={-s * 0.35} dur={9000} delay={0} opacity={0.55} />
      <Blob src={BLOBS.purple} size={s * 0.9} x={width - s * 0.5} y={height * 0.18} dur={11000} delay={800} opacity={0.4} />
      <Blob src={BLOBS.sky} size={s * 0.8} x={-s * 0.3} y={height * 0.55} dur={10000} delay={1600} opacity={0.38} />
      <Blob src={BLOBS.green} size={s * 0.6} x={width - s * 0.35} y={height * 0.78} dur={12000} delay={400} opacity={0.18} />
      <View style={[StyleSheet.absoluteFill, { backgroundColor: 'rgba(20,20,20,0.35)' }]} />
    </View>
  );
}

/* ---------------- building blocks ---------------- */
function Glass({ style, children, strong, accent }) {
  return (
    <View style={[styles.glass, strong && { backgroundColor: GLASS_STRONG }, accent && { borderColor: accent }, style]}>
      <View style={styles.glassShine} pointerEvents="none" />
      {children}
    </View>
  );
}

function Squish({ onPress, style, children, disabled }) {
  const s = useRef(new Animated.Value(1)).current;
  const to = (v) => Animated.spring(s, { toValue: v, useNativeDriver: true, speed: 30, bounciness: 12 }).start();
  return (
    <Pressable onPress={onPress} disabled={disabled} onPressIn={() => to(0.95)} onPressOut={() => to(1)}>
      <Animated.View style={[style, { transform: [{ scale: s }] }]}>{children}</Animated.View>
    </Pressable>
  );
}

function Btn({ label, icon, kind = 'primary', onPress, small, style }) {
  const bg = kind === 'primary' ? C.blue : kind === 'discord' ? C.discord : kind === 'done' ? C.green : GLASS_STRONG;
  const fg = kind === 'done' ? '#06210f' : '#fff';
  return (
    <Squish onPress={onPress} style={[styles.btn, small && styles.btnSm, { backgroundColor: bg }, kind === 'ghost' && { borderWidth: 1, borderColor: EDGE }, kind !== 'ghost' && styles.btnGlow, kind !== 'ghost' && { shadowColor: bg }, style]}>
      {icon ? <Ionicons name={icon} size={small ? 16 : 19} color={fg} /> : null}
      <Text style={[styles.btnText, small && { fontSize: 14 }, { color: fg }]} numberOfLines={1}>{label}</Text>
    </Squish>
  );
}

function CopyBtn({ value, small, kind = 'primary', label }) {
  const { t, toast } = useApp();
  const [done, setDone] = useState(false);
  const press = async () => {
    await Clipboard.setStringAsync(value);
    setDone(true);
    toast(value === IP ? t('ipCopied') : t('copied'));
    setTimeout(() => setDone(false), 1500);
  };
  return <Btn small={small} kind={done ? 'done' : kind} icon={done ? 'checkmark' : 'copy-outline'} label={done ? t('copied') : (label || t('copy'))} onPress={press} />;
}

function Eyebrow({ children, dot, center }) {
  return (
    <View style={[styles.eyebrow, center && { alignSelf: 'center' }]}>
      {dot ? <View style={styles.eyebrowDot} /> : null}
      <Text style={styles.eyebrowText}>{children}</Text>
    </View>
  );
}

function PageHead({ eyebrow, title, intro }) {
  return (
    <View style={{ alignItems: 'center', marginBottom: 20, marginTop: 6 }}>
      {eyebrow ? <Eyebrow center>{eyebrow}</Eyebrow> : null}
      <Text style={styles.pageTitle}>{title}</Text>
      {intro ? <Text style={styles.pageIntro}>{intro}</Text> : null}
    </View>
  );
}

function Pixel({ name, color, size = 6 }) {
  const rows = PIXELS[name];
  const shade = (hex, a) => {
    const n = parseInt(hex.slice(1), 16);
    const c = (v) => Math.max(0, Math.min(255, Math.round(v + a)));
    return `rgb(${c(n >> 16)},${c((n >> 8) & 255)},${c(n & 255)})`;
  };
  const pal = { a: shade(color, -70), b: color, c: name === 'crown' ? C.red : name === 'star' ? '#fff' : shade(color, 60), d: shade(color, -30) };
  return (
    <View>
      {rows.map((row, y) => {
        const runs = [];
        for (const ch of row) {
          const last = runs[runs.length - 1];
          if (last && last.ch === ch) last.n += 1; else runs.push({ ch, n: 1 });
        }
        return (
          <View key={y} style={{ flexDirection: 'row', height: size, marginTop: y ? -0.5 : 0 }}>
            {runs.map((r, i) => (
              <View key={i} style={{ width: r.n * size + (r.ch === '.' ? 0 : 0.5), marginRight: r.ch === '.' ? 0 : -0.5, height: size + 0.5, backgroundColor: r.ch === '.' ? 'transparent' : pal[r.ch] }} />
            ))}
          </View>
        );
      })}
    </View>
  );
}

/* ---------------- screens ---------------- */
function StatusPill() {
  const { t, live } = useApp();
  const st = live.status.state;
  const color = st === 'online' ? C.green : st === 'offline' ? C.red : C.muted;
  const text = st === 'loading' ? t('checking') : st === 'online' ? `${t('serverOnline')} · ${live.status.players}/${live.status.max}` : t('serverOffline');
  return (
    <View style={styles.statusPill}>
      <View style={[styles.statusDot, { backgroundColor: color }]} />
      <Text style={styles.statusText}>{text}</Text>
    </View>
  );
}

function OnlineNow() {
  const { t } = useApp();
  const [names, setNames] = useState(null);
  useEffect(() => {
    let alive = true;
    const load = () => fetch(`${API}?action=online`).then((r) => r.json()).then((d) => { if (alive) setNames(d.names || []); }).catch(() => {});
    load();
    const id = setInterval(load, 60000);
    return () => { alive = false; clearInterval(id); };
  }, []);
  if (!names || names.length === 0) return null;
  return (
    <Glass style={{ padding: 16, marginTop: 14 }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 12 }}>
        <View style={[styles.statusDot, { backgroundColor: C.green }]} />
        <Text style={{ color: '#fff', fontFamily: F.x, fontSize: 16 }}>{t('onlineList').toUpperCase()}</Text>
        <Text style={{ color: C.muted, fontFamily: F.b, fontSize: 13 }}>{names.length}</Text>
      </View>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 12 }}>
        {names.map((n) => (
          <View key={n} style={{ alignItems: 'center', width: 64 }}>
            <Image source={{ uri: `https://mc-heads.net/avatar/${encodeURIComponent(n)}/64` }} style={{ width: 44, height: 44, borderRadius: 10 }} />
            <Text style={{ color: C.soft, fontFamily: F.m, fontSize: 11, marginTop: 4 }} numberOfLines={1}>{n}</Text>
          </View>
        ))}
      </ScrollView>
    </Glass>
  );
}

function HomeScreen({ go }) {
  const { t, live } = useApp();
  const online = live.status.state === 'online' ? String(live.status.players) : live.status.state === 'offline' ? t('offline') : '–';
  const cards = [
    { title: t('cardJoin'), sub: t('cardJoinSub'), color: C.sky, icon: 'game-controller', onPress: () => go('join') },
    { title: t('cardRules'), sub: t('cardRulesSub'), color: C.green, icon: 'shield-checkmark', onPress: () => go('community') },
    { title: t('cardStore'), sub: t('cardStoreSub'), color: C.gold, icon: 'cart', onPress: () => go('store') },
    { title: t('cardDiscord'), sub: t('cardDiscordSub'), color: C.purple, icon: 'logo-discord', onPress: () => Linking.openURL(DISCORD) },
  ];
  return (
    <View>
      <Eyebrow dot>{t('seasonLive')}</Eyebrow>
      <Text style={styles.heroTitle}>{t('welcome').toUpperCase()}</Text>
      <Text style={[styles.heroTitle, { color: C.sky }]}>CHICKENSMP</Text>
      <Text style={styles.heroText}>{t('tagline')}</Text>
      <View style={{ marginTop: 18 }}><CopyBtn value={IP} label={IP} /></View>

      <View style={styles.chips}>
        <ChipCard icon="person" tint={C.green} value={online} label={t('online')} />
        <ChipCard icon="cube" tint={C.sky} value={VERSION} label={t('version')} />
        <ChipCard icon="phone-portrait" tint={C.purple} value="Java & BE" label={t('crossplay')} />
      </View>

      <Glass style={styles.season}>
        <View style={styles.seasonTag}><Text style={styles.seasonTagText}>SEASON 2</Text></View>
        <Text style={styles.seasonTitle}>{t('seasonTitle').toUpperCase()}</Text>
        <Text style={styles.seasonSub}>{t('seasonSub')}</Text>
        <StatusPill />
        <View style={styles.seasonBar} />
      </Glass>

      <OnlineNow />

      {cards.map((c) => (
        <Squish key={c.title} onPress={c.onPress} style={{ marginTop: 14 }}>
          <Glass style={styles.card}>
            <View style={[styles.cardStripe, { backgroundColor: c.color }]} />
            <View style={[styles.cardIcon, { backgroundColor: c.color + '26' }]}>
              <Ionicons name={c.icon} size={26} color={c.color} />
            </View>
            <View style={{ flex: 1 }}>
              <Text style={styles.cardTitle}>{c.title.toUpperCase()}</Text>
              <Text style={[styles.cardSub, { color: c.color }]}>{c.sub}</Text>
            </View>
            <Ionicons name="chevron-forward" size={20} color={C.muted} />
          </Glass>
        </Squish>
      ))}
    </View>
  );
}

function ChipCard({ icon, tint, value, label }) {
  return (
    <Glass style={styles.chipCard} strong>
      <View style={[styles.chipIcon, { backgroundColor: tint + '2a' }]}><Ionicons name={icon} size={18} color={tint} /></View>
      <Text style={styles.chipValue} numberOfLines={1}>{value}</Text>
      <Text style={styles.chipLabel} numberOfLines={1}>{label.toUpperCase()}</Text>
    </Glass>
  );
}

function Segmented({ items, value, onChange }) {
  const [w, setW] = useState(0);
  const idx = Math.max(0, items.findIndex((i) => i.id === value));
  const pos = useRef(new Animated.Value(idx)).current;
  const stretch = useRef(new Animated.Value(1)).current;
  useEffect(() => {
    Animated.parallel([
      Animated.spring(pos, { toValue: idx, useNativeDriver: true, speed: 14, bounciness: 9 }),
      Animated.sequence([
        Animated.timing(stretch, { toValue: 1.25, duration: 110, useNativeDriver: true }),
        Animated.spring(stretch, { toValue: 1, useNativeDriver: true, speed: 16, bounciness: 14 }),
      ]),
    ]).start();
  }, [idx, pos, stretch]);
  const seg = w ? (w - 8) / items.length : 0;
  return (
    <Glass style={styles.segment} strong>
      <View style={{ flexDirection: 'row' }} onLayout={(e) => setW(e.nativeEvent.layout.width + 8)}>
        {seg ? (
          <Animated.View
            style={[styles.segmentPill, { width: seg, transform: [{ translateX: Animated.multiply(pos, seg) }, { scaleX: stretch }] }]}
          />
        ) : null}
        {items.map((it) => (
          <Pressable key={it.id} style={styles.segmentItem} onPress={() => onChange(it.id)}>
            <Text style={[styles.segmentText, it.id === value && { color: '#fff' }]} numberOfLines={1}>{it.label}</Text>
          </Pressable>
        ))}
      </View>
    </Glass>
  );
}

function Field({ label, value, copy }) {
  return (
    <View style={styles.field}>
      <View style={{ flex: 1 }}>
        <Text style={styles.fieldLabel}>{label.toUpperCase()}</Text>
        <Text style={styles.fieldValue}>{value}</Text>
      </View>
      {copy ? <CopyBtn small value={value} /> : null}
    </View>
  );
}

function Steps({ items }) {
  return (
    <View style={{ gap: 12, marginTop: 16 }}>
      {items.map((s, i) => (
        <View key={i} style={{ flexDirection: 'row', gap: 12, alignItems: 'flex-start' }}>
          <View style={styles.stepNum}><Text style={styles.stepNumText}>{i + 1}</Text></View>
          <Text style={styles.stepText}>{s}</Text>
        </View>
      ))}
    </View>
  );
}

function HelpPanel() {
  const { t } = useApp();
  return (
    <Glass style={{ padding: 20, marginTop: 16, alignItems: 'center' }}>
      <Text style={styles.h2}>{t('helpTitle')}</Text>
      <Text style={[styles.body, { textAlign: 'center', marginBottom: 14 }]}>{t('helpText')}</Text>
      <Btn kind="discord" icon="logo-discord" label={t('joinDiscord')} onPress={() => Linking.openURL(DISCORD)} />
    </Glass>
  );
}

function JoinScreen() {
  const { t } = useApp();
  const [ed, setEd] = useState('java');
  return (
    <View>
      <PageHead eyebrow="4 Steps" title={t('joinTitle').toUpperCase()} intro={t('joinIntro')} />
      <Segmented items={[{ id: 'java', label: 'Java' }, { id: 'bedrock', label: 'Bedrock' }]} value={ed} onChange={setEd} />
      <Glass style={{ padding: 20, marginTop: 14 }}>
        <View style={styles.joinHead}>
          <Text style={styles.h2}>{ed === 'java' ? 'JAVA EDITION' : 'BEDROCK EDITION'}</Text>
          <View style={[styles.chip, ed === 'bedrock' && { backgroundColor: 'rgba(61,220,132,0.15)' }]}>
            <Text style={[styles.chipText, ed === 'bedrock' && { color: C.green }]}>{ed === 'java' ? 'PC · Mac' : 'Mobile · Console'}</Text>
          </View>
        </View>
        <View style={{ gap: 10 }}>
          <Field label={t('address')} value={IP} copy />
          {ed === 'java' ? <Field label={t('version')} value={VERSION} /> : <Field label={t('port')} value={BEDROCK_PORT} copy />}
        </View>
        <Steps items={ed === 'java' ? [t('j1'), t('j2'), t('j3'), t('j4')] : [t('b1'), t('b2'), t('b3'), t('b4')]} />
      </Glass>
      <HelpPanel />
    </View>
  );
}

function PackageCard({ it }) {
  const { t } = useApp();
  const [open, setOpen] = useState(false);
  const h = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    Animated.spring(h, { toValue: open ? 1 : 0, useNativeDriver: false, speed: 14, bounciness: 6 }).start();
  }, [open, h]);
  const price = it.price === 'free' ? t('free') : it.price || t('askPrice');
  return (
    <Glass style={[styles.pkg, it.popular && styles.pkgPopular]} accent={it.popular ? 'rgba(255,140,50,0.6)' : null}>
      {it.popular ? (
        <View style={styles.badge}><Ionicons name="flame" size={12} color="#fff" /><Text style={styles.badgeText}>{t('popular').toUpperCase()}</Text></View>
      ) : null}
      <View style={styles.pkgMedia}><Pixel name={it.art} color={it.color} size={it.art === 'crown' ? 8 : 7} /></View>
      <Text style={styles.pkgName}>{it.name}</Text>
      {it.tag ? <Text style={[styles.pkgTag, { color: it.color }]}>{it.tag}</Text> : null}
      <View style={styles.pkgPrice}>
        <Text style={[styles.pkgPriceMain, !it.price && { color: C.muted, fontSize: 15 }]}>{price}</Text>
        {it.per ? <Text style={styles.pkgPer}>{t(it.per)}</Text> : null}
      </View>
      <Text style={[styles.body, { textAlign: 'center', marginTop: 6 }]}>{t(it.desc)}</Text>
      <Animated.View style={{ overflow: 'hidden', maxHeight: h.interpolate({ inputRange: [0, 1], outputRange: [0, 260] }), opacity: h, alignSelf: 'stretch' }}>
        <View style={styles.perks}>
          {it.perks.map((p) => (
            <View key={p} style={{ flexDirection: 'row', gap: 10, alignItems: 'center' }}>
              <View style={[styles.perkDot, { backgroundColor: it.color }]} />
              <Text style={styles.perkText}>{t(p)}</Text>
            </View>
          ))}
        </View>
      </Animated.View>
      <View style={styles.pkgActions}>
        <Squish onPress={() => setOpen(!open)} style={styles.infoBtn}>
          <Ionicons name={open ? 'chevron-up' : 'information-circle-outline'} size={22} color={C.soft} />
        </Squish>
        <View style={{ flex: 1 }}>
          <Btn small icon="chatbubble" label={t(it.claim ? 'claim' : 'buy')} onPress={() => Linking.openURL(DISCORD)} />
        </View>
      </View>
    </Glass>
  );
}

function StoreScreen() {
  const { t } = useApp();
  const [cat, setCat] = useState('ranks');
  const data = cat === 'ranks' ? RANKS : cat === 'keys' ? KEYS : EXTRAS;
  const desc = cat === 'ranks' ? t('ranksDesc') : cat === 'keys' ? t('keysDesc') : t('extrasDesc');
  return (
    <View>
      <PageHead eyebrow="ChickenSMP" title={t('storeTitle').toUpperCase()} intro={t('storeIntro')} />
      <Segmented items={[{ id: 'ranks', label: t('catRanks') }, { id: 'keys', label: t('catKeys') }, { id: 'extras', label: t('catExtras') }]} value={cat} onChange={setCat} />
      <Text style={[styles.body, { textAlign: 'center', marginVertical: 14 }]}>{desc}</Text>
      <View style={{ gap: 14 }}>
        {data.map((it) => <PackageCard key={it.name} it={it} />)}
      </View>
      <Glass style={{ padding: 20, marginTop: 16 }}>
        <View style={[styles.tab, { backgroundColor: C.blue }]} />
        <Text style={[styles.h2, { textAlign: 'center' }]}>{t('howTitle')}</Text>
        <Steps items={[t('h1'), t('h2'), t('h3'), t('h4')]} />
      </Glass>
    </View>
  );
}

function CommunityScreen({ go }) {
  const { t, live } = useApp();
  const d = live.discord;
  return (
    <View>
      <Squish onPress={() => go('tickets')} style={{ marginBottom: 14 }}>
        <Glass style={styles.card} accent="rgba(59,123,255,0.55)">
          <View style={[styles.cardStripe, { backgroundColor: C.blue }]} />
          <View style={[styles.cardIcon, { backgroundColor: 'rgba(59,123,255,0.22)' }]}>
            <Ionicons name="chatbubble-ellipses" size={26} color={C.sky} />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.cardTitle}>{t('tkCard').toUpperCase()}</Text>
            <Text style={[styles.cardSub, { color: C.sky }]}>{t('tkCardSub')}</Text>
          </View>
          <Ionicons name="chevron-forward" size={20} color={C.muted} />
        </Glass>
      </Squish>
      <Glass style={{ padding: 22, alignItems: 'center' }} accent="rgba(88,101,242,0.55)">
        <View style={[styles.cardIcon, { backgroundColor: 'rgba(88,101,242,0.25)', marginBottom: 10 }]}>
          <Ionicons name="logo-discord" size={28} color="#fff" />
        </View>
        <Text style={styles.h2}>{t('discordTitle')}</Text>
        <Text style={[styles.body, { textAlign: 'center' }]}>{t('discordText')}</Text>
        <View style={{ flexDirection: 'row', gap: 22, marginVertical: 14 }}>
          <Count value={d.members} label={t('members')} />
          <Count value={d.online} label={t('onlineNow')} dot />
        </View>
        <Btn kind="discord" icon="logo-discord" label={t('joinDiscord')} onPress={() => Linking.openURL(DISCORD)} />
      </Glass>

      <PageHead title={t('rulesTitle').toUpperCase()} intro={t('rulesIntro')} />
      <View style={{ gap: 10, marginTop: -6 }}>
        {RULES.map((r, i) => (
          <Glass key={r} style={styles.rule}>
            <View style={styles.ruleNum}><Text style={styles.ruleNumText}>{i + 1}</Text></View>
            <View style={{ flex: 1 }}>
              <Text style={styles.ruleTitle}>{t(r)}</Text>
              <Text style={styles.ruleText}>{t(r + 'p')}</Text>
            </View>
          </Glass>
        ))}
      </View>
      <Text style={[styles.note]}>{t('rulesNote')}</Text>

      <PageHead title={t('teamTitle').toUpperCase()} />
      <View style={styles.teamGrid}>
        {TEAM.map((m) => (
          <Glass key={m.name} style={styles.member}>
            <Image source={{ uri: `https://mc-heads.net/avatar/${m.name}/96` }} style={[styles.avatar, { shadowColor: m.color }]} />
            <Text style={styles.memberName} numberOfLines={1} adjustsFontSizeToFit>{m.name}</Text>
            <View style={[styles.role, { backgroundColor: m.color + '2e' }]}>
              <Text style={[styles.roleText, { color: m.color }]}>{(m.roleKey ? t(m.roleKey) : m.role).toUpperCase()}</Text>
            </View>
            <Text style={styles.memberText}>{t(m.text)}</Text>
          </Glass>
        ))}
      </View>

      <Pressable onPress={() => Linking.openURL(WEBSITE)} style={{ alignItems: 'center', marginTop: 22 }}>
        <Text style={{ color: C.sky, fontFamily: F.b }}>store.chickensmp.net</Text>
      </Pressable>
      <Text style={[styles.note, { textAlign: 'center' }]}>© {new Date().getFullYear()} ChickenSMP · {t('disclaimer')}</Text>
    </View>
  );
}

function Count({ value, label, dot }) {
  return (
    <View style={{ alignItems: 'center' }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
        {dot ? <View style={[styles.statusDot, { backgroundColor: C.green }]} /> : null}
        <Text style={{ color: '#fff', fontFamily: F.x, fontSize: 22 }}>{value ?? '–'}</Text>
      </View>
      <Text style={styles.chipLabel}>{label.toUpperCase()}</Text>
    </View>
  );
}

/* ---------------- shell ---------------- */
const TABS = [
  { id: 'home', icon: 'home', label: 'tabHome' },
  { id: 'join', icon: 'game-controller', label: 'tabJoin' },
  { id: 'store', icon: 'cart', label: 'tabStore' },
  { id: 'community', icon: 'people', label: 'tabCommunity' },
  { id: 'profile', icon: 'person-circle', label: 'tabProfile' },
];

function TabBar({ tab, setTab }) {
  const { t } = useApp();
  const insets = useSafeAreaInsets();
  const [w, setW] = useState(0);
  const idx = Math.max(0, TABS.findIndex((x) => x.id === (tab === 'tickets' ? 'community' : tab)));
  const pos = useRef(new Animated.Value(idx)).current;
  const sx = useRef(new Animated.Value(1)).current;
  const sy = useRef(new Animated.Value(1)).current;
  useEffect(() => {
    Animated.parallel([
      Animated.spring(pos, { toValue: idx, useNativeDriver: true, speed: 12, bounciness: 10 }),
      Animated.sequence([
        Animated.parallel([
          Animated.timing(sx, { toValue: 1.45, duration: 120, easing: Easing.out(Easing.quad), useNativeDriver: true }),
          Animated.timing(sy, { toValue: 0.78, duration: 120, easing: Easing.out(Easing.quad), useNativeDriver: true }),
        ]),
        Animated.parallel([
          Animated.spring(sx, { toValue: 1, useNativeDriver: true, speed: 10, bounciness: 16 }),
          Animated.spring(sy, { toValue: 1, useNativeDriver: true, speed: 10, bounciness: 16 }),
        ]),
      ]),
    ]).start();
  }, [idx, pos, sx, sy]);
  const seg = w / TABS.length;
  return (
    <View style={[styles.tabWrap, { paddingBottom: Math.max(insets.bottom, 10) }]} pointerEvents="box-none">
      <Glass style={styles.tabBar}>
        <View style={{ flexDirection: 'row' }} onLayout={(e) => setW(e.nativeEvent.layout.width)}>
          {w ? (
            <Animated.View
              style={[styles.tabBlob, { width: seg - 8, transform: [{ translateX: Animated.add(Animated.multiply(pos, seg), 4) }, { scaleX: sx }, { scaleY: sy }] }]}
            />
          ) : null}
          {TABS.map((x) => {
            const on = x.id === (tab === 'tickets' ? 'community' : tab);
            return (
              <Pressable key={x.id} style={styles.tabItem} onPress={() => setTab(x.id)}>
                <Ionicons name={on ? x.icon : x.icon + '-outline'} size={22} color={on ? '#fff' : C.muted} />
                <Text style={[styles.tabLabel, on && { color: '#fff' }]} numberOfLines={1}>{t(x.label)}</Text>
              </Pressable>
            );
          })}
        </View>
      </Glass>
    </View>
  );
}

function Header({ lang, setLang }) {
  const { live } = useApp();
  return (
    <View style={styles.header}>
      <Image source={require('./assets/logo.png')} style={styles.logo} resizeMode="contain" />
      <View style={{ flex: 1 }} />
      <View style={[styles.statusPill, { marginTop: 0, marginRight: 8 }]}>
        <View style={[styles.statusDot, { backgroundColor: live.status.state === 'online' ? C.green : live.status.state === 'offline' ? C.red : C.muted }]} />
        <Text style={styles.statusText}>{live.status.state === 'online' ? live.status.players : '–'}</Text>
      </View>
      <Glass style={styles.lang} strong>
        {['de', 'en'].map((l) => (
          <Pressable key={l} onPress={() => setLang(l)} style={[styles.langBtn, lang === l && { backgroundColor: C.blue }]}>
            <Text style={[styles.langText, lang === l && { color: '#fff' }]}>{l.toUpperCase()}</Text>
          </Pressable>
        ))}
      </Glass>
    </View>
  );
}

function Shell() {
  const insets = useSafeAreaInsets();
  const [lang, setLangState] = useState('en');
  const [tab, setTab] = useState('home');
  const [toastMsg, setToastMsg] = useState(null);
  const [refreshing, setRefreshing] = useState(false);
  const toastAnim = useRef(new Animated.Value(0)).current;
  const fade = useRef(new Animated.Value(1)).current;
  const scrollRef = useRef(null);
  const live = useLiveData();

  useEffect(() => {
    AsyncStorage.getItem('csmp-lang').then((v) => { if (v === 'de' || v === 'en') setLangState(v); }).catch(() => {});
  }, []);
  const setLang = (l) => { setLangState(l); AsyncStorage.setItem('csmp-lang', l).catch(() => {}); };
  const t = useCallback((k) => (STRINGS[lang] && STRINGS[lang][k]) || STRINGS.en[k] || k, [lang]);

  const toast = useCallback((msg) => {
    setToastMsg(msg);
    toastAnim.setValue(0);
    Animated.sequence([
      Animated.spring(toastAnim, { toValue: 1, useNativeDriver: true, speed: 18, bounciness: 10 }),
      Animated.delay(1300),
      Animated.timing(toastAnim, { toValue: 0, duration: 200, useNativeDriver: true }),
    ]).start();
  }, [toastAnim]);

  const go = (id) => {
    if (id === tab) return;
    Animated.timing(fade, { toValue: 0, duration: 90, useNativeDriver: true }).start(() => {
      setTab(id);
      scrollRef.current?.scrollTo({ y: 0, animated: false });
      Animated.spring(fade, { toValue: 1, useNativeDriver: true, speed: 16, bounciness: 6 }).start();
    });
  };

  const onRefresh = async () => { setRefreshing(true); await live.reload(); setRefreshing(false); };
  const ctx = useMemo(() => ({ t, lang, toast, live }), [t, lang, toast, live]);

  return (
    <Ctx.Provider value={ctx}>
      <View style={{ flex: 1, backgroundColor: C.bg, overflow: 'hidden' }}>
        <StatusBar style="light" />
        <LiquidBackground />
        <View style={{ paddingTop: insets.top }}>
          <Header lang={lang} setLang={setLang} />
        </View>
        <ScrollView
          ref={scrollRef}
          contentContainerStyle={{ padding: 16, paddingBottom: 120 + insets.bottom }}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#fff" colors={[C.blue]} progressBackgroundColor={C.surface} />}
        >
          <Animated.View style={{ opacity: fade, transform: [{ translateY: fade.interpolate({ inputRange: [0, 1], outputRange: [12, 0] }) }] }}>
            {tab === 'home' && <HomeScreen go={go} />}
            {tab === 'join' && <JoinScreen />}
            {tab === 'store' && <StoreScreen />}
            {tab === 'community' && <CommunityScreen go={go} />}
            {tab === 'profile' && <ProfileScreen ui={{ t, lang, toast, Glass, Btn, Segmented, PageHead, go }} />}
            {tab === 'tickets' && <TicketScreen ui={{ t, lang, toast, Glass, Btn, PageHead }} go={go} />}
          </Animated.View>
        </ScrollView>
        <TabBar tab={tab} setTab={go} />
        <Updater t={t} Btn={Btn} />
        <OtaUpdate t={t} />
        {toastMsg ? (
          <Animated.View
            pointerEvents="none"
            style={[styles.toast, { bottom: 104 + insets.bottom, opacity: toastAnim, transform: [{ scale: toastAnim.interpolate({ inputRange: [0, 1], outputRange: [0.7, 1] }) }] }]}
          >
            <Ionicons name="checkmark-circle" size={18} color="#06210f" />
            <Text style={styles.toastText}>{toastMsg}</Text>
          </Animated.View>
        ) : null}
      </View>
    </Ctx.Provider>
  );
}

export default function App() {
  const [loaded] = useFonts({ Poppins_400Regular, Poppins_500Medium, Poppins_600SemiBold, Poppins_700Bold, Poppins_800ExtraBold });
  if (!loaded) return <View style={{ flex: 1, backgroundColor: C.bg }} />;
  return (
    <SafeAreaProvider>
      <Shell />
    </SafeAreaProvider>
  );
}

/* ---------------- styles ---------------- */
const styles = StyleSheet.create({
  glass: {
    backgroundColor: GLASS, borderRadius: 22, borderWidth: 1, borderColor: EDGE, overflow: 'hidden',
  },
  glassShine: {
    position: 'absolute', top: 0, left: 18, right: 18, height: 1, backgroundColor: SHINE,
  },
  btn: {
    minHeight: 52, paddingHorizontal: 22, borderRadius: 16, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 10,
  },
  btnSm: { minHeight: 42, paddingHorizontal: 14, borderRadius: 12, gap: 7 },
  btnGlow: { shadowOpacity: 0.6, shadowRadius: 18, shadowOffset: { width: 0, height: 10 }, elevation: 6 },
  btnText: { color: '#fff', fontFamily: F.b, fontSize: 16 },

  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 8 },
  logo: { width: 112, height: 48 },
  lang: { flexDirection: 'row', padding: 4, borderRadius: 14 },
  langBtn: { paddingHorizontal: 10, paddingVertical: 5, borderRadius: 10 },
  langText: { color: C.muted, fontFamily: F.b, fontSize: 12 },

  eyebrow: {
    alignSelf: 'flex-start', flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 12, paddingVertical: 5,
    borderRadius: 999, borderWidth: 1, borderColor: 'rgba(79,182,255,0.4)', backgroundColor: 'rgba(20,20,20,0.45)', marginBottom: 10,
  },
  eyebrowDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: C.green },
  eyebrowText: { color: C.sky, fontFamily: F.b, fontSize: 11, letterSpacing: 0.8, textTransform: 'uppercase' },
  heroTitle: { color: '#fff', fontFamily: F.x, fontSize: 40, lineHeight: 44 },
  heroText: { color: C.soft, fontFamily: F.r, fontSize: 16, lineHeight: 24, marginTop: 12 },

  chips: { flexDirection: 'row', gap: 10, marginTop: 18 },
  chipCard: { flex: 1, alignItems: 'center', paddingVertical: 12, paddingHorizontal: 6, borderRadius: 16 },
  chipIcon: { width: 36, height: 36, borderRadius: 11, alignItems: 'center', justifyContent: 'center', marginBottom: 6 },
  chipValue: { color: '#fff', fontFamily: F.x, fontSize: 14 },
  chipLabel: { color: C.muted, fontFamily: F.b, fontSize: 10, letterSpacing: 0.4, marginTop: 1 },

  season: { marginTop: 18, padding: 20, paddingBottom: 26, alignItems: 'center' },
  seasonTag: { backgroundColor: C.green, paddingHorizontal: 18, paddingVertical: 8, borderRadius: 12, marginBottom: 12 },
  seasonTagText: { color: '#06210f', fontFamily: F.x, fontSize: 17 },
  seasonTitle: { color: '#fff', fontFamily: F.x, fontSize: 20, textAlign: 'center' },
  seasonSub: { color: C.muted, fontFamily: F.s, fontSize: 13, textAlign: 'center', marginTop: 2 },
  seasonBar: { position: 'absolute', bottom: 0, alignSelf: 'center', width: 80, height: 6, borderTopLeftRadius: 6, borderTopRightRadius: 6, backgroundColor: C.green },
  statusPill: {
    flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 14, paddingHorizontal: 12, paddingVertical: 6,
    borderRadius: 999, backgroundColor: 'rgba(20,20,20,0.5)',
  },
  statusDot: { width: 9, height: 9, borderRadius: 5 },
  statusText: { color: '#fff', fontFamily: F.s, fontSize: 13 },

  card: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 20, paddingLeft: 24, paddingRight: 16 },
  cardStripe: { position: 'absolute', left: 0, top: '28%', bottom: '28%', width: 6, borderTopRightRadius: 4, borderBottomRightRadius: 4 },
  cardIcon: { width: 52, height: 52, borderRadius: 16, alignItems: 'center', justifyContent: 'center' },
  cardTitle: { color: '#fff', fontFamily: F.x, fontSize: 20 },
  cardSub: { fontFamily: F.b, fontSize: 13 },

  pageTitle: { color: '#fff', fontFamily: F.x, fontSize: 34, textAlign: 'center', marginTop: 4 },
  pageIntro: { color: C.soft, fontFamily: F.r, fontSize: 15, lineHeight: 22, textAlign: 'center', marginTop: 6, maxWidth: 420 },
  h2: { color: '#fff', fontFamily: F.x, fontSize: 19, marginBottom: 4 },
  body: { color: C.soft, fontFamily: F.r, fontSize: 14, lineHeight: 21 },
  note: { color: C.muted, fontFamily: F.r, fontSize: 13, lineHeight: 19, marginTop: 14 },

  segment: { padding: 4, borderRadius: 16 },
  segmentPill: {
    position: 'absolute', top: 0, bottom: 0, left: 0, borderRadius: 12, backgroundColor: C.blue,
    shadowColor: C.blue, shadowOpacity: 0.7, shadowRadius: 14, shadowOffset: { width: 0, height: 6 }, elevation: 4,
  },
  segmentItem: { flex: 1, alignItems: 'center', paddingVertical: 11 },
  segmentText: { color: C.muted, fontFamily: F.b, fontSize: 14, textTransform: 'uppercase' },

  joinHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14, gap: 8 },
  chip: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 999, backgroundColor: 'rgba(59,123,255,0.18)' },
  chipText: { color: C.sky, fontFamily: F.b, fontSize: 11 },
  field: {
    flexDirection: 'row', alignItems: 'center', gap: 10, padding: 12, paddingLeft: 16, borderRadius: 14,
    borderWidth: 1, borderColor: C.border, backgroundColor: 'rgba(20,20,20,0.55)',
  },
  fieldLabel: { color: C.muted, fontFamily: F.b, fontSize: 10.5, letterSpacing: 0.8 },
  fieldValue: { color: '#fff', fontFamily: F.b, fontSize: 18 },
  stepNum: { width: 28, height: 28, borderRadius: 9, backgroundColor: 'rgba(255,255,255,0.1)', alignItems: 'center', justifyContent: 'center' },
  stepNumText: { color: '#fff', fontFamily: F.x, fontSize: 13 },
  stepText: { flex: 1, color: C.soft, fontFamily: F.r, fontSize: 15, lineHeight: 22, paddingTop: 2 },

  pkg: { alignItems: 'center', paddingTop: 26, paddingHorizontal: 18, paddingBottom: 16 },
  pkgPopular: { backgroundColor: 'rgba(255,140,50,0.08)' },
  badge: {
    position: 'absolute', top: 14, right: 14, flexDirection: 'row', alignItems: 'center', gap: 4,
    paddingHorizontal: 9, paddingVertical: 4, borderRadius: 9, backgroundColor: '#ff6a24',
  },
  badgeText: { color: '#fff', fontFamily: F.x, fontSize: 10.5, letterSpacing: 0.5 },
  pkgMedia: { height: 96, alignItems: 'center', justifyContent: 'center', marginBottom: 10 },
  pkgName: { color: '#fff', fontFamily: F.x, fontSize: 21 },
  pkgTag: { fontFamily: F.x, fontSize: 12, letterSpacing: 0.6, marginTop: 2 },
  pkgPrice: { flexDirection: 'row', alignItems: 'baseline', gap: 6, marginTop: 6 },
  pkgPriceMain: { color: C.sky, fontFamily: F.x, fontSize: 20 },
  pkgPer: { color: C.muted, fontFamily: F.s, fontSize: 13 },
  perks: { gap: 8, marginTop: 14, padding: 14, borderRadius: 14, backgroundColor: 'rgba(20,20,20,0.45)', borderWidth: 1, borderColor: C.border },
  perkDot: { width: 8, height: 8, borderRadius: 2 },
  perkText: { color: C.soft, fontFamily: F.m, fontSize: 14 },
  pkgActions: { flexDirection: 'row', gap: 10, marginTop: 16, alignSelf: 'stretch' },
  infoBtn: { width: 46, height: 42, borderRadius: 12, borderWidth: 1, borderColor: EDGE, backgroundColor: GLASS, alignItems: 'center', justifyContent: 'center' },
  tab: { position: 'absolute', top: 0, alignSelf: 'center', width: 64, height: 5, borderBottomLeftRadius: 5, borderBottomRightRadius: 5 },

  rule: { flexDirection: 'row', gap: 12, padding: 14, borderRadius: 16, alignItems: 'flex-start' },
  ruleNum: { width: 30, height: 30, borderRadius: 9, backgroundColor: 'rgba(79,182,255,0.18)', alignItems: 'center', justifyContent: 'center' },
  ruleNumText: { color: C.sky, fontFamily: F.x, fontSize: 13 },
  ruleTitle: { color: '#fff', fontFamily: F.b, fontSize: 15 },
  ruleText: { color: C.soft, fontFamily: F.r, fontSize: 13.5, lineHeight: 20 },

  teamGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12, marginTop: -6 },
  member: { width: '47.5%', alignItems: 'center', paddingVertical: 18, paddingHorizontal: 10 },
  avatar: { width: 72, height: 72, borderRadius: 14, marginBottom: 10 },
  memberName: { color: '#fff', fontFamily: F.x, fontSize: 15 },
  role: { paddingHorizontal: 10, paddingVertical: 3, borderRadius: 999, marginTop: 6 },
  roleText: { fontFamily: F.x, fontSize: 10.5 },
  memberText: { color: C.soft, fontFamily: F.r, fontSize: 12.5, textAlign: 'center', marginTop: 8, lineHeight: 18 },

  tabWrap: { position: 'absolute', left: 0, right: 0, bottom: 0, paddingHorizontal: 14 },
  tabBar: { padding: 4, borderRadius: 26, backgroundColor: 'rgba(26,26,28,0.985)' },
  tabBlob: {
    position: 'absolute', top: 0, bottom: 0, left: 0, borderRadius: 22, backgroundColor: 'rgba(59,123,255,0.9)',
    shadowColor: C.blue, shadowOpacity: 0.8, shadowRadius: 16, shadowOffset: { width: 0, height: 4 }, elevation: 6,
  },
  tabItem: { flex: 1, alignItems: 'center', paddingVertical: 9, gap: 2 },
  tabLabel: { color: C.muted, fontFamily: F.b, fontSize: 10 },

  toast: {
    position: 'absolute', alignSelf: 'center', flexDirection: 'row', alignItems: 'center', gap: 8,
    paddingHorizontal: 18, paddingVertical: 11, borderRadius: 14, backgroundColor: C.green,
  },
  toastText: { color: '#06210f', fontFamily: F.b, fontSize: 14 },
});
