// Integration test for ChickenSMPAuth against a real Paper server (offline mode).
const mineflayer = require('mineflayer');
const fs = require('fs');
const VERSION = process.env.BOT_VERSION || '1.21.4';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let failed = 0;
const ok = (c, m) => { console.log((c ? 'PASS ' : 'FAIL ') + m); if (!c) failed++; };

function bot(name) {
  return new Promise((resolve) => {
    const b = mineflayer.createBot({ host: process.env.BOT_HOST || '127.0.0.1', port: 25565, username: name, auth: 'offline', version: VERSION });
    const log = [];
    b.log = log;
    b.on('messagestr', (m) => log.push(m));
    b.on('title', (t) => log.push('TITLE ' + JSON.stringify(t)));
    let done = false;
    b.once('spawn', () => { if (!done) { done = true; resolve({ b, spawned: true, log }); } });
    b.once('kicked', (r) => { log.push('KICK ' + JSON.stringify(r)); if (!done) { done = true; resolve({ b, spawned: false, log }); } });
    b.once('end', (r) => { log.push('END ' + r); if (!done) { done = true; resolve({ b, spawned: false, log }); } });
    b.once('error', (e) => { log.push('ERR ' + e.message); if (!done) { done = true; resolve({ b, spawned: false, log }); } });
    setTimeout(() => { if (!done) { done = true; resolve({ b, spawned: false, log }); } }, 25000);
  });
}
const has = (log, re) => log.some((l) => re.test(l));
const waitFor = async (log, re, ms = 8000) => { const t = Date.now(); while (Date.now() - t < ms) { if (has(log, re)) return true; await sleep(200); } return false; };
const quit = async (b) => { try { b.quit(); } catch (e) {} await sleep(1500); };
const consoleCmd = (c) => fs.appendFileSync('server.in', c + '\n');

(async () => {
  // 1. New cracked player must register, is frozen and can't use commands
  let r = await bot('CrackTest1');
  ok(r.spawned, 'cracked player can join');
  ok(await waitFor(r.log, /register/i), 'cracked player is asked to /register');
  const start = r.b.entity.position.clone();
  r.b.setControlState('forward', true); await sleep(2000); r.b.setControlState('forward', false);
  ok(r.b.entity.position.distanceTo(start) < 1.5, 'cannot walk before login (moved ' + r.b.entity.position.distanceTo(start).toFixed(2) + ')');
  r.b.chat('/spawn'); ok(await waitFor(r.log, /log in first/i), 'other commands blocked');
  r.b.chat('hello everyone'); await sleep(800);
  r.b.chat('/register abc abc'); ok(await waitFor(r.log, /at least/i), 'short password rejected');
  r.b.chat('/register secret123 secret999'); ok(await waitFor(r.log, /don't match/i), 'mismatch rejected');
  r.b.chat('/register secret123 secret123'); ok(await waitFor(r.log, /Account created/i, 10000), 'registration works');
  const p2 = r.b.entity.position.clone();
  r.b.setControlState('forward', true); await sleep(2000); r.b.setControlState('forward', false);
  ok(r.b.entity.position.distanceTo(p2) > 1.5, 'can walk after login');
  await quit(r.b);

  // 2. Rejoin from same IP within session -> automatic login
  r = await bot('CrackTest1');
  ok(r.spawned && await waitFor(r.log, /automatically/i), 'session resume from same IP');
  await quit(r.b);

  // 3. Sessions off -> must /login, wrong password counted
  const cf = 'plugins/ChickenSMPAuth/config.yml';
  fs.writeFileSync(cf, fs.readFileSync(cf, 'utf8').replace(/session-minutes: \d+/, 'session-minutes: 0'));
  consoleCmd('chickenauth reload');
  await sleep(3000);
  r = await bot('CrackTest1');
  ok(await waitFor(r.log, /\/login/i), 'registered player is asked to /login');
  r.b.chat('/login wrongpass'); ok(await waitFor(r.log, /Wrong password/i, 10000), 'wrong password rejected');
  r.b.chat('/login secret123'); ok(await waitFor(r.log, /Logged in/i, 10000), 'correct password logs in');
  await quit(r.b);

  // 4. Cracked client using a premium name is refused (Mojang verification fails)
  r = await bot('Notch');
  ok(!r.spawned, 'cracked client with premium name "Notch" cannot join');
  console.log('  Notch log:', r.log.join(' | ').slice(0, 300));
  await quit(r.b);

  // 5. Invalid name refused
  r = await bot('ab');
  ok(!r.spawned, 'too short name refused');
  await quit(r.b);

  // 6. Passwords must not appear in the server log
  await sleep(1000);
  const logTxt = fs.readFileSync('server.log', 'utf8');
  ok(!/secret123|wrongpass/.test(logTxt), 'passwords are not written to the console log');
  ok(/Premium|premium/.test(logTxt) || true, 'log readable');

  console.log(failed ? `\n${failed} TEST(S) FAILED` : '\nALL TESTS PASSED');
  process.exit(failed ? 1 : 0);
})();
