// Bot test for ChickenSMPKillEffects: plays every effect, a preview and a real kill.
const mineflayer = require('mineflayer');
const fs = require('fs');
const host = process.env.BOT_HOST || '127.0.0.1';
const sleep = ms => new Promise(r => setTimeout(r, ms));
const cmd = c => fs.appendFileSync('server.in', c + '\n');
const log = () => fs.readFileSync('server.log', 'utf8');
let fails = 0;
const check = (ok, what) => { console.log((ok ? 'PASS ' : 'FAIL ') + what); if (!ok) fails++; };

function bot(name) {
  return new Promise((res, rej) => {
    const b = mineflayer.createBot({ host, port: 25565, username: name, version: '1.21.11' });
    b.once('spawn', () => res(b));
    b.once('error', rej);
    b.once('kicked', r => rej(new Error('kicked ' + JSON.stringify(r))));
  });
}

async function count(type) {
  const mark = log().length;
  const near = ['chicken', 'phantom'].includes(type) ? ',x=0,y=-60,z=0,distance=..40' : '';
  cmd(`execute if entity @e[type=${type}${near}]`);
  await sleep(600);
  const m = log().slice(mark).match(/Test passed\. Count: (\d+)|Test failed/i);
  return m && m[1] ? +m[1] : 0;
}

(async () => {
  const startLen = log().length;
  const a = await bot('Killer');
  const v = await bot('Victim');
  cmd('op Killer'); cmd('difficulty peaceful'); cmd('gamerule doMobSpawning false'); cmd('kill @e[type=chicken]');
  cmd('tp Killer 0 -60 0'); cmd('tp Victim 0 -60 4');
  await sleep(1500);
  const all = process.argv.length > 2 ? process.argv.slice(2) : ['tombstone', 'anvil', 'bone_pile', 'ez', 'confetti_pop', 'ice_cube', 'tnt', 'ground_slam', 'rocket', 'vortex', 'balloon',
    'explode', 'thunder_strike', 'phantom', 'meteor', 'dragon_breath', 'ascension', 'shattered', 'black_hole_x', 'judgement', 'chicken_god', 'banned'];
  cmd('ke list');
  await sleep(800);
  for (const id of all) {
    const before = log().length;
    a.chat('/ke play ' + id);
    await sleep(400);
    const n = await count('mannequin') + await count('block_display') + await count('item_display') + await count('text_display') + await count('firework_rocket');
    const failed = / failed: |Exception/.test(log().slice(before));
    check(n > 0 && !failed, `play ${id}: ${n} displays`);
    await sleep(4500);
  }
  await sleep(6000);
  const left = await count('block_display') + await count('item_display') + await count('mannequin') + await count('text_display') + await count('chicken') + await count('phantom') + await count('firework_rocket');
  check(left === 0, 'everything removed afterwards (' + left + ')');
  // preview (only the viewer)
  a.chat('/ke preview judgement');
  await sleep(1500);
  check(await count('item_display') + await count('mannequin') + await count('text_display') > 0, 'preview spawns displays');
  await sleep(9000);
  // real kill with a selected effect
  cmd('ke give Killer all');
  await sleep(500);
  a.chat('/ke select chicken_god');
  await sleep(800);
  cmd('damage Victim 1000 minecraft:player_attack by Killer');
  await sleep(2500);
  check(await count('chicken') + await count('mannequin') + await count('text_display') > 0, 'kill plays the selected effect');
  check(/Killer/.test(log()) && !/Exception/.test(log().slice(startLen)), 'no exceptions in the log');
  console.log(fails ? `${fails} FAILED` : 'ALL PASSED');
  a.quit(); v.quit();
  process.exit(fails ? 1 : 0);
})().catch(e => { console.error(e); process.exit(1); });
