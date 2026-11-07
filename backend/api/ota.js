// Expo Updates endpoint: the app asks here on start whether newer code exists.
import crypto from 'node:crypto';

const RAW = 'https://raw.githubusercontent.com/virtualchickenfr/Chickensmp-App/ota';

export default async function handler(req, res) {
  const runtime = String(req.headers['expo-runtime-version'] || (req.query && req.query['runtime-version']) || '');
  const platform = String(req.headers['expo-platform'] || (req.query && req.query.platform) || '');
  res.setHeader('expo-protocol-version', '1');
  res.setHeader('expo-sfv-version', '0');
  res.setHeader('cache-control', 'private, max-age=0');
  let m = null;
  try {
    const r = await fetch(`${RAW}/manifest.json?t=${Date.now()}`, { cache: 'no-store' });
    if (r.ok) m = await r.json();
  } catch { /* none */ }
  if (!m || platform !== 'android' || String(m.runtimeVersion) !== runtime) return res.status(204).end();
  const boundary = `csmp${crypto.randomBytes(8).toString('hex')}`;
  const part = (name, obj) => `--${boundary}\r\nContent-Disposition: form-data; name="${name}"\r\nContent-Type: application/json; charset=utf-8\r\n\r\n${JSON.stringify(obj)}\r\n`;
  const body = part('manifest', m) + part('extensions', { assetRequestHeaders: {} }) + `--${boundary}--\r\n`;
  res.setHeader('content-type', `multipart/mixed; boundary=${boundary}`);
  return res.status(200).send(body);
}
