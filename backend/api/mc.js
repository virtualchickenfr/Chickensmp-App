// Called by the ChickenSMP server plugin (outbound only, shared secret).
import { getJson, putJson, sha, readBody } from './_store.js';

const lc = (u) => String(u).toLowerCase();

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
// Only the ChickenSMP game server may call this endpoint.
const ALLOWED_IPS = (process.env.MC_SERVER_IPS || '208.115.203.109').split(',').map((x) => x.trim());
const num = (v) => (Number.isFinite(Number(v)) ? Number(v) : 0);

function cleanPlayer(p) {
  return {
    uuid: String(p.uuid).toLowerCase(),
    name: String(p.name || '').slice(0, 32),
    online: !!p.online,
    bedrock: !!p.bedrock,
    rank: p.rank ? String(p.rank).slice(0, 32) : null,
    groups: Array.isArray(p.groups) ? p.groups.slice(0, 10).map((g) => String(g).slice(0, 32)) : [],
    prefix: p.prefix ? String(p.prefix).slice(0, 40) : null,
    balance: p.balance == null ? null : num(p.balance),
    playtime: num(p.playtime),
    deaths: num(p.deaths),
    playerKills: num(p.playerKills),
    mobKills: num(p.mobKills),
    jumps: num(p.jumps),
    walkCm: num(p.walkCm),
    joins: num(p.joins),
    firstPlayed: num(p.firstPlayed),
    lastSeen: num(p.lastSeen),
    updatedAt: Date.now(),
  };
}

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'method' });
  const ip = String(req.headers['x-real-ip'] || String(req.headers['x-forwarded-for'] || '').split(',')[0]).trim();
  if (!ALLOWED_IPS.includes(ip)) return res.status(403).json({ error: 'ip', ip });
  const b = readBody(req);
  const secret = String(b.secret || '');
  if (secret.length < 32) return res.status(401).json({ error: 'secret' });

  // First server to connect registers its secret (trust on first use).
  const reg = await getJson('server/secret.json');
  if (!reg) await putJson('server/secret.json', { hash: sha(secret), createdAt: Date.now() });
  else if (reg.hash !== sha(secret)) return res.status(401).json({ error: 'secret' });

  if (b.action === 'hello') return res.json({ ok: true });

  if (b.action === 'code') {
    const code = String(b.code || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
    if (code.length < 6 || !UUID.test(String(b.uuid))) return res.status(400).json({ error: 'bad' });
    await putJson(`codes/${sha(code)}.json`, { uuid: String(b.uuid).toLowerCase(), name: String(b.name || ''), exp: Date.now() + 10 * 60 * 1000 });
    return res.json({ ok: true });
  }

  if (b.action === 'stats') {
    const players = (Array.isArray(b.players) ? b.players : []).filter((p) => p && UUID.test(String(p.uuid))).slice(0, 300);
    await Promise.all(players.map((p) => putJson(`players/${String(p.uuid).toLowerCase()}.json`, cleanPlayer(p))));
    if (Array.isArray(b.onlineNames)) await putJson('server/online.json', { names: b.onlineNames.slice(0, 200).map(String), updatedAt: Date.now() });
    return res.json({ ok: true, count: players.length });
  }

  // Rewards waiting for players who are online right now.
  if (b.action === 'pending') {
    const online = (Array.isArray(b.online) ? b.online : []).map(lc);
    const idx = (await getJson('rewards/pending.json')) || { uuids: [] };
    const want = idx.uuids.filter((u) => online.includes(u));
    const out = [];
    for (const u of want) {
      const rw = await getJson(`rewards/${u}.json`);
      if (rw) for (const i of rw.items) if (!i.delivered) out.push({ uuid: u, id: i.id, key: i.key, amount: i.amount, reason: i.reason });
    }
    return res.json({ ok: true, rewards: out });
  }

  if (b.action === 'ack') {
    const ids = new Set((Array.isArray(b.ids) ? b.ids : []).map(String));
    const uuids = [...new Set((Array.isArray(b.uuids) ? b.uuids : []).map(lc))];
    const idx = (await getJson('rewards/pending.json')) || { uuids: [] };
    for (const u of uuids) {
      const rw = await getJson(`rewards/${u}.json`);
      if (!rw) continue;
      rw.items = rw.items.map((i) => (ids.has(i.id) ? { ...i, delivered: true, deliveredAt: Date.now() } : i));
      await putJson(`rewards/${u}.json`, rw);
      if (!rw.items.some((i) => !i.delivered)) idx.uuids = idx.uuids.filter((x) => x !== u);
    }
    await putJson('rewards/pending.json', idx);
    return res.json({ ok: true });
  }

  return res.status(400).json({ error: 'action' });
}
