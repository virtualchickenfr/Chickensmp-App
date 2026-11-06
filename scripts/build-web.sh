#!/usr/bin/env bash
# Builds the installable web app (iPhone / any browser) into build/dist
set -euo pipefail
BASE="${BASE:-}"   # e.g. /Chickensmp-App for GitHub Pages
rm -rf build
npx --yes create-expo-app@latest build --template blank --yes
cp -r App.js app.json src assets build/
cd build
if [ -n "$BASE" ]; then BASE="$BASE" node -e 'const f="app.json",j=require("./"+f);j.expo.experiments={baseUrl:process.env.BASE};require("fs").writeFileSync(f,JSON.stringify(j,null,2))'; fi
npx expo install expo-clipboard expo-status-bar expo-font @expo/vector-icons @expo-google-fonts/poppins @react-native-async-storage/async-storage react-native-safe-area-context react-dom react-native-web @expo/metro-runtime
npx expo export --platform web --output-dir dist
cd dist
cp ../assets/apple-touch-icon.png ../assets/icon-192.png ../assets/icon-512.png .
cat > manifest.json <<JSON
{"name":"ChickenSMP","short_name":"ChickenSMP","start_url":"$BASE/","scope":"$BASE/","display":"standalone","orientation":"portrait","background_color":"#141414","theme_color":"#141414",
 "icons":[{"src":"$BASE/icon-192.png","sizes":"192x192","type":"image/png"},{"src":"$BASE/icon-512.png","sizes":"512x512","type":"image/png","purpose":"any maskable"}]}
JSON
BASE="$BASE" node -e '
const fs=require("fs"),B=process.env.BASE||"";
let h=fs.readFileSync("index.html","utf8");
h=h.replace(/<meta name="viewport"[^>]*>/, `<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, viewport-fit=cover">`);
const head=`<link rel="manifest" href="${B}/manifest.json"><link rel="apple-touch-icon" href="${B}/apple-touch-icon.png"><meta name="apple-mobile-web-app-capable" content="yes"><meta name="mobile-web-app-capable" content="yes"><meta name="apple-mobile-web-app-status-bar-style" content="black-translucent"><meta name="apple-mobile-web-app-title" content="ChickenSMP"><meta name="theme-color" content="#141414"><title>ChickenSMP</title><style>html,body{background:#141414}</style>`;
h=h.replace("</head>", head+"</head>");
fs.writeFileSync("index.html",h);
'
touch .nojekyll
cp index.html 404.html
