const mineflayer = require('mineflayer');
const fs = require('fs');
const sleep = ms => new Promise(r => setTimeout(r, ms));
const cmd = c => fs.appendFileSync('server.in', c + '\n');
const log = () => fs.readFileSync('server.log', 'utf8');
let fails = 0;
const check = (ok, what) => { console.log((ok ? 'PASS ' : 'FAIL ') + what); if (!ok) fails++; };
function bot(name) { return new Promise((res, rej) => { const b = mineflayer.createBot({ host: '127.0.0.1', port: 25565, username: name, version: '1.21.11' }); b.once('spawn', () => res(b)); b.once('error', rej); }); }
async function count(type) { const mark = log().length; cmd(`execute if entity @e[type=${type}]`); await sleep(500); const m = log().slice(mark).match(/Test passed\. Count: (\d+)|Test failed/i); return m && m[1] ? +m[1] : 0; }
const ids = process.argv.slice(2);
(async () => {
  const startLen = log().length;
  const a = await bot('Killer'); const v = await bot('Victim');
  cmd('op Killer'); cmd('difficulty peaceful'); cmd('tp Killer 0 -60 0'); cmd('tp Victim 0 -60 4'); cmd('time set day');
  await sleep(1500);
  cmd('ke reload'); await sleep(1000);
  for (const id of ids) {
    const before = log().length;
    a.chat('/ke play ' + id);
    await sleep(700);
    const mark = log().length;
    for (const t of ['block_display', 'item_display', 'mannequin', 'text_display']) cmd(`execute if entity @e[type=${t}]`);
    await sleep(600);
    const n = [...log().slice(mark).matchAll(/Test passed\. Count: (\d+)/g)].reduce((s, m) => s + +m[1], 0);
    const seg = log().slice(before);
    const failed = /failed: |Exception|Unknown effect|not found/i.test(seg);
    check(n > 0 && !failed, `play ${id}: ${n} entities` + (failed ? ' ERR ' + seg.slice(0, 300) : ''));
    await sleep(5200);
  }
  await sleep(6000);
  const left = await count('block_display') + await count('item_display') + await count('mannequin') + await count('text_display');
  check(left === 0, 'all entities removed afterwards (' + left + ')');
  cmd('ke give Killer all'); await sleep(500);
  a.chat('/ke select ' + ids[ids.length - 1]); await sleep(800);
  cmd('damage Victim 1000 minecraft:player_attack by Killer'); await sleep(2500);
  check(await count('mannequin') + await count('block_display') > 0, 'real kill plays ' + ids[ids.length - 1]);
  check(!/Exception/.test(log().slice(startLen)), 'no exceptions in the log');
  console.log(fails ? `${fails} FAILED` : 'ALL PASSED');
  a.quit(); v.quit(); process.exit(fails ? 1 : 0);
})().catch(e => { console.error(e); process.exit(1); });
