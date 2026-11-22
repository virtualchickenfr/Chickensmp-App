/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 */
package dev.killeffects;

import dev.killeffects.Text;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

interface Gui
extends InventoryHolder {
    public void click(Player var1, int var2, boolean var3, boolean var4);

    public static ItemStack item(Material material, String string, List<String> list) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(Text.color(string));
            itemMeta.setLore(list.stream().map(Text::color).toList());
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    public static ItemStack head(UUID uUID, String string, List<String> list) {
        ItemMeta itemMeta;
        ItemStack itemStack = Gui.item(Material.PLAYER_HEAD, string, list);
        if (uUID != null && (itemMeta = itemStack.getItemMeta()) instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta)itemMeta;
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer((UUID)uUID));
            itemStack.setItemMeta((ItemMeta)skullMeta);
        }
        return itemStack;
    }

    public static ItemStack pane(Material material) {
        return Gui.item(material, " ", List.of());
    }

    public static void fill(Inventory inventory, Material material) {
        ItemStack itemStack = Gui.pane(material);
        for (int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack2 = inventory.getContents()[i];
            if (itemStack2 != null && !itemStack2.isEmpty()) continue;
            inventory.setItem(i, itemStack);
        }
    }

    public static void border(Inventory inventory, Material material) {
        ItemStack itemStack = Gui.pane(material);
        int n = inventory.getSize() / 9;
        for (int i = 0; i < inventory.getSize(); ++i) {
            int n2 = i / 9;
            int n3 = i % 9;
            if (n2 != 0 && n2 != n - 1 && n3 != 0 && n3 != 8) continue;
            inventory.setItem(i, itemStack);
        }
    }
}

