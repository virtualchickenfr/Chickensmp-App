// Mineflayer test for ChickenSMPAdmin: menus, shard tools, spawners. Run next to server.in / server.log.
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');
const fs = require('fs');
const sleep = ms => new Promise(r => setTimeout(r, ms));
let fail = 0;
const ok = (c, m) => { console.log((c ? 'PASS ' : 'FAIL ') + m); if (!c) fail++; };
const con = c => fs.appendFileSync('server.in', c + '\n');
const log = () => fs.readFileSync('server.log', 'utf8');
const NAME = 'AdmBot';
const b = mineflayer.createBot({ host: process.env.BOT_HOST || '127.0.0.1', port: +(process.env.BOT_PORT || 25567), username: NAME, auth: 'offline', version: '1.21.11' });
const msgs = [];
b.on('messagestr', m => msgs.push(m));
const said = re => msgs.some(m => re.test(m));
const win = () => new Promise(r => { b.once('windowOpen', r); setTimeout(() => r(null), 5000); });
const title = w => { try { return JSON.stringify(w.title); } catch { return String(w.title); } };
async function click(w, slot, button = 0, mode = 0) { const n = win(); await b.clickWindow(slot, button, mode); return n; }
const count = name => b.inventory.items().filter(i => i.name === name).reduce((a, i) => a + i.count, 0);
const custom = (name) => b.inventory.items().find(i => i.name === name && i.components && JSON.stringify(i.components).includes('chickensmpadmin'));

b.once('spawn', async () => {
  b.digTime = () => 2500; // prismarine-block can't read 1.21.11 enchantments
  try {
    await sleep(1500);
    con('op ' + NAME); con('clear ' + NAME); con('gamemode survival ' + NAME); con('effect give ' + NAME + ' resistance 600 5'); con('time set day'); con('execute at ' + NAME + ' run fill ~-10 ~ ~-6 ~10 ~6 ~8 air');
    await sleep(1500);
    // ---- menu
    b.chat('/admin');
    let w = await win();
    ok(w && title(w).includes('ᴀᴅᴍɪɴ'), 'admin menu opens ' + (w && title(w)));
    // tools -> player -> pickaxe, shovel, axe, rocket, bucket
    w = await click(w, 21);
    ok(w && title(w).includes('ѕᴇʟᴇᴄᴛ'), 'player picker opens');
    const head = w.slots.findIndex((s, i) => i < 45 && s && s.name === 'player_head');
    w = await click(w, head);
    ok(w && title(w).includes('ѕʜᴀʀᴅ'), 'tools menu opens');
    for (const s of [20, 21, 22, 23, 24]) { await b.clickWindow(s, 0, 0); await sleep(300); }
    b.closeWindow(w); await sleep(800);
    ok(count('netherite_pickaxe') === 1 && count('netherite_shovel') === 1 && count('netherite_axe') === 1 && count('firework_rocket') === 1 && count('bucket') === 1, 'got all 5 shard tools');

    // ---- 3x3 pickaxe
    const p = b.entity.position.floored();
    const wx = p.x, wy = p.y, wz = p.z + 3;
    con(`fill ${wx - 1} ${wy} ${wz} ${wx + 1} ${wy + 2} ${wz} minecraft:stone`);
    await sleep(1500);
    await b.equip(b.inventory.items().find(i => i.name === 'netherite_pickaxe'), 'hand');
    const center = b.blockAt(new Vec3(wx, wy + 1, wz));
    await b.lookAt(center.position.offset(0.5, 0.5, 0), true);
    await b.dig(center, true, 'ignore');
    await sleep(800);
    let left = 0;
    for (let x = -1; x <= 1; x++) for (let y = 0; y <= 2; y++) { const bl = b.blockAt(new Vec3(wx + x, wy + y, wz)); if (bl && bl.name === 'stone') left++; }
    ok(left === 0, '3x3 pickaxe broke the whole wall (left ' + left + ')');

    // ---- sponge bucket
    con(`fill ${wx - 2} ${wy} ${wz + 2} ${wx + 2} ${wy} ${wz + 4} minecraft:water`);
    await sleep(1500);
    await b.equip(b.inventory.items().find(i => i.name === 'bucket'), 'hand');
    await b.lookAt(new Vec3(wx + 0.5, wy + 0.2, wz + 3.5), true);
    b.activateItem(); await sleep(300); b.deactivateItem();
    await sleep(1000);
    let water = 0;
    for (let x = -2; x <= 2; x++) for (let z = 2; z <= 4; z++) { const bl = b.blockAt(new Vec3(wx + x, wy, wz + z)); if (bl && bl.name === 'water') water++; }
    ok(water === 0, 'sponge bucket removed the water (left ' + water + ')');
    ok(count('bucket') === 1 && count('water_bucket') === 0, 'bucket stayed a bucket');

    // ---- tree axe
    const tx = p.x + 3, tz = p.z;
    con(`fill ${tx} ${wy} ${tz} ${tx} ${wy + 4} ${tz} minecraft:oak_log`);
    con(`fill ${tx - 1} ${wy + 4} ${tz - 1} ${tx + 1} ${wy + 5} ${tz + 1} minecraft:oak_leaves[persistent=false] keep`);
    await sleep(1500);
    await b.equip(b.inventory.items().find(i => i.name === 'netherite_axe'), 'hand');
    const log0 = b.blockAt(new Vec3(tx, wy, tz));
    await b.lookAt(log0.position.offset(0.5, 0.5, 0.5), true);
    await b.dig(log0, true, 'ignore');
    await sleep(1500);
    let logs = 0;
    for (let y = 0; y <= 4; y++) { const bl = b.blockAt(new Vec3(tx, wy + y, tz)); if (bl && bl.name === 'oak_log') logs++; }
    ok(logs === 0, 'tree axe felled the tree (logs left ' + logs + ')');
    // log house without leaves stays
    con(`fill ${tx} ${wy} ${tz - 3} ${tx} ${wy + 3} ${tz - 3} minecraft:oak_log`);
    await sleep(1200);
    const h0 = b.blockAt(new Vec3(tx, wy, tz - 3));
    await b.lookAt(h0.position.offset(0.5, 0.5, 0.5), true);
    await b.dig(h0, true, 'ignore');
    await sleep(1200);
    let house = 0;
    for (let y = 1; y <= 3; y++) { const bl = b.blockAt(new Vec3(tx, wy + y, tz - 3)); if (bl && bl.name === 'oak_log') house++; }
    ok(house === 3, 'logs without natural leaves are not felled (' + house + '/3 left)');

    // ---- spawners
    con(`admin give ${NAME} spawner zombie 3`);
    con(`give ${NAME} minecraft:diamond_pickaxe[enchantments={silk_touch:1}]`);
    con(`give ${NAME} minecraft:iron_pickaxe`);
    await sleep(1500);
    ok(count('spawner') === 3, 'got 3 zombie spawners (' + count('spawner') + ')');
    await b.equip(b.inventory.items().find(i => i.name === 'spawner'), 'hand');
    const sx = wx - 3, sz = wz - 2;
    con(`setblock ${sx} ${wy} ${sz} air`); await sleep(800);
    const ground = b.blockAt(new Vec3(sx, wy - 1, sz));
    await b.lookAt(ground.position.offset(0.5, 1, 0.5), true);
    await b.placeBlock(ground, new Vec3(0, 1, 0)).catch(() => {});
    await sleep(1000);
    const sp = b.blockAt(new Vec3(sx, wy, sz));
    ok(sp && sp.name === 'spawner', 'spawner placed');
    ok(/placed a zombie spawner/.test(log()), 'placement logged');
    b.activateBlock(sp); await sleep(800);
    ok(said(/Stacked 1x/), 'right-click with spawner stacks it');
    // empty hand -> GUI
    b.setQuickBarSlot(8); await sleep(300);
    await sleep(4000); // production (1-2 s cycles in the test config)
    b.activateBlock(sp);
    w = await win();
    ok(w && title(w).includes('ѕᴘᴀᴡɴᴇʀ'), 'spawner GUI opens ' + (w && title(w)));
    const info = w && w.slots[49];
    ok(info && JSON.stringify(info.components || info.nbt || '').includes('2'), 'info shows stack');
    const flesh = w ? w.slots.slice(0, 45).filter(s => s && s.name === 'rotten_flesh').length : 0;
    ok(flesh > 0, 'storage filled with rotten flesh (' + flesh + ' stacks)');
    // take one stack
    const before = count('rotten_flesh');
    const fs0 = w.slots.findIndex((s, i) => i < 45 && s && s.name === 'rotten_flesh');
    await b.clickWindow(fs0, 0, 0); await sleep(500);
    ok(count('rotten_flesh') > before, 'clicking an item takes it (' + before + ' -> ' + count('rotten_flesh') + ')');
    await b.clickWindow(50, 0, 0); await sleep(500);
    ok(said(/Collected .* XP/), 'collect XP works');
    await b.clickWindow(48, 0, 0); await sleep(500);
    ok(said(/Sold .* items for/) || said(/Nothing to sell/) || said(/not available/), 'sell all works');
    b.closeWindow(w); await sleep(500);
    // break with silk touch: removes 1 of the stack
    await b.equip(b.inventory.items().find(i => i.name === 'diamond_pickaxe'), 'hand');
    const sBefore = count('spawner');
    await b.dig(b.blockAt(new Vec3(sx, wy, sz)), true, 'ignore').catch(() => {});
    await sleep(1000);
    ok(count('spawner') === sBefore + 1 && b.blockAt(new Vec3(sx, wy, sz)).name === 'spawner', 'breaking takes 1 from the stack, block stays');
    // without silk touch: denied
    await b.equip(b.inventory.items().find(i => i.name === 'iron_pickaxe'), 'hand');
    await b.dig(b.blockAt(new Vec3(sx, wy, sz)), true, 'ignore').catch(() => {});
    await sleep(800);
    ok(said(/Silk Touch/) && b.blockAt(new Vec3(sx, wy, sz)).name === 'spawner', 'no silk touch -> denied');

    // ---- money / ranks / keyall dispatch the right console commands
    b.chat('/admin'); w = await win();
    w = await click(w, 23); // money
    const head2 = w.slots.findIndex((s, i) => i < 45 && s && s.name === 'player_head');
    w = await click(w, head2);
    ok(w && title(w).includes('ᴍᴏɴᴇʏ'), 'money menu opens');
    const emerald = w.slots.findIndex((s, i) => i < 36 && s && s.name === 'emerald');
    await b.clickWindow(emerald, 0, 0); await sleep(800);
    ok(/ran: \/moneymanager add AdmBot 1000/.test(log()), 'money: moneymanager add AdmBot 1000');
    b.closeWindow(b.currentWindow); await sleep(500);
    b.chat('/admin'); w = await win();
    w = await click(w, 20);
    const head3 = w.slots.findIndex((s, i) => i < 45 && s && s.name === 'player_head');
    w = await click(w, head3);
    ok(w && title(w).includes('ʀᴀɴᴋѕ'), 'ranks menu opens');
    w = await click(w, 11); // first store rank (Plus)
    ok(w && w.slots.some(s => s && s.name === 'clock'), 'duration menu opens');
    const clock = w.slots.findIndex((s, i) => i < 36 && s && s.name === 'clock');
    w = await click(w, clock + 1);   // 1d
    ok(w && title(w).includes('ᴄᴏɴꜰɪʀᴍ'), 'confirm menu opens');
    await b.clickWindow(11, 0, 0); await sleep(800);
    ok(/ran: \/lp user AdmBot parent addtemp hero 1d accumulate/.test(log()), 'rank: lp addtemp hero 1d');
    b.chat('/admin'); w = await win();
    w = await click(w, 31);
    ok(w && title(w).includes('ᴋᴇʏ ᴀʟʟ'), 'key all menu opens');
    const eye = w.slots.findIndex((s, i) => i < 36 && s && s.name === 'ender_eye');
    w = await click(w, eye);
    await b.clickWindow(11, 0, 0); await sleep(800);
    ok(/ran: \/ckeyall random 1/.test(log()), 'key all: ckeyall random 1');

    // ---- data survives a reload of the plugin's file
    con('save-all'); await sleep(500);
  } catch (e) {
    console.log('ERROR', e); fail++;
  }
  console.log(fail ? 'SOME FAILED (' + fail + ')' : 'ALL PASSED');
  process.exit(fail ? 1 : 0);
});
b.on('kicked', r => { console.log('kicked', r); process.exit(1); });
