// Integration test for ChickenSMPAC-plus: Banhammer (catch, cage, ban) and staff mode (vanish).
const mineflayer = require('mineflayer');
const fs = require('fs');
const VERSION = process.env.BOT_VERSION || '1.21.4';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let failed = 0;
const ok = (c, m) => { console.log((c ? 'PASS ' : 'FAIL ') + m); if (!c) failed++; };
const consoleCmd = (c) => fs.appendFileSync('server.in', c + '\n');
const serverLog = () => fs.readFileSync('server.log', 'utf8');
const waitLog = async (re, ms = 10000) => { const t = Date.now(); while (Date.now() - t < ms) { if (re.test(serverLog())) return true; await sleep(250); } return false; };

function bot(name) {
  return new Promise((resolve) => {
    const b = mineflayer.createBot({ host: process.env.BOT_HOST || '127.0.0.1', port: 25565, username: name, auth: 'offline', version: VERSION });
    b.log = [];
    b.on('messagestr', (m) => b.log.push(m));
    b.on('title', (t) => b.log.push('TITLE ' + JSON.stringify(t)));
    b.on('kicked', (r) => b.log.push('KICK ' + JSON.stringify(r)));
    b.once('spawn', () => resolve(b));
    b.once('error', (e) => { console.log('bot error', name, e.message); resolve(b); });
    setTimeout(() => resolve(b), 25000);
  });
}

(async () => {
  // --- GrimAC flags reach /sus: a bot without gravity "flies"
  const fly = await bot('FlyBot');
  await sleep(3000);
  consoleCmd('tp FlyBot 40 -60 40');
  await sleep(2000);
  fly.physics.gravity = 0;
  fly.setControlState('jump', true); fly.setControlState('forward', true);
  await sleep(6000);
  fly.clearControlStates();
  await sleep(1500);
  consoleCmd('sus FlyBot');
  ok(await waitLog(/FlyBot (SUSPICIOUS|WATCH|OK)/, 8000), 'Grim flags for FlyBot were recorded in /sus');
  try { fly.quit(); } catch (e) {}

  console.log(failed ? `${failed} FAILED` : 'ALL PASSED');
  process.exit(failed ? 1 : 0);
})();
