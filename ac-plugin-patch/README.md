# ChickenSMPAC 2.1.1 (patch)

No source exists. ChatModule.java decompiled with CFR from ChickenSMPAC-2.1.0.jar, fixed and recompiled against the original jar (only ChatModule.class replaced; two CFR switch artefacts fixed by hand).
`/chat clear` now sends `chat.clear-lines` single blank messages to every player (the old version sent one message with 150 newlines and skipped everyone with chickensmpac.chat.bypass, i.e. all ops/staff with `*`).
