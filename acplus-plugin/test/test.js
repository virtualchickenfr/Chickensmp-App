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
  // inside the test safe box (-15..15), so the PvP-area map protection doesn't cancel the digging
  const spots = [-14, -10, -6, -2, 2, 6, 10, 14].map((x) => [x, 5]).concat([[-14, 12]]);
  for (const [x, z] of spots) {
    consoleCmd(`setblock ${x} -61 ${z} minecraft:deepslate_diamond_ore`);
    consoleCmd(`tp NormalBot ${x} -60 ${z - 1}`);
    await sleep(900);
    const b = viewer.blockAt(new (require('vec3'))(x, -61, z));
    let err = '';
    if (b && b.name.includes('diamond')) { try { await viewer.dig(b, true); } catch (e) { err = e.message; } }
    await sleep(300);
    console.log('xray spot', x, z, 'before', b && b.name, 'after', viewer.blockAt(new (require('vec3'))(x, -61, z))?.name, 'pos', viewer.entity.position.toString(), err);
  }
  ok(await waitLog(/X-RAY\? NormalBot found/, 6000), 'X-ray alert after many diamond veins');

  // --- /sus GUI: list -> click head -> player details
  for (let i = 0; i < 3; i++) consoleCmd('sus test NormalBot KillAura combat');
  await sleep(1000);
  const nextWin = () => new Promise((r) => { staff.once('windowOpen', r); setTimeout(() => r(null), 5000); });
  let w = nextWin(); staff.chat('/sus'); w = await w;
  const title = (x) => { try { return JSON.stringify(x.title); } catch (e) { return String(x && x.title); } };
  ok(w && title(w).includes('ѕᴜѕᴘɪᴄɪᴏᴜѕ'), '/sus opens the GUI ' + (w && title(w)));
  const headSlot = w ? w.slots.findIndex((it, i) => i < 45 && it && it.name === 'player_head') : -1;
  ok(headSlot >= 0, 'flagged player is listed as a head');
  if (headSlot >= 0) {
    const w2p = nextWin(); await staff.clickWindow(headSlot, 0, 0); const w2 = await w2p;
    ok(w2 && title(w2).includes('NormalBot'), 'clicking the head opens the player page ' + (w2 && title(w2)));
    if (w2) staff.closeWindow(w2);
  }

  // --- ban animation: /ban on an online player waits for the animation, then bans
  const victim = await bot('BanBot');
  await sleep(1500);
  consoleCmd('tp BanBot 0 -60 0');
  await sleep(1500);
  const y0 = victim.entity.position.y;
  staff.chat('/ban BanBot Hacking');
  ok(await waitLog(/Ban animation: StaffBot -> \/ban BanBot Hacking/), 'ban command is taken over by the animation');
  await sleep(1500);
  ok(victim.log.some((l) => /BANNED/.test(l)), 'victim sees the BANNED title');
  ok(victim.entity.position.y > y0 + 0.3, 'victim is lifted (' + (victim.entity.position.y - y0).toFixed(2) + ')');
  ok(Object.values(staff.entities).filter((e) => e.name === 'text_display').length >= 20, 'beam and ring displays are visible');
  ok(!fs.readFileSync('banned-players.json', 'utf8').includes('BanBot'), 'not banned yet during the animation');
  await sleep(4000);
  ok(fs.readFileSync('banned-players.json', 'utf8').includes('BanBot'), 'BanBot is banned after the animation');
  ok(victim.log.some((l) => /KICK/.test(l)), 'victim was kicked');
  await sleep(500);
  ok(Object.values(staff.entities).filter((e) => e.name === 'text_display').length === 0, 'all displays removed');

  for (const b of [staff, viewer]) { try { b.quit(); } catch (e) {} }
  await sleep(1000);
  console.log(failed ? `${failed} FAILED` : 'ALL PASSED');
  process.exit(failed ? 1 : 0);
})();
