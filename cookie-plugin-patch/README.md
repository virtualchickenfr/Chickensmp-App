# CookieClicker 1.2.1-chickensmp (patch)

No source for CookieClicker 1.2.0-chickensmp existed. These two classes were decompiled with CFR
from the server jar, changed and recompiled against the original jar (the rest of the decompiled
code does not compile, so only these classes are replaced in the jar):

- `Model.java`: `Profile.upgrade(id)` is capped to `upgrades.<id>.max-level` from the live config
  (lowering a max level also limits levels bought before; max-level 0 = upgrade off).
- `MainMenu.java`: the Buildings button is hidden when there are no buildings.
- The jar's embedded default `config.yml` has `buildings: {}` (the plugin merges default keys, so
  buildings could not be removed from the live config before).

Jar: branch `plugin-dist` → `CookieClicker-1.2.1-chickensmp.jar`. Tested locally with a fake Vault
economy: 0 buildings loaded, 10 clicks = 500 cookies with cookies-per-click 50.
