/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 */
package dev.killeffects;

import java.util.Locale;
import org.bukkit.Material;

enum Rarity {
    COMMON("Common", "&#AAAAAA", 0xAAAAAA, Material.LIGHT_GRAY_STAINED_GLASS_PANE),
    UNCOMMON("Uncommon", "&#00FC00", 0x00FC00, Material.LIME_STAINED_GLASS_PANE),
    RARE("Rare", "&#00A4FC", 0x00A4FC, Material.LIGHT_BLUE_STAINED_GLASS_PANE),
    EPIC("Epic", "&#A303F9", 0xA303F9, Material.PURPLE_STAINED_GLASS_PANE),
    LEGENDARY("Legendary", "&#FCE300", 0xFCE300, Material.YELLOW_STAINED_GLASS_PANE),
    MYTHIC("Mythic", "&#FC0000", 0xFC0000, Material.RED_STAINED_GLASS_PANE);

    final String display;
    final String color;
    final int rgb;
    final Material pane;

    private Rarity(String string2, String string3, int n2, Material material) {
        this.display = string2;
        this.color = string3;
        this.rgb = n2;
        this.pane = material;
    }

    String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    String label() {
        return this.color + "&l" + this.display.toUpperCase(Locale.ROOT);
    }

    static Rarity parse(String string) {
        if (string == null) {
            return null;
        }
        for (Rarity rarity : Rarity.values()) {
            if (!rarity.name().equalsIgnoreCase(string)) continue;
            return rarity;
        }
        return null;
    }
}

