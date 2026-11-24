# Tidal 1.0.1-chickensmp (patch)

No source existed. `ShopMenu.java` was decompiled with CFR from Tidal-1.0.0-chickensmp.jar, changed and
recompiled against the original jar (only this class is replaced). Free rods (price 0 in rods.yml, the
Driftwood Rod) can only be taken from `/fish shop` once per `free-rod-cooldown-minutes` (default 60,
0 = off); timestamps in `plugins/Tidal/free-rods.yml`, bypass permission `tidal.freerod.bypass`,
message `messages.free-rod-cooldown` ({prefix}, {minutes}). Tested locally with a mineflayer bot.
