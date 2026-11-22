/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 */
package dev.killeffects;

import dev.killeffects.KillEffect;
import dev.killeffects.KillEffects;
import dev.killeffects.Store;
import dev.killeffects.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

final class KillEffectsExpansion
extends PlaceholderExpansion {
    private final KillEffects plugin;

    KillEffectsExpansion(KillEffects killEffects) {
        this.plugin = killEffects;
    }

    public String getIdentifier() {
        return "killeffects";
    }

    public String getAuthor() {
        return "ChickenSMP";
    }

    public String getVersion() {
        return KillEffects.VERSION;
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(OfflinePlayer offlinePlayer, String string) {
        String string2 = string.toLowerCase();
        if (string2.equals("total")) {
            return String.valueOf(this.plugin.effects().size());
        }
        if (offlinePlayer == null) {
            return "";
        }
        Store.Profile profile = this.plugin.profile(offlinePlayer.getUniqueId());
        if (profile == null) {
            profile = new Store.Profile();
        }
        if (string2.startsWith("keys_")) {
            return String.valueOf(profile.keys(string2.substring(5)));
        }
        Player player = offlinePlayer.getPlayer();
        return switch (string2) {
            case "selected" -> {
                KillEffect var8_8;
                KillEffect v0 = var8_8 = player == null ? null : this.plugin.selected(player);
                if (var8_8 == null) {
                    yield this.plugin.raw("messages.none");
                }
                yield Text.color(var8_8.colored());
            }
            case "selected_id" -> {
                if (profile.selected == null) {
                    yield "";
                }
                yield profile.selected;
            }
            case "owned" -> String.valueOf(player == null ? profile.owned.size() : this.plugin.ownedCount(player));
            case "kills" -> KillEffects.num(profile.kills);
            case "opened" -> KillEffects.num(profile.opened);
            default -> null;
        };
    }
}

