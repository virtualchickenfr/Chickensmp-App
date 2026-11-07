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
  report: { label: 'Spieler melden', color: 0xff5a6a },
  purchase: { label: 'Kauf / Rang', color: 0x3ddc84 },
  appeal: { label: 'Entbannung', color: 0xa855f7 },
  other: { label: 'Sonstiges', color: 0x9a9a9a },
};
const clip = (v, n) => String(v || '').replace(/\s+$/g, '').slice(0, n);
const noPing = (v) => v.replace(/@(everyone|here)/gi, '@​$1');

async function sendToDiscord(t, player) {
  const hook = process.env.DISCORD_TICKET_WEBHOOK;
  if (!hook || !/^https:\/\/(discord|discordapp)\.com\/api\/webhooks\//.test(hook)) return false;
  const cat = CATS[t.category] || CATS.other;
  const fields = [
    { name: 'Spieler', value: noPing(t.name) || '–', inline: true },
    { name: 'Rang', value: noPing((player && player.rank) || '–'), inline: true },
    { name: 'Discord', value: noPing(t.discord || '–'), inline: true },
  ];
  if (t.target) fields.push({ name: 'Gemeldeter Spieler', value: noPing(t.target), inline: true });
  if (player) fields.push({ name: 'Status', value: player.online ? 'Gerade online' : 'Offline', inline: true });
  const body = {
    username: 'ChickenSMP App',
    allowed_mentions: { parse: [] },
    embeds: [{
      title: `🎫 ${cat.label} · #${t.id}`,
      description: `**${noPing(t.subject)}**\n\n${noPing(t.message)}`.slice(0, 3900),
      color: cat.color,
      fields,
      thumbnail: { url: `https://mc-heads.net/avatar/${t.uuid}/64` },
      footer: { text: 'Erstellt in der ChickenSMP-App' },
      timestamp: new Date(t.createdAt).toISOString(),
    }],
  };
  try {
    const r = await fetch(hook, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    return r.ok;
  } catch {
    return false;
  }
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
    const blobs = await listPlayers();
    const players = (await Promise.all(blobs.map((b) => fetch(b.url).then((r) => (r.ok ? r.json() : null)).catch(() => null)))).filter(Boolean);
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
