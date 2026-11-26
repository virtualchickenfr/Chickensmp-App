# ChickenSMPAC 2.1.1 (patch)

No source exists. ChatModule.java decompiled with CFR from ChickenSMPAC-2.1.0.jar, fixed and recompiled against the original jar (only ChatModule.class replaced; two CFR switch artefacts fixed by hand).
`/chat clear` now sends `chat.clear-lines` single blank messages to every player (the old version sent one message with 150 newlines and skipped everyone with chickensmpac.chat.bypass, i.e. all ops/staff with `*`).

# ChickenSMPAC 2.1.2 (patch, 2026-11-26)

`AnnounceModule.java` rewritten (decompiled original + changes), compiled against ChickenSMPAC-2.1.1.jar and the Paper 1.21.11 libraries, only `AnnounceModule.class` and the plugin.yml version replaced.
`/announce` messages support `{head}` (the sender's skin head as a chat object component, textures from the player's profile, 1.21.9+ clients) and `{rank}` (`%luckperms_prefix%` via PlaceholderAPI). Title, subtitle and chat lines are sent as components.
Live `messages.yml`: `announce.title: "{head} {rank}&f{player}"`, chat `""`, `"  {head} {rank}&f{player}"`, `"  &f{message}"`, `""`.
