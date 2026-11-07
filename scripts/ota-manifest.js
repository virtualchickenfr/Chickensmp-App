// Builds an Expo Updates (protocol 1) manifest for an `expo export --platform android` output.
// usage: node ota-manifest.js <distDir> <baseUrl> <runtimeVersion> <appConfig.json>
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const [dist, base, runtime, cfgFile] = process.argv.slice(2);
const meta = JSON.parse(fs.readFileSync(path.join(dist, 'metadata.json'), 'utf8')).fileMetadata.android;
const MIME = { png: 'image/png', jpg: 'image/jpeg', jpeg: 'image/jpeg', gif: 'image/gif', webp: 'image/webp', ttf: 'font/ttf', otf: 'font/otf', json: 'application/json', mp3: 'audio/mpeg', wav: 'audio/wav' };
const b64url = (b) => b.toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

function asset(rel, ext, launch) {
  const buf = fs.readFileSync(path.join(dist, rel));
  return {
    hash: b64url(crypto.createHash('sha256').update(buf).digest()),
    key: crypto.createHash('md5').update(buf).digest('hex'),
    fileExtension: launch ? '.bundle' : `.${ext}`,
    contentType: launch ? 'application/javascript' : (MIME[ext] || 'application/octet-stream'),
    url: `${base}/${rel}`,
  };
}

const launchAsset = asset(meta.bundle, 'bundle', true);
const assets = meta.assets.map((a) => asset(a.path, a.ext, false));
const h = crypto.createHash('sha256').update(launchAsset.hash + assets.map((a) => a.hash).join('')).digest('hex');
const id = `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`;
const cfg = JSON.parse(fs.readFileSync(cfgFile, 'utf8'));
const manifest = { id, createdAt: new Date().toISOString(), runtimeVersion: String(runtime), launchAsset, assets, metadata: {}, extra: { expoClient: cfg } };
fs.writeFileSync(path.join(dist, 'manifest.json'), JSON.stringify(manifest));
console.log(`OTA manifest ${id}: ${assets.length} assets, runtime ${runtime}`);
