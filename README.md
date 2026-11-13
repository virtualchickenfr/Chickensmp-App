# ChickenSMP App

Offizielle App für den Minecraft-Server **ChickenSMP** (chickensmp.net) – gebaut mit Expo / React Native.

**Download (Android):** https://github.com/virtualchickenfr/chickensmp-app/releases/latest/download/ChickenSMP.apk

## Version 1
- Live-Serverstatus und Spielerzahl, IP mit einem Tipp kopieren
- „So joinst du“ für Java und Bedrock (Port 25018)
- Shop: Ränge, Crate-Keys, Extras – Kauf per Discord-Ticket
- Community: Discord (Mitglieder/online), Regeln, Team
- Deutsch / Englisch, Glass- und Liquid-Design passend zur Website

## Wie gebaut wird
Jeder Push auf `main` startet GitHub Actions (`.github/workflows/android.yml`): neues Expo-Projekt, App-Code rein,
`expo prebuild`, Gradle-Release-Build. Die APK landet als Release.

`keystore/debug.keystore` signiert die APK immer gleich, damit Updates über die alte Version installiert werden können.
Für den Play Store später einen eigenen Upload-Key nutzen.

## Dateien
- `App.js` – die ganze App (Screens, Glass-UI, Liquid-Animationen)
- `src/content.js` – Texte DE/EN, Ränge, Keys, Regeln, Team
- `assets/` – Logo, Icon, Splash, Hintergrund-Blobs
