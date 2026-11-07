// Serves the ChickenSMP web app (same code as the phone app) from the repo's `web` branch.
const RAW = 'https://raw.githubusercontent.com/virtualchickenfr/Chickensmp-App/web';
const TYPES = {
  html: 'text/html; charset=utf-8', js: 'application/javascript; charset=utf-8', css: 'text/css; charset=utf-8',
  json: 'application/json; charset=utf-8', png: 'image/png', jpg: 'image/jpeg', jpeg: 'image/jpeg', svg: 'image/svg+xml',
  ico: 'image/x-icon', ttf: 'font/ttf', otf: 'font/otf', woff: 'font/woff', woff2: 'font/woff2', webp: 'image/webp', map: 'application/json',
};

export default async function handler(req, res) {
  let p = String((req.query && req.query.p) || '').replace(/^\/+/, '');
  if (p.includes('..')) return res.status(400).end();
  if (!p || !/\.[a-z0-9]+$/i.test(p)) p = 'index.html';
  let r = await fetch(`${RAW}/${p}`);
  if (!r.ok && p !== 'index.html') { p = 'index.html'; r = await fetch(`${RAW}/index.html`); }
  if (!r.ok) return res.status(502).send('App not built yet');
  const ext = (p.split('.').pop() || '').toLowerCase();
  const hashed = /^_expo\/|^assets\//.test(p);
  res.setHeader('Content-Type', TYPES[ext] || 'application/octet-stream');
  res.setHeader('Cache-Control', hashed ? 'public, s-maxage=31536000, max-age=31536000, immutable' : 'public, s-maxage=60, stale-while-revalidate=300, max-age=0');
  return res.status(200).end(Buffer.from(await r.arrayBuffer()));
}
