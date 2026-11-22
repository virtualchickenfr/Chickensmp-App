/*
 * Decompiled with CFR 0.152.
 */
package dev.killeffects;

import dev.killeffects.KillEffect;
import dev.killeffects.Rarity;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

record Crate(String id, String name, String texture, double price, Map<Rarity, Double> weights, List<String> lore) {
    Rarity rollRarity(Random random, Map<Rarity, List<KillEffect>> map) {
        double d = 0.0;
        for (Map.Entry<Rarity, Double> entry : this.weights.entrySet()) {
            if (map.getOrDefault((Object)entry.getKey(), List.of()).isEmpty()) continue;
            d += entry.getValue().doubleValue();
        }
        if (d <= 0.0) {
            return null;
        }
        double d2 = random.nextDouble() * d;
        Rarity rarity = null;
        for (Map.Entry<Rarity, Double> entry : this.weights.entrySet()) {
            if (map.getOrDefault((Object)entry.getKey(), List.of()).isEmpty() || entry.getValue() <= 0.0) continue;
            rarity = entry.getKey();
            if (!((d2 -= entry.getValue().doubleValue()) <= 0.0)) continue;
            return entry.getKey();
        }
        return rarity;
    }

    KillEffect roll(Random random, Map<Rarity, List<KillEffect>> map) {
        Rarity rarity = this.rollRarity(random, map);
        if (rarity == null) {
            return null;
        }
        List<KillEffect> list = map.get((Object)rarity);
        return list.get(random.nextInt(list.size()));
    }

    Map<Rarity, Double> percents(Map<Rarity, List<KillEffect>> map) {
        double d = 0.0;
        for (Map.Entry<Rarity, Double> entry : this.weights.entrySet()) {
            if (map.getOrDefault((Object)entry.getKey(), List.of()).isEmpty()) continue;
            d += ((Double)entry.getValue()).doubleValue();
        }
        EnumMap enumMap = new EnumMap(Rarity.class);
        for (Map.Entry entry : this.weights.entrySet()) {
            if ((Double)entry.getValue() <= 0.0 || map.getOrDefault(entry.getKey(), List.of()).isEmpty()) continue;
            enumMap.put((Rarity)((Object)entry.getKey()), d <= 0.0 ? 0.0 : (Double)entry.getValue() * 100.0 / d);
        }
        return enumMap;
    }

    static List<String> list(List<String> arrayList) {
        return arrayList == null ? new ArrayList() : arrayList;
    }
}

