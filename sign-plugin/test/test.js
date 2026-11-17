// Integration test for ChickenSMPSign: /sign, daily limit, /unsign, op = unlimited.
const mineflayer = require('mineflayer');
const fs = require('fs');
const VERSION = process.env.BOT_VERSION || '1.21.4';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let failed = 0;
const ok = (c, m) => { console.log((c ? 'PASS ' : 'FAIL ') + m); if (!c) failed++; };
const consoleCmd = (c) => fs.appendFileSync('server.in', c + '\n');
const serverLog = () => fs.readFileSync('server.log', 'utf8');

function bot(name) {
  return new Promise((resolve) => {
    const b = mineflayer.createBot({ host: process.env.BOT_HOST || '127.0.0.1', port: 25565, username: name, auth: 'offline', version: VERSION });
    b.log = [];
    b.on('messagestr', (m) => b.log.push(m));
    b.once('spawn', () => resolve(b));
    b.once('error', (e) => { console.log('bot error', name, e.message); resolve(b); });
    setTimeout(() => resolve(b), 25000);
  });
}

// Run a chat command and return the chat lines it produced.
async function run(b, c) {
  const n = b.log.length;
  b.chat(c);
  await sleep(1200);
  return b.log.slice(n).join(' | ');
}

// Ask the server what the bot holds (lore is in the components).
async function held(name) {
  const n = serverLog().length;
  consoleCmd(`data get entity ${name} SelectedItem`);
  await sleep(1200);
  return serverLog().slice(n);
}

(async () => {
  const b = await bot('SignBot');
  await sleep(2000);
  consoleCmd('give SignBot diamond_sword');
  consoleCmd('give SignBot stick');
  consoleCmd('give SignBot apple');
  await sleep(1500);
  b.setQuickBarSlot(0);
  await sleep(500);

  let r = await run(b, '/sign Hello world');
  ok(/Item signed!/.test(r) && /0 left today/.test(r), 'default player signs once, 0 left: ' + r);
  let h = await held('SignBot');
  ok(/Signed by/.test(h) && /SignBot/.test(h) && /Hello world/.test(h), 'lore has signer + message');
  ok(/▬/.test(h) && /┃/.test(h), 'lore has the frame');

  r = await run(b, '/sign again');
  ok(/already signed by SignBot/.test(r), 'cannot sign twice: ' + r);

  b.setQuickBarSlot(1);
  await sleep(500);
  r = await run(b, '/sign');
  ok(/used all your signs for today \(1\)/.test(r), 'daily limit reached: ' + r);
  r = await run(b, '/unsign');
  ok(/not signed/.test(r), 'unsign on unsigned item: ' + r);

  b.setQuickBarSlot(0);
  await sleep(500);
  r = await run(b, '/unsign');
  ok(/Signature removed/.test(r), 'unsign own item: ' + r);
  h = await held('SignBot');
  ok(!/Signed by/.test(h) && !/Hello world/.test(h), 'lore removed after unsign');

  b.setQuickBarSlot(3);
  await sleep(500);
  r = await run(b, '/sign');
  ok(/Hold the item/.test(r), 'empty hand: ' + r);

  consoleCmd('op SignBot');
  await sleep(1000);
  b.setQuickBarSlot(1);
  await sleep(500);
  r = await run(b, '/sign op test');
  ok(/Item signed!/.test(r) && !/left today/.test(r), 'op is unlimited: ' + r);
  b.setQuickBarSlot(2);
  await sleep(500);
  r = await run(b, '/sign ' + 'x'.repeat(41));
  ok(/too long/.test(r), 'message length limit: ' + r);
  r = await run(b, '/sign &cred &lbold');
  h = await held('SignBot');
  ok(/Item signed!/.test(r) && /red bold/.test(h), 'colour codes stripped');

  ok(/SignBot signed DIAMOND_SWORD \[default\] \("Hello world"\)/.test(serverLog()), 'console log line');

  // --- rank: LuckPerms group with a higher limit and a gradient frame
  const rb = await bot('RankBot');
  await sleep(1500);
  consoleCmd('lp creategroup supreme');
  await sleep(800);
  consoleCmd('lp group supreme setweight 5');
  consoleCmd('lp group supreme permission set chickensmp.sign.limit.10 true');
  await sleep(800);
  consoleCmd('lp user RankBot parent add supreme');
  consoleCmd('give RankBot golden_apple');
  await sleep(2500);
  rb.setQuickBarSlot(0);
  await sleep(500);
  r = await run(rb, '/sign');
  ok(/Item signed!/.test(r) && /9 left today/.test(r), 'supreme has 10 per day: ' + r);
  h = await held('RankBot');
  ok(/ff9900/i.test(h), 'supreme frame uses the supreme colours');
  ok(/RankBot signed GOLDEN_APPLE \[supreme\]/.test(serverLog()), 'rank detected from LuckPerms');
  rb.quit();
  console.log(failed ? `${failed} FAILED` : 'ALL PASSED');
  b.quit();
  process.exit(failed ? 1 : 0);
})();
