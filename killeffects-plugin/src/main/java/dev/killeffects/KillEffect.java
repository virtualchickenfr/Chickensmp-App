/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 */
package dev.killeffects;

import dev.killeffects.Fx;
import dev.killeffects.Rarity;
import java.util.function.Consumer;
import org.bukkit.Material;

record KillEffect(String id, String name, Rarity rarity, String description, Material fallback, Consumer<Fx> play) {
    String permission() {
        return "killeffects.effect." + this.id;
    }

    String colored() {
        return this.rarity.color + this.name;
    }
}

