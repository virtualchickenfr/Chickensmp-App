# ChickenSMP – project context for Claude Code

The owner writes in German and wants short German answers. **Everything in game is English only** (no German texts in plugins/configs). Do the work yourself; only stop for decisions that can't be undone.

## What this repo is
One repo for everything around the Minecraft server **ChickenSMP**:
- **App + website** (Expo / React Native, one codebase): `App.js`, `src/`, `assets/`, `app.json`, `index.js`
- **Backend** (Vercel serverless): `backend/api/*.js`, `backend/vercel.json`
- **Server plugin ChickenSMPApp** (app login, stats, daily rewards): `plugin/`
- **Server plugin ChickenSMPAuth** (login for cracked players only): `auth-plugin/`
- **Auto-announcements** run through **EconomyCore reminders** (`plugins/EconomyCore/economy/announcement/reminder.yml` + `config.yml` stagger 240 s). Our version lives in `server-config/EconomyCore/reminder.yml` – edit there and copy to the server. The separate plugin `announce-plugin/` (ChickenSMPAnnounce) exists but is **disabled** on the server (`ChickenSMPAnnounce.jar.disabled`) to avoid double messages.
- EconomyCore menus `/discord`, `/store`, `/media`, `/rules` (`plugins/EconomyCore/economy/menus/*.yml`) were edited on the server with our links and the Media requirements from the website. EconomyCore loads **every `.yml`** in that folder as a menu – backups must end in `.bak`, never `copy.yml`.
- **Build scripts**: `scripts/` (`build-web.sh`, `ota-manifest.js`)

## Server facts
- Paper 1.21.11, Java 21. Join address `chickensmp.net` (SRV → `mc.chickensmp.net:25577`), Bedrock port 25573.
- Installed: Geyser-Spigot + Floodgate, ViaVersion/ViaBackwards, PacketEvents 2.14.0, LuckPerms, Vault, PlaceholderAPI, Maintenance, TAB and many ChickenSMP* plugins.
- Panel: launch.enderhost.co, server id `8e834fdc` (Pterodactyl). Upload jars by fetching them from raw.githubusercontent.com (branch `plugin-dist`) and POSTing to the panel's signed upload URL (`files/pull` fails with DaemonConnectionException).
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
| `discord-status.yml` | Every 10 min: Discord member/online counts → `status` branch. **Currently disabled** (invite expired, see below). |

Results go to branches because artifacts can't be downloaded from the cloud container.

## App / website
- Website `store.chickensmp.net` = the web build of the app. Vercel serves it via `backend/api/web.js`, which proxies the `web` branch. On screens ≥ 900 px wide there is a separate desktop layout (top nav, multi-column pages, footer); `desk` flag in the app context.
- Style: glassmorphism / liquid, dark, blue gradient (#3b7bff → #7cc4ff), Poppins. Default language English, German available.
- OTA updates: expo-updates against `https://store.chickensmp.net/api/ota` (runtimeVersion "2"). A new APK is only needed for native changes.
- Android home-screen widget: react-native-android-widget (`src/widget/`).
- Server status: `https://api.mcsrvstat.us/3/mc.chickensmp.net:25577`.

## Backend (Vercel)
- Team `team_5pxkgffktT4kPsTuSYBb8Q98`, project `chickensmp` (`prj_hIc9rrMHAdtnMky7N1hGqvWw9Qps`), domain store.chickensmp.net. Deployed with the Vercel MCP `create_deployment` (files inline or by sha).
- Blob store (public) via env `BLOB_READ_WRITE_TOKEN`; env `MC_SERVER_IPS="208.115.203.109,208.115.203.106"` (plugin allowlist).
- `api/app.js`: login, me, logout, online, discord, daily, tickets, ticket, leaderboard. `api/mc.js`: plugin-only endpoints (IP allowlist + TOFU secret).
- Daily rewards: 7-day streak, Europe/Berlin day boundary, delivered in game with `keymanager add {player} {key} {amount}`.

## ChickenSMPAuth (auth-plugin/)
- Premium Java accounts: verified with Mojang at packet level (PacketEvents), keep their Mojang UUID via `AsyncPlayerPreLoginEvent#setPlayerProfile`. Bedrock (Floodgate / Geyser local connection): no login. Cracked: `/register` + `/login`, frozen until logged in. Cracked clients using a premium name are refused.
- Passive while `online-mode=true`.
- CI tests pass (cracked flow + "Notch" refused). **Not yet tested: a real premium account getting through.**
- **Not installed yet.** Plan: (1) upload jar (passive), (2) with nobody online set `online-mode=false` and `enforce-secure-profile=false`, restart, owner joins with his real account + one Bedrock test. Rollback: `online-mode=true`.

## Open items
1. **Discord invite `AfTVp4f88` has expired** – all "Join Discord" buttons are dead. Need a new never-expiring invite code from the owner, then replace it in `src/content.js`, `backend/api/app.js`, `.github/workflows/discord-status.yml` in the `discord` reminder of `server-config/EconomyCore/reminder.yml` (copy to the server) and in `menus/discord.yml` on the server, redeploy backend, re-enable `discord-status.yml`.
2. Ticket webhook: owner must set Vercel env `DISCORD_TICKET_WEBHOOK` himself (never enter webhook URLs/tokens yourself), then redeploy backend and send a test ticket.
3. Daily reward delivery in game not yet tested with a real player.
4. Server is in maintenance mode (owner decides when to turn it off).

## Rules
- Never enter passwords, tokens, API keys or webhook URLs; the owner does that.
- No permanent deletions. Commit messages end with the Co-Authored-By line.
