// Mineflayer test for ChickenSMPStocks 3 (ore market). Run next to server.in / server.log.
const mineflayer = require('mineflayer');
const fs = require('fs');
const sleep = ms => new Promise(r => setTimeout(r, ms));
let fail = 0;
const ok = (c, m) => { console.log((c ? 'PASS ' : 'FAIL ') + m); if (!c) fail++; };
const con = c => fs.appendFileSync('server.in', c + '\n');
const log = () => fs.readFileSync('server.log', 'utf8');
const NAME = 'StockBot';
const b = mineflayer.createBot({ host: process.env.BOT_HOST || '127.0.0.1', port: +(process.env.BOT_PORT || 25565), username: NAME, auth: 'offline', version: '1.21.11' });
const msgs = [];
b.on('messagestr', m => msgs.push(m));
const said = re => msgs.some(m => re.test(m));
const win = () => new Promise(r => { b.once('windowOpen', r); setTimeout(() => r(null), 5000); });
const title = w => { try { return JSON.stringify(w.title); } catch { return String(w.title); } };
const lore = it => { try { return JSON.stringify(it.components || it.nbt); } catch { return ''; } };
async function click(w, slot) { const n = win(); await b.clickWindow(slot, 0, 0); return n; }

b.once('spawn', async () => {
  try {
    await sleep(4000);
    ok(said(/old stocks were sold/), 'old stocks were paid out and the player was told');
    con('op ' + NAME);
    await sleep(1000);
    b.chat('/stocks');
    let w = await win();
    ok(w && title(w).includes('ORE MARKET'), 'market opens ' + (w && title(w)));
    const ores = [20, 21, 22, 23, 24].map(s => w.slots[s] && w.slots[s].name);
    ok(JSON.stringify(ores) === JSON.stringify(['coal', 'iron_ingot', 'gold_ingot', 'diamond', 'netherite_ingot']), '5 ores shown ' + ores);
    ok(w.slots[4] && w.slots[4].name === 'book' && lore(w.slots[4]).includes('HOW IT WORKS'), 'how it works book');
    ok(w.slots[38] && w.slots[38].name === 'chest', 'your ores item');
    ok(w.slots[42] && w.slots[42].name === 'nether_star', 'top traders item');
    // taking an item must not work
    await b.clickWindow(10, 0, 1); await sleep(500);
    ok(!b.inventory.items().some(i => i.name.includes('glass_pane')), 'cannot take items out');

    w = await click(w, 22); // gold
    ok(w && title(w).includes('GOLD'), 'gold screen opens ' + (w && title(w)));
    await b.clickWindow(29, 0, 0); await sleep(800); // buy 10
    ok(said(/Bought 10 GOLD/i), 'bought 10 gold');
    await b.clickWindow(30, 0, 0); await sleep(800); // buy 64
    ok(said(/Bought 64 GOLD/i), 'bought 64 gold');
    w = b.currentWindow;
    ok(w && w.slots[13] && w.slots[13].count === 64 && lore(w.slots[13]).includes('74'), 'screen shows 74 owned');
    await b.clickWindow(32, 0, 0); await sleep(800); // sell 1
    ok(said(/Sold 1 GOLD/i), 'sold 1 gold');
    await b.clickWindow(34, 0, 0); await sleep(800); // sell all
    ok(said(/Sold 73 GOLD/i), 'sold all 73 gold');
    await b.clickWindow(32, 0, 0); await sleep(800);
    ok(said(/don't have that much/i), 'selling with nothing is refused');

    // max: netherite max 25
    w = await click(b.currentWindow, 40);
    ok(w && title(w).includes('ORE MARKET'), 'back to market');
    w = await click(w, 24);
    await b.clickWindow(30, 0, 0); await sleep(800);
    ok(said(/Bought 25 NETHERITE/i), 'buy 64 netherite is capped at 25');
    await b.clickWindow(28, 0, 0); await sleep(800);
    ok(said(/at most 25/i), 'max message');
    b.closeWindow(b.currentWindow); await sleep(500);

    // admin
    b.chat('/stocks news coal up'); await sleep(800);
    ok(said(/ORE NEWS/), 'news broadcast');
    b.chat('/stocks set diamond 5000'); await sleep(800);
    ok(said(/Set DIAMOND to \$5,000.00/i), 'admin set');
    b.chat('/stocks reload'); await sleep(800);
    ok(said(/Config reloaded/), 'reload');
    b.chat('/stocks'); w = await win();
    ok(w && lore(w.slots[42]).includes(NAME), 'top traders lists the bot');
    ok(lore(w.slots[23]).includes('5,000.00'), 'diamond price shown as 5,000');
    b.closeWindow(w);
  } catch (e) { console.log('ERROR', e); fail++; }
  await sleep(500);
  const l = log();
  ok(/Paid out old stock fish x10/.test(l) && /Paid out old stock crate x2/.test(l), 'console logs the payouts');
  ok(!/Exception[^\n]*\n(\s+at [^\n]*\n)*[^\n]*stocks/i.test(l) && !/net\.chickensmp\.stocks/.test(l), 'no exceptions from the plugin');
  console.log(msgs.filter(m => /ORES|NEWS/.test(m)).join('\n'));
  console.log(fail ? 'FAILED ' + fail : 'ALL PASSED');
  b.quit(); process.exit(fail ? 1 : 0);
});
b.on('kicked', r => { console.log('kicked', r); process.exit(1); });
b.on('error', e => { console.log('error', e); });
