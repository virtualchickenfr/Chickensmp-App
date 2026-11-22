# ChickenSMPKillEffects 2.x

Block kill effects: little animated models built from block displays (helicopter, balloon, piano, UFO ...),
24 effects in 6 rarities, crates, shards. Version 1.x (particle/sprite effects) was decompiled from the
installed jar and rebuilt; `Effects.MIGRATE` moves owned 1.x effects to the new effect of the same rarity.

- `Model.java` – model engine (boxes, children, interpolated move/rotate/scale, shatter)
- `Effects.java` – the 24 effects
- Build: `mvn -q -B package` → `target/ChickenSMPKillEffects.jar`
- Test: Paper 1.21.11 + Vault + this jar, then `node test/test.js <ids...>` (mineflayer bots: plays every effect, a preview and a real kill)
