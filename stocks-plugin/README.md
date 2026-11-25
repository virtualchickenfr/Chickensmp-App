# ChickenSMPStocks 3 – Ore Market

`/stocks` (aliases `stock`, `market`, `aktien`, `ores`) opens a chest menu with 5 ores:
coal, iron, gold, diamond, netherite. Prices move every minute on their own and drift back
to their normal price; random ORE NEWS gives jumps of 8–20 %. Players buy 1/10/64 and sell
1/10/all; fee 1.5 % on each trade; max per ore in `config.yml`. No real items are given.

Version 2 (made-up companies, dialog screens) was decompiled from the server jar (no source)
and replaced. On first start, 3.0 pays out every old company share at its last price (no fee),
logs it and tells the player on join.

Admin (`stocks.admin`): `/stocks reload`, `/stocks set <ore> <price>`, `/stocks news <ore> up|down`.
Test: `test/test.js` (mineflayer) with `test/fakeeco` as Vault economy, run by `stocks-plugin.yml`.
