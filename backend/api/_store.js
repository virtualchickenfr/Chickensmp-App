import { put, del, list } from '@vercel/blob';
import crypto from 'node:crypto';

const TOKEN = process.env.BLOB_READ_WRITE_TOKEN || '';
const STORE = (TOKEN.split('_')[3] || '').toLowerCase();
export const BASE = `https://${STORE}.public.blob.vercel-storage.com`;

export const sha = (s) => crypto.createHash('sha256').update(String(s)).digest('hex');

export async function getJson(path) {
  const r = await fetch(`${BASE}/${path}?t=${Date.now()}`, { cache: 'no-store' });
  if (!r.ok) return null;
  try { return await r.json(); } catch { return null; }
}

export async function putJson(path, obj) {
  await put(path, JSON.stringify(obj), {
    access: 'public', addRandomSuffix: false, allowOverwrite: true,
    contentType: 'application/json', cacheControlMaxAge: 60,
  });
}

export async function delPath(path) {
  try { await del(`${BASE}/${path}`); } catch { /* already gone */ }
}

export async function listPlayers() {
  const out = [];
  let cursor;
  do {
    const r = await list({ prefix: 'players/', limit: 1000, cursor });
    out.push(...r.blobs);
    cursor = r.hasMore ? r.cursor : undefined;
  } while (cursor && out.length < 5000);
  return out;
}

export function readBody(req) {
  if (req.body && typeof req.body === 'object') return req.body;
  try { return JSON.parse(req.body || '{}'); } catch { return {}; }
}
