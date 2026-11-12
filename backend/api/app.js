// Used by the ChickenSMP app: login with in-game code, profile, logout, leaderboards, online list, tickets.
import crypto from 'node:crypto';
import { getJson, putJson, delPath, sha, readBody, listPlayers } from './_store.js';

function cors(res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Headers', 'Authorization, Content-Type');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
}
const bearer = (req) => String(req.headers.authorization || '').replace(/^Bearer\s+/i, '').trim();

async function auth(req) {
  const tok = bearer(req);
  if (tok.length !== 64) return null;
  return getJson(`tokens/${sha(tok)}.json`);
}

const CATS = {
  support: { label: 'Support', color: 0x3b7bff },
  bug: { label: 'Bug', color: 0xf5b82e },
  report: { label: 'Player report', color: 0xff5a6a },
  purchase: { label: 'Purchase / rank', color: 0x3ddc84 },
  appeal: { label: 'Ban appeal', color: 0xa855f7 },
  other: { label: 'Other', color: 0x9a9a9a },
};
const clip = (v, n) => String(v || '').replace(/\s+$/g, '').slice(0, n);
const noPing = (v) => v.replace(/@(everyone|here)/gi, '@​$1');

async function sendToDiscord(t, player) {
  const raw = String(process.env.DISCORD_TICKET_WEBHOOK || '');
  // Find the webhook address inside the value, even with quotes, a "KEY=" prefix or an /api/v10/ path.
  const m = /https:\/\/(?:[a-z]+\.)?discord(?:app)?\.com\/api\/(?:v\d+\/)?webhooks\/\d+\/[\w-]+/.exec(raw);
  if (!m) {
    // Log only the shape of the value, never the address or its token.
    const shape = raw.trim().replace(/[A-Za-z0-9_-]{12,}/g, '…').slice(0, 80);
    console.log(`ticket ${t.id}: webhook ${raw.trim() ? `has an unexpected format (length ${raw.length}, shape "${shape}")` : 'is not set'}`);
    return false;
  }
  const hook = m[0];
  const cat = CATS[t.category] || CATS.other;
  const fields = [
    { name: 'Player', value: noPing(t.name) || '–', inline: true },
    { name: 'Rank', value: noPing((player && player.rank) || '–'), inline: true },
    { name: 'Discord', value: noPing(t.discord || '–'), inline: true },
  ];
  if (t.target) fields.push({ name: 'Reported player', value: noPing(t.target), inline: true });
  if (player) fields.push({ name: 'Status', value: player.online ? 'Online now' : 'Offline', inline: true });
  const body = {
    username: 'ChickenSMP Tickets',
    allowed_mentions: { parse: [] },
    embeds: [{
      title: `🎫 ${cat.label} · #${t.id}`,
      description: `**${noPing(t.subject)}**\n\n${noPing(t.message)}`.slice(0, 3900),
      color: cat.color,
      fields,
      thumbnail: { url: `https://mc-heads.net/avatar/${t.uuid}/64` },
      footer: { text: 'Sent from the ChickenSMP app / website' },
      timestamp: new Date(t.createdAt).toISOString(),
    }],
  };
  try {
    const r = await fetch(hook, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    if (!r.ok) console.log(`ticket ${t.id}: Discord answered ${r.status} ${(await r.text()).slice(0, 200)}`);
    else console.log(`ticket ${t.id}: delivered to Discord`);
    return r.ok;
  } catch (e) {
    console.log(`ticket ${t.id}: Discord request failed ${e && e.message}`);
    return false;
  }
}

// Daily login rewards: a 7-day streak, delivered in-game by the server plugin.
export const DAILY = [
  { key: 'basic', amount: 1 }, { key: 'basic', amount: 2 }, { key: 'plus', amount: 1 }, { key: 'plus', amount: 2 },
  { key: 'core', amount: 1 }, { key: 'core', amount: 2 }, { key: 'supreme', amount: 1 },
];
const berlinDay = (ms) => new Date(ms).toLocaleDateString('sv-SE', { timeZone: 'Europe/Berlin' });

async function dailyState(uuid) {
  const c = (await getJson(`daily/${uuid}.json`)) || { last: null, streak: 0, total: 0 };
  const today = berlinDay(Date.now());
  const yesterday = berlinDay(Date.now() - 86400000);
  const claimedToday = c.last === today;
  const alive = claimedToday || c.last === yesterday;
  const streak = alive ? c.streak : 0;
  const nextIndex = claimedToday ? (streak - 1) % 7 : streak % 7;
  return { c, today, claimedToday, streak, nextIndex };
}

export default async function handler(req, res) {
  cors(res);
  if (req.method === 'OPTIONS') return res.status(204).end();
  const action = String((req.query && req.query.action) || '');

  if (action === 'login' && req.method === 'POST') {
    const code = String(readBody(req).code || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
    if (code.length < 6) return res.status(400).json({ error: 'invalid' });
    const key = `codes/${sha(code)}.json`;
    const c = await getJson(key);
    if (!c || !c.exp || c.exp < Date.now()) return res.status(400).json({ error: 'invalid' });
    await delPath(key);
    const token = crypto.randomBytes(32).toString('hex');
    await putJson(`tokens/${sha(token)}.json`, { uuid: c.uuid, name: c.name, createdAt: Date.now() });
    return res.json({ token, uuid: c.uuid, name: c.name });
  }

  if (action === 'me' && req.method === 'GET') {
    const t = await auth(req);
    if (!t) return res.status(401).json({ error: 'auth' });
    const p = await getJson(`players/${t.uuid}.json`);
    res.setHeader('Cache-Control', 'no-store');
    return res.json({ uuid: t.uuid, name: (p && p.name) || t.name, stats: p });
  }

  if (action === 'logout' && req.method === 'POST') {
    const tok = bearer(req);
    if (tok.length === 64) await delPath(`tokens/${sha(tok)}.json`);
    return res.json({ ok: true });
  }

  if (action === 'online' && req.method === 'GET') {
    const o = await getJson('server/online.json');
    res.setHeader('Cache-Control', 's-maxage=30, stale-while-revalidate=120');
    const fresh = o && Date.now() - o.updatedAt < 15 * 60 * 1000;
    return res.json({ names: fresh ? o.names : [], updatedAt: o ? o.updatedAt : null });
  }

  if (action === 'discord' && req.method === 'GET') {
    let out = { members: null, online: null };
    const UA = { 'User-Agent': 'DiscordBot (https://store.chickensmp.net, 1.0)', Accept: 'application/json' };
    try {
      const r = await fetch('https://discord.com/api/v10/invites/AfTVp4f88?with_counts=true', { headers: UA });
      if (r.ok) {
        const d = await r.json();
        out = { members: d.approximate_member_count ?? null, online: d.approximate_presence_count ?? null };
      }
    } catch { /* blocked */ }
    if (out.members != null) {
      putJson('cache/discord.json', { ...out, at: Date.now() }).catch(() => {});
    } else {
      // Fallbacks: counts written every few minutes by a GitHub Action, then our last good value
      try {
        const r = await fetch(`https://raw.githubusercontent.com/virtualchickenfr/Chickensmp-App/status/discord.json?t=${Date.now()}`);
        if (r.ok) { const d = await r.json(); if (d.members != null) out = { members: d.members, online: d.online ?? null }; }
      } catch { /* ignore */ }
      if (out.members == null) {
        const c = await getJson('cache/discord.json');
        if (c && c.members != null) out = { members: c.members, online: c.online ?? null };
      }
    }
    res.setHeader('Cache-Control', out.members != null ? 's-maxage=300, stale-while-revalidate=900' : 'no-store');
    return res.json(out);
  }

  if (action === 'daily' && req.method === 'GET') {
    const t = await auth(req);
    if (!t) return res.status(401).json({ error: 'auth' });
    const st = await dailyState(t.uuid);
    const rw = (await getJson(`rewards/${t.uuid}.json`)) || { items: [] };
    res.setHeader('Cache-Control', 'no-store');
    return res.json({ rewards: DAILY, claimedToday: st.claimedToday, streak: st.streak, nextIndex: st.nextIndex, total: st.c.total || 0, undelivered: rw.items.filter((i) => !i.delivered).length });
  }

  if (action === 'daily' && req.method === 'POST') {
    const t = await auth(req);
    if (!t) return res.status(401).json({ error: 'auth' });
    const st = await dailyState(t.uuid);
    if (st.claimedToday) return res.status(409).json({ error: 'claimed', streak: st.streak });
    const streak = st.streak + 1;
    const reward = DAILY[(streak - 1) % 7];
    await putJson(`daily/${t.uuid}.json`, { last: st.today, streak, total: (st.c.total || 0) + 1 });
    const rw = (await getJson(`rewards/${t.uuid}.json`)) || { items: [] };
    const item = { id: crypto.randomBytes(4).toString('hex'), key: reward.key, amount: reward.amount, reason: `daily-${streak}`, createdAt: Date.now(), delivered: false };
    rw.items = [item, ...rw.items].slice(0, 40);
    await putJson(`rewards/${t.uuid}.json`, rw);
    const idx = (await getJson('rewards/pending.json')) || { uuids: [] };
    if (!idx.uuids.includes(t.uuid)) { idx.uuids.push(t.uuid); await putJson('rewards/pending.json', idx); }
    return res.json({ ok: true, streak, reward, nextIndex: (streak - 1) % 7 });
  }

  if (action === 'tickets' && req.method === 'GET') {
    const t = await auth(req);
    if (!t) return res.status(401).json({ error: 'auth' });
    const idx = (await getJson(`tickets/${t.uuid}/index.json`)) || { tickets: [] };
    res.setHeader('Cache-Control', 'no-store');
    return res.json({ tickets: idx.tickets });
  }

  if (action === 'ticket' && req.method === 'POST') {
    const t = await auth(req);
    if (!t) return res.status(401).json({ error: 'auth' });
    const b = readBody(req);
    const category = CATS[b.category] ? b.category : 'other';
    const subject = clip(b.subject, 80).trim();
    const message = clip(b.message, 1500).trim();
    if (subject.length < 3 || message.length < 10) return res.status(400).json({ error: 'short' });
    const idx = (await getJson(`tickets/${t.uuid}/index.json`)) || { tickets: [] };
    const recent = idx.tickets.filter((x) => Date.now() - x.createdAt < 30 * 60 * 1000);
    if (recent.length >= 3) return res.status(429).json({ error: 'limit' });
    const player = await getJson(`players/${t.uuid}.json`);
    const ticket = {
      id: crypto.randomBytes(3).toString('hex').toUpperCase(),
      uuid: t.uuid, name: (player && player.name) || t.name,
      category, subject, message,
      discord: clip(b.discord, 40).trim(), target: clip(b.target, 32).trim(),
      createdAt: Date.now(), status: 'open',
    };
    ticket.delivered = await sendToDiscord(ticket, player);
    await putJson(`tickets/${t.uuid}/${ticket.id}.json`, ticket);
    idx.tickets = [{ id: ticket.id, category, subject, createdAt: ticket.createdAt, status: 'open', delivered: ticket.delivered }, ...idx.tickets].slice(0, 50);
    await putJson(`tickets/${t.uuid}/index.json`, idx);
    return res.json({ ok: true, id: ticket.id, delivered: ticket.delivered });
  }

  if (action === 'leaderboard' && req.method === 'GET') {
    const players = await listPlayers();
    const top = (key) => players.filter((p) => p[key] > 0).sort((a, b) => b[key] - a[key]).slice(0, 10).map((p) => ({ name: p.name, uuid: p.uuid, value: p[key], rank: p.rank || null }));
    res.setHeader('Cache-Control', 's-maxage=120, stale-while-revalidate=600');
    return res.json({
      players: players.length,
      playtime: top('playtime'), playerKills: top('playerKills'), mobKills: top('mobKills'),
      balance: top('balance'), updatedAt: Date.now(),
    });
  }

  return res.status(404).json({ error: 'not found' });
}
