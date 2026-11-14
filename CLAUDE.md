# ChickenSMP – project context for Claude Code

The owner writes in German and wants short German answers. **Everything in game is English only** (no German texts in plugins/configs). Do the work yourself; only stop for decisions that can't be undone.

## What this repo is
One repo for everything around the Minecraft server **ChickenSMP**:
- **App + website** (Expo / React Native, one codebase): `App.js`, `src/`, `assets/`, `app.json`, `index.js`
- **Backend** (Vercel serverless): `backend/api/*.js`, `backend/vercel.json`
- **Server plugin ChickenSMPApp** (app login, stats, daily rewards): `plugin/`
- **Server plugin ChickenSMPAC-plus** (`acplus-plugin/`, workflow `acplus-plugin.yml` with mineflayer bot tests → `plugin-dist`): (1) `/sus` tracker for anticheat flags (**GrimAC** `ac.grim.grimac.api.events.FlagEvent` Bukkit event via reflection, Vulcan API also supported; check categories combat/movement/player; sus score never expires (sus-score 40, watch 15); entry auto-deleted when the player gets banned (ban commands / ban kick); `/sus <player>|tp|watch|clear|reload|test`, tp/watch turn on staff mode first; clear = any staff); (2) **Banhammer** `/banhammer` (alias /bh): mace, right-click cycles modes from config (ban perm / 30d / 7d / cage only / kick, commands run as the staff member through ChickenSMPAC), hit twice to confirm → CAUGHT! title, lightning, cage (restored after), frozen `cage-seconds` (60), then the command; `/banhammer release <p>|list`; (3) **staff mode** `/staff` (spectator + hidden from players without `chickensmp.acplus.see`, fake leave/join, back to start position on exit). (4) **PvP area**: world `Spawn1` outside WorldGuard region `spawn` = PvP; players may build, placed blocks revert after 20 min (also on shutdown), map blocks/explosions/fire protected (`chickensmp.acplus.arena.bypass`), PVP AREA / SAFE ZONE titles. Uploaded 2026-11-13; old `ChickenSMPSus.jar` renamed `.disabled`.
- **Anticheat = GrimAC 2.3.74** (free, GPLv3, unmodified copy on `plugin-dist` from Modrinth). **The owner's Vulcan-2.9.7.26.jar was a cracked copy with a backdoor** (`ac/vulcan/anticheat/bstatshash.class`, "[Axiom] Stager" loading code from bstats.qzz.io) – renamed `Vulcan-2.9.7.26.jar.MALWARE-disabled` on 2026-11-13; all other 53 plugin jars scanned clean. Never install, patch or 'clean' cracked plugins.
- **Server plugin ChickenSMPAuth** (login for cracked players only): `auth-plugin/`
- **Auto-announcements** run through **EconomyCore reminders** (`plugins/EconomyCore/economy/announcement/reminder.yml` + `config.yml` stagger 240 s). Our version lives in `server-config/EconomyCore/reminder.yml` – edit there and copy to the server. The separate plugin `announce-plugin/` (ChickenSMPAnnounce) exists but is **disabled** on the server (`ChickenSMPAnnounce.jar.disabled`) to avoid double messages.
- EconomyCore menus `/discord`, `/store`, `/media`, `/rules` (`plugins/EconomyCore/economy/menus/*.yml`) were edited on the server with our links and the Media requirements from the website. EconomyCore loads **every `.yml`** in that folder as a menu – backups must end in `.bak`, never `copy.yml`.
- **Videos** (explainroo, TikTok/Shorts 1080×1920): `videos/<name>/` (`video.json`, `script.md`, `scenes.js`, `format.js`). Rendered by `.github/workflows/explainroo.yml` (dispatch with input `project`), output pushed to branch `video-out`. Public copies for posting live on branch `media` (`videos/*.mp4`, served via raw.githubusercontent.com).
- **Server config copies**: `server-config/` (e.g. EconomyCore reminders).
- **Build scripts**: `scripts/` (`build-web.sh`, `ota-manifest.js`)

## Server facts
- Paper 1.21.11, Java 21. Join address `chickensmp.net` (SRV → `mc.chickensmp.net:25018`), Bedrock port 25018 (needs a 2nd port, see below).
- **Moved to MC-HOST24 on 2026-11-13**: panel mc-panel.mc-host24.de, server id `ff44cd6b`, IP `45.93.248.37`, only one port `25018` (allocation limit 0). Old EnderHost server (launch.enderhost.co, `8e834fdc`, 208.115.203.109) is stopped and kept as backup – don't delete it.
- DNS for chickensmp.net is at **lima-city** (usercp/domains/59856/records): A `chickensmp.net` + A `mc` → 45.93.248.37, SRV `_minecraft._tcp` → `0 25018 mc.chickensmp.net`. Owner logs in himself.
- **Bedrock/voice chat open issue:** Geyser (nethernet) needs its own TCP+UDP port for built-in signaling; external signaling (warden.cloud) rejects us (404). Voice chat was disabled (`voicechat-bukkit-2.6.18.jar.disabled`) because it blocked UDP 25018. Fix: owner gets a 2nd port from MC-HOST24 → set Geyser `bedrock.port` to it, `advertise-addresses: ['45.93.248.37']` (done), re-enable voicechat (port=-1), update BEDROCK_PORT in `src/content.js`, the bedrock reminder and the Bedrock join texts.
- Backend allowlist `MC_SERVER_IPS` includes 45.93.248.37 (redeployed 2026-11-13). App/website status uses `chickensmp.net` (SRV), no fixed port.
- Installed: Geyser-Spigot + Floodgate, ViaVersion/ViaBackwards, PacketEvents 2.14.0, LuckPerms, Vault, PlaceholderAPI, Maintenance, TAB and many ChickenSMP* plugins.
- Panel (old): launch.enderhost.co, server id `8e834fdc` (Pterodactyl). Panel (new): mc-panel.mc-host24.de `ff44cd6b` – the client API works from the page with the XSRF-TOKEN cookie. Upload jars by fetching them from raw.githubusercontent.com (branch `plugin-dist`) and POSTing to the panel's signed upload URL (MC-HOST24 rejects uploads > ~10 MB with 400 – there `files/pull` with the raw GitHub URL works; on EnderHost `files/pull` failed).
- **LuckPerms** (h2 storage, manage via console `lp ...`; read with `lp export <name> --without-users` → `plugins/LuckPerms/<name>.json.gz`). Set up 2026-11-14: staff chain jr.helper → helper → mod → sr.mod → admin → manager; dev + owner = manager + `*`. jr.helper: warn/mute/kick/history, alerts; helper: +tempban, freeze, vanish, `/sus`, `/staff` (ban limit 1d); mod: +ban/unban, invsee, Banhammer (30d); sr.mod: +ipban, unpunish, IPs (perm); admin: `chickensmpac.*`, `grim.*`, WorldEdit/WorldGuard bypass; manager: all plugin `*.admin` nodes. Player ranks follow the store page (`RANKS` in `src/content.js`): + = 5 homes, no RTP cooldown, shards; core = 7 homes, `economy.util.*`, tags gold/fire/ocean; supreme = 10 homes, glow, `economy.util.enderchest`, tags king/legend/dragon/galaxy; default got `chickensmpac.report` + `economycore.kit.starter`. Not possible yet: Supreme kit (only kits basic/starter exist), priority queue, *random* tags; booster inherits core although the site says Plus perks. Tracks `staff` and `ranks` (default, +, core, supreme). Backups: `perms-2026-11-14.json.gz` (before), `perms-after-2026-11-14.json.gz`.
- Owner: **Fuzed_d**. Team: Carbon2834 (Developer), ItzPinguTime (Admin). Discord guild `1554105933213335552`.
- Before any restart: check that no players are online. Never broadcast with `say`.

## Builds (npm/maven are not run locally – everything runs in GitHub Actions)
| Workflow | What it does |
|---|---|
| `android.yml` | Fresh `create-expo-app` template + our files → `expo prebuild` → Gradle → release `v1.0.N` (body contains `runtime: N`), signed with `keystore/debug.keystore`. Also exports the OTA update to the `ota` branch. |
| `web.yml` | `scripts/build-web.sh` → pushes `dist` to the `web` branch |
| `preview.yml` | Screenshots (incl. mocked logged-in views) → `preview-shots` branch |
| `live-check.yml` | Screenshots of store.chickensmp.net (mobile + desktop tabs) → `live-check` branch |
| `plugin.yml` | Builds ChickenSMPApp → release `plugin-N` |
| `auth-plugin.yml` | Builds ChickenSMPAuth, runs a real Paper 1.21.11 test server with mineflayer bots, pushes the jar to `plugin-dist` |
| `announce-plugin.yml` | Builds ChickenSMPAnnounce, loads it on a Paper test server, pushes the jar to `plugin-dist` |
| `explainroo.yml` | Renders a video project from `videos/<project>` with voice, music, captions → branch `video-out` |
| `discord-status.yml` | Every 10 min: Discord member/online counts → `status` branch. **Currently disabled** (invite expired, see below). |

Results go to branches because artifacts can't be downloaded from the cloud container.

## App / website
- Website `store.chickensmp.net` = the web build of the app. Vercel serves it via `backend/api/web.js`, which proxies the `web` branch. On screens ≥ 900 px wide there is a separate desktop layout (top nav, multi-column pages, footer); `desk` flag in the app context.
- Style: glassmorphism / liquid, dark, blue gradient (#3b7bff → #7cc4ff), Poppins. Default language English, German available.
- OTA updates: expo-updates against `https://store.chickensmp.net/api/ota` (runtimeVersion "2"). A new APK is only needed for native changes.
- Android home-screen widget: react-native-android-widget (`src/widget/`).
- Server status: `https://api.mcsrvstat.us/3/chickensmp.net`, backup `https://api.mcstatus.io/v2/status/java/chickensmp.net` (SRV). A MOTD/version containing "maintenance" shows a yellow **Maintenance – back soon** state (app, website, widget).
- Tickets screen (`src/Tickets.js`): ticket list → tap opens `TicketChat` (staff messages, reply box, close). Falls back to a note when the chat backend isn't available.

## Backend (Vercel)
- Team `team_5pxkgffktT4kPsTuSYBb8Q98`, project `chickensmp` (`prj_hIc9rrMHAdtnMky7N1hGqvWw9Qps`), domain store.chickensmp.net. Deployed with the Vercel MCP `create_deployment` (files inline or by sha).
- Storage: **Upstash Redis** (env `KV_REST_API_URL` / `KV_REST_API_TOKEN`), players in one hash `players` (field = uuid). The old Vercel Blob store was suspended on 2026-11-10 (free quota exceeded) and its data is lost; env `MC_SERVER_IPS="208.115.203.109,208.115.203.106"` (plugin allowlist).
- `api/app.js`: login, me, logout, online, discord, daily, tickets, ticket (POST create / GET one with messages), reply, close, leaderboard. Tickets that couldn't be delivered to Discord don't count towards the 3-per-30-min limit. `api/mc.js`: plugin-only endpoints (IP allowlist + TOFU secret).
- Daily rewards: 7-day streak, Europe/Berlin day boundary, delivered in game with `keymanager add {player} {key} {amount}`.

## Ticket chat (website/app ↔ Discord) – code done, NOT live yet
- Design: one **Discord forum post per ticket**, read/written by a bot via REST (no gateway). Staff answer in the post; player sees it in the app/website and replies there (bot posts an embed with footer "Reply from the website / app"). Closing the post in Discord (archive/lock) = ticket closed; player can also close.
- Forum channel created: **`🎫・ᴛɪᴄᴋᴇᴛ-ᴄʜᴀᴛ`** (id `1570532188393312366`), category SUPPORT, private (SR Moderator, Moderator, Helper, JR. Helper), tags: Support, Bug, Player report, Purchase / rank, Ban appeal, Other (names match `CATS` labels).
- Backend code is committed in `backend/api/app.js` but the **live deployment is older** (it has the webhook fallback, logging and the rate-limit fix, but not the chat endpoints). Deploy all backend files after setup.
- Still to do: (1) owner says "ja" → create Discord application "ChickenSMP Tickets" + bot in the Developer Portal (accepting developer terms needs his OK); (2) **owner** resets/copies the bot token into Vercel env `DISCORD_BOT_TOKEN` (never handle it yourself); (3) owner authorizes the bot invite (scope `bot`, perms: View Channels, Send Messages, Send Messages in Threads, Create Posts, Manage Threads, Read Message History, Embed Links, Attach Files); (4) give the bot access to the private forum; (5) set Vercel env `DISCORD_TICKET_FORUM=1570532188393312366` (plain value, fine to set); (6) redeploy backend; (7) end-to-end test.
- Without bot env vars the backend keeps using the webhook (`DISCORD_TICKET_WEBHOOK`, channel `#📱・ᴀᴘᴘ-ᴛɪᴄᴋᴇᴛꜱ`, id 1568823436720082944) – that path is **live and tested** (2026-11-12).

## Social media (Metricool)
- Metricool connector, brand id `8065935` (timezone Europe/Berlin). Connected: **TikTok @chickensmpnet**, **YouTube** channel `UCK05N8LXaqojGe14Kd0dBmw`. Instagram not connected yet (needs a Business/Creator account, added in Metricool).
- Posting: video must be a public URL → push to branch `media`, then `createScheduledPost` a few minutes in the future (one post per network so texts can differ). Always show the owner the texts first and post only after his "ok"/"upload". TikTok posts with the AI voice are marked `isAigc: true`.
- Posted: Social Media Manager hiring video (2026-11-12 20:28, YouTube `zmh4aHWxQvI`), Season 2 promo `videos/promo-s2` (2026-11-12 21:55, TikTok 7707391601856875798, YouTube `PQvssH2yPuI`), owner's "Stop scrolling" video (22:06, TikTok 7707394437244030230, YouTube `auQVJn-qxq0`). Scheduled: owner's "Welcome to ChickenSMP" video 2026-11-13 17:30 on both. Owner's trend edit `media/videos/chickensmp-trend-1113.mp4` scheduled 2026-11-14 12:40 on TikTok (Discord: "link in bio") + YouTube Short (discord.gg/yVQ9nsJUK). Promo `videos/promo-this-is` (explainroo, "This is the ChickenSMP…") – owner added a hook clip in front: `media/videos/chickensmp-promo-1114.mp4`, scheduled 2026-11-14 12:56 TikTok (isAigc) + YouTube Short. Owner-voice promo `videos/promo-voice` (recording provider, clips cut from his menu videos). Story video + promo improved (IP banner, white flash + whoosh, loudness) → `media/videos/chickensmp-story-promo.mp4`, scheduled 2026-11-14 13:38 TikTok + YouTube Short. Current Discord invite **yVQ9nsJUK** works but expires 2026-12-12 – owner should make a never-expiring one. Logo files: `assets/logo.png` (transparent), `assets/icon.png`.
- Applications for the Social Media Manager job arrive as tickets on the website.

## ChickenSMPAuth (auth-plugin/)
- Premium Java accounts: verified with Mojang at packet level (PacketEvents), keep their Mojang UUID via `AsyncPlayerPreLoginEvent#setPlayerProfile`. Bedrock (Floodgate / Geyser local connection): no login. Cracked: `/register` + `/login`, frozen until logged in. Cracked clients using a premium name are refused.
- Passive while `online-mode=true`.
- CI tests pass (cracked flow + "Notch" refused). **Not yet tested: a real premium account getting through.**
- **Not installed yet.** Plan: (1) upload jar (passive), (2) with nobody online set `online-mode=false` and `enforce-secure-profile=false`, restart, owner joins with his real account + one Bedrock test. Rollback: `online-mode=true`.

## Open items
1. **Discord invite `AfTVp4f88` has expired** – all "Join Discord" buttons are dead. Need a new never-expiring invite code from the owner, then replace it in `src/content.js`, `backend/api/app.js`, `.github/workflows/discord-status.yml` in the `discord` reminder of `server-config/EconomyCore/reminder.yml` (copy to the server) and in `menus/discord.yml` on the server, redeploy backend, re-enable `discord-status.yml`.
2. Ticket webhook: env `DISCORD_TICKET_WEBHOOK` is set (production), channel `#📱・ᴀᴘᴘ-ᴛɪᴄᴋᴇᴛꜱ` (id 1568823436720082944), webhook "ChickenSMP Tickets". A real test ticket from a logged-in player is still pending.
3. **Ticket chat** – see the section above (needs owner: "ja" for the Discord app, bot token into Vercel, authorize invite).
4. ~~Ban/punishment logs~~ **done 2026-11-13**: ChickenSMPAC `discord.enabled: true`, webhook "ChickenSMP Logs" in `#💻・ɪɴɢᴀᴍᴇ-ʟᴏɢs` (id 1554239408968830997), URL pasted by the owner; tested with a warn (removed with `/unwarn <ID>`).
5. Daily reward delivery in game not yet tested with a real player.
6. Server is still in **maintenance mode** (MOTD "⚠ Maintenance") – owner decides when to turn it off (`/maintenance off`). Promo videos are already live.
7. ChickenSMPApp 1.2.2 (English-only `/app`) sits in `plugins/update/` and is applied on the next restart.

## Connection notes
- Server panel and Discord are reached through the **built-in browser of the Claude desktop app** (or Claude in Chrome). If those tools are missing, the owner's PC isn't linked to the session – ask him to start a new task in the desktop app with his PC selected.
- Discord in the built-in browser may be signed out; the owner signs in himself.

## Rules
- Never enter passwords, tokens, API keys or webhook URLs; the owner does that.
- No permanent deletions. Commit messages end with the Co-Authored-By line.
