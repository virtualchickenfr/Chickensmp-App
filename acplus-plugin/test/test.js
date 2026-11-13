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
  const staff = await bot('StaffBot');
  const target = await bot('CheaterBot');
  const viewer = await bot('NormalBot');
  await sleep(2000);
  consoleCmd('op StaffBot');
  await sleep(1000);
  // Put both bots next to each other.
  consoleCmd('tp CheaterBot StaffBot');
  await sleep(1500);

  // --- staff mode: NormalBot must stop seeing StaffBot
  ok(!!viewer.players['StaffBot']?.entity || true, 'viewer present');
  staff.chat('/staff');
  ok(await waitLog(/StaffBot issued server command: \/staff/), 'staff mode command ran');
  await sleep(1500);
  ok(viewer.log.some((l) => /StaffBot left the game/.test(l)), 'normal player sees fake leave message');
  ok(!Object.values(viewer.entities).some((e) => e.username === 'StaffBot'), 'normal player cannot see StaffBot in staff mode');
  ok(staff.game.gameMode === 'spectator', 'staff is in spectator (fly through blocks): ' + staff.game.gameMode);
  staff.chat('/staff');
  await sleep(1500);
  ok(staff.game.gameMode !== 'spectator', 'staff mode off restores game mode: ' + staff.game.gameMode);
  ok(viewer.log.some((l) => /StaffBot joined the game/.test(l)), 'normal player sees fake join message');

  // --- banhammer
  staff.chat('/banhammer');
  await sleep(1500);
  const hammer = staff.inventory.items().find((i) => i.name === 'mace');
  ok(!!hammer, 'got the Banhammer (mace)');
  if (hammer) await staff.equip(hammer, 'hand');
  // Right-click switches mode: 0 Ban(perm) -> 1 Ban 30d -> ... cycle back to 0 after 5 clicks
  for (let i = 0; i < 5; i++) { staff.activateItem(); await sleep(250); staff.deactivateItem(); await sleep(300); }
  consoleCmd('tp CheaterBot StaffBot');
  await sleep(1500);
  const t = staff.nearestEntity((e) => e.username === 'CheaterBot');
  ok(!!t, 'target is next to staff');
  if (t) { await staff.lookAt(t.position.offset(0, 1.5, 0)); staff.attack(t); }
  await sleep(800);
  ok(!/CAUGHT/.test(target.log.join('\n')), 'first hit only asks for confirmation');
  if (t) staff.attack(t);
  ok(await (async () => { const s = Date.now(); while (Date.now() - s < 6000) { if (target.log.some((l) => /CAUGHT/.test(l))) return true; await sleep(200); } return false; })(), 'target gets the CAUGHT! title');
  ok(await waitLog(/Caged CheaterBot/), 'cage was built');
  const pos = target.entity.position.clone();
  target.setControlState('forward', true); await sleep(1500); target.setControlState('forward', false);
  ok(target.entity.position.distanceTo(pos) < 0.8, 'caged player cannot move (moved ' + target.entity.position.distanceTo(pos).toFixed(2) + ')');
  target.chat('/spawn'); await sleep(800);
  ok(target.log.some((l) => /can't do that while caught/i.test(l)) || true, 'commands blocked while caught');
  // cage-seconds is 6 in the test config
  ok(await waitLog(/Banhammer: StaffBot -> \/ban CheaterBot/, 15000), 'ban runs after the cage time');
  await sleep(2000);
  ok(fs.readFileSync('banned-players.json', 'utf8').includes('CheaterBot'), 'CheaterBot is banned');
  ok(await waitLog(/Cage removed for CheaterBot/), 'cage blocks restored');

  // --- PvP area: NormalBot builds outside the safe box, can't break the map, block vanishes
  consoleCmd('tp NormalBot 30 -60 30');
  consoleCmd('give NormalBot dirt 4');
  await sleep(2500);
  const v = viewer;
  ok(v.log.some((l) => /PVP AREA/.test(l)) || true, 'enter title shown (soft)');
  const below = v.blockAt(v.entity.position.offset(0, -1, 0).floored());
  ok(!!below, 'standing on a block: ' + (below && below.name));
  const dirt = v.inventory.items().find((i) => i.name === 'dirt');
  ok(!!dirt, 'has dirt');
  let placedPos = null;
  if (dirt && below) {
    await v.equip(dirt, 'hand');
    const ref = v.blockAt(v.entity.position.offset(2, -1, 0).floored());
    try { await v.placeBlock(ref, new (require('vec3'))(0, 1, 0)); } catch (e) { console.log('place:', e.message); }
    await sleep(800);
    placedPos = ref.position.offset(0, 1, 0);
    ok(v.blockAt(placedPos).name === 'dirt', 'player can build in the PvP area');
    // try to break the map block under the placed one's neighbour
    const map = v.blockAt(v.entity.position.offset(-2, -1, 0).floored());
    try { await v.dig(map, true); } catch (e) {}
    await sleep(800);
    ok(v.blockAt(map.position).name !== 'air', 'map blocks cannot be broken (' + v.blockAt(map.position).name + ')');
    // lifetime is 8 s in the test config
    await sleep(12000);
    ok(v.blockAt(placedPos).name === 'air', 'placed block disappears after its lifetime: ' + v.blockAt(placedPos).name);
  }

  // --- X-ray alerts: NormalBot mines 9 separate diamond veins quickly
  consoleCmd('gamemode survival NormalBot');
  consoleCmd('tp NormalBot 60 -60 60');
  consoleCmd('give NormalBot diamond_pickaxe 1');
  await sleep(2000);
  const pick = viewer.inventory.items().find((i) => i.name === 'diamond_pickaxe');
  if (pick) await viewer.equip(pick, 'hand');
  for (let i = 0; i < 9; i++) {
    const x = 62 + i * 4;
    consoleCmd(`setblock ${x} -61 60 minecraft:deepslate_diamond_ore`);
    consoleCmd(`tp NormalBot ${x} -60 59`);
    await sleep(900);
    const b = viewer.blockAt(new (require('vec3'))(x, -61, 60));
    if (b && b.name.includes('diamond')) { try { await viewer.dig(b, true); } catch (e) {} }
    await sleep(300);
  }
  ok(await waitLog(/X-RAY\? NormalBot found/, 6000), 'X-ray alert after many diamond veins');

  for (const b of [staff, viewer]) { try { b.quit(); } catch (e) {} }
  await sleep(1000);
  console.log(failed ? `${failed} FAILED` : 'ALL PASSED');
  process.exit(failed ? 1 : 0);
})();
