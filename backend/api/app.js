// Used by the ChickenSMP app: login with in-game code, profile, logout, leaderboards.
import crypto from 'node:crypto';
import { getJson, putJson, delPath, sha, readBody, listPlayers } from './_store.js';

function cors(res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Headers', 'Authorization, Content-Type');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
}
const bearer = (req) => String(req.headers.authorization || '').replace(/^Bearer\s+/i, '').trim();

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
    const tok = bearer(req);
    if (tok.length !== 64) return res.status(401).json({ error: 'auth' });
    const t = await getJson(`tokens/${sha(tok)}.json`);
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

  if (action === 'leaderboard' && req.method === 'GET') {
    const blobs = await listPlayers();
    const players = (await Promise.all(blobs.map((b) => fetch(b.url).then((r) => (r.ok ? r.json() : null)).catch(() => null)))).filter(Boolean);
    const top = (key) => players.filter((p) => p[key] > 0).sort((a, b) => b[key] - a[key]).slice(0, 10).map((p) => ({ name: p.name, uuid: p.uuid, value: p[key] }));
    res.setHeader('Cache-Control', 's-maxage=120, stale-while-revalidate=600');
    return res.json({
      players: players.length,
      playtime: top('playtime'), playerKills: top('playerKills'), mobKills: top('mobKills'),
      balance: top('balance'), updatedAt: Date.now(),
    });
  }

  return res.status(404).json({ error: 'not found' });
}
