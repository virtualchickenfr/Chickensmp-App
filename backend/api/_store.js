// Storage on Upstash Redis (REST). Replaces Vercel Blob, whose free plan was far too small.
// Players live in one hash ("players", field = uuid) so the server's stats sync is a single command.
import crypto from 'node:crypto';

const URL_ = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL || '';
const TOKEN = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN || '';

export const sha = (s) => crypto.createHash('sha256').update(String(s)).digest('hex');

async function cmd(...args) {
  if (!URL_ || !TOKEN) throw new Error('redis not configured');
  const r = await fetch(URL_, {
    method: 'POST',
    headers: { Authorization: `Bearer ${TOKEN}`, 'Content-Type': 'application/json' },
    body: JSON.stringify(args),
  });
  const d = await r.json();
  if (d.error) throw new Error(d.error);
  return d.result;
}

const parse = (v) => { if (v == null) return null; try { return JSON.parse(v); } catch { return null; } };
const playerId = (path) => { const m = /^players\/([^/]+)\.json$/.exec(path); return m ? m[1] : null; };

export async function getJson(path) {
  const pid = playerId(path);
  return parse(pid ? await cmd('HGET', 'players', pid) : await cmd('GET', path));
}

// opts.ex = expiry in seconds
export async function putJson(path, obj, opts = {}) {
  const pid = playerId(path);
  if (pid) return cmd('HSET', 'players', pid, JSON.stringify(obj));
  return opts.ex ? cmd('SET', path, JSON.stringify(obj), 'EX', String(opts.ex)) : cmd('SET', path, JSON.stringify(obj));
}

export async function putPlayers(list) {
  if (!list.length) return 0;
  const args = ['HSET', 'players'];
  for (const p of list) args.push(p.uuid, JSON.stringify(p));
  return cmd(...args);
}

export async function delPath(path) {
  const pid = playerId(path);
  try { return pid ? await cmd('HDEL', 'players', pid) : await cmd('DEL', path); } catch { return 0; }
}

// All synced players as objects
export async function listPlayers() {
  const vals = (await cmd('HVALS', 'players')) || [];
  return vals.map(parse).filter(Boolean);
}

export function readBody(req) {
  if (req.body && typeof req.body === 'object') return req.body;
  try { return JSON.parse(req.body || '{}'); } catch { return {}; }
}
