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
    COMMON("Common", "&7", 0xAAAAAA, Material.LIGHT_GRAY_STAINED_GLASS_PANE),
    UNCOMMON("Uncommon", "&a", 0x55FF55, Material.LIME_STAINED_GLASS_PANE),
    RARE("Rare", "&#3B9BFF", 3906559, Material.LIGHT_BLUE_STAINED_GLASS_PANE),
    EPIC("Epic", "&#B565FF", 11888127, Material.PURPLE_STAINED_GLASS_PANE),
    LEGENDARY("Legendary", "&#FFB020", 16756768, Material.ORANGE_STAINED_GLASS_PANE),
    MYTHIC("Mythic", "&#FF4D6D", 16731501, Material.RED_STAINED_GLASS_PANE);

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

