// usage: node tr.js id1 id2 ...  -> trace/<id>.jsonl
const mineflayer = require('mineflayer'); const fs = require('fs');
const sleep = ms => new Promise(r => setTimeout(r, ms));
const cmd = c => fs.appendFileSync('server.in', c + '\n');
(async () => {
  const a = mineflayer.createBot({ host: '127.0.0.1', port: 25565, username: 'Killer', version: '1.21.11' });
  await new Promise(r => a.once('spawn', r));
  cmd('op Killer'); cmd('difficulty peaceful'); cmd('gamemode spectator Killer'); cmd('tp Killer 0.5 -60 0.5 0 0'); cmd('time set day');
  await sleep(1500);
  for (const id of process.argv.slice(2)) { a.chat('/ke play ' + id); await sleep(6500); }
  a.quit(); process.exit(0);
})();
