/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package dev.tidal;

import dev.tidal.Catalog;
import dev.tidal.Gui;
import dev.tidal.MainMenu;
import dev.tidal.Tidal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class ShopMenu
implements Gui {
    private static final int[] SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int BACK = 45;
    private static final int BALANCE = 49;
    private static final int CLOSE = 53;
    private static final long CONFIRM_MS = 5000L;
    private final Tidal plugin;
    private final Inventory inv;
    private final Map<Integer, Catalog.Rod> slots = new HashMap<Integer, Catalog.Rod>();
    private String pending;
    private long pendingAt;

    private ShopMenu(Tidal tidal) {
        this.plugin = tidal;
        this.inv = Bukkit.createInventory((InventoryHolder)this, (int)54, (String)tidal.gui("shop.title", new Object[0]));
    }

    static void open(Tidal tidal, Player player) {
        ShopMenu shopMenu = new ShopMenu(tidal);
        shopMenu.render(player);
        player.openInventory(shopMenu.inv);
    }

    public Inventory getInventory() {
        return this.inv;
    }

    private void render(Player player) {
        int n;
        for (int i = 0; i < 54; ++i) {
            this.inv.setItem(i, null);
        }
        this.slots.clear();
        ArrayList<Catalog.Rod> arrayList = new ArrayList<Catalog.Rod>();
        for (Catalog.Rod rod : this.plugin.catalog().rods.values()) {
            if (!(rod.price() >= 0.0)) continue;
            arrayList.add(rod);
        }
        double d = this.plugin.economy().getBalance((OfflinePlayer)player);
        Catalog.Rod rod = this.plugin.items().heldRod(player);
        boolean bl = this.pending != null && System.currentTimeMillis() - this.pendingAt < 5000L;
        for (n = 0; n < arrayList.size() && n < SLOTS.length; ++n) {
            Catalog.Rod rod2 = (Catalog.Rod)arrayList.get(n);
            ItemStack itemStack = this.plugin.items().rod(rod2);
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                ArrayList<String> arrayList2;
                ArrayList<String> arrayList3 = arrayList2 = itemMeta.getLore() == null ? new ArrayList<String>() : new ArrayList(itemMeta.getLore());
                String string = bl && rod2.id().equals(this.pending) ? "shop.status-confirm" : (rod2.price() == 0.0 ? "shop.status-free" : (d >= rod2.price() ? "shop.status-buy" : "shop.status-poor"));
                arrayList2.addAll(this.plugin.guiList("shop.footer", "price", rod2.price() == 0.0 ? this.plugin.gui("shop.free", new Object[0]) : this.plugin.money(rod2.price()), "tier", n + 1, "tiers", arrayList.size()));
                if (rod != null && rod.id().equals(rod2.id())) {
                    arrayList2.addAll(this.plugin.guiList("shop.status-holding", new Object[0]));
                }
                arrayList2.addAll(this.plugin.guiList(string, "price", this.plugin.money(rod2.price()), "missing", this.plugin.money(Math.max(0.0, rod2.price() - d))));
                itemMeta.setLore(arrayList2);
                if (bl && rod2.id().equals(this.pending)) {
                    itemMeta.setEnchantmentGlintOverride(Boolean.valueOf(true));
                }
                itemStack.setItemMeta(itemMeta);
            }
            this.inv.setItem(SLOTS[n], itemStack);
            this.slots.put(SLOTS[n], rod2);
        }
        for (n = 45; n < 54; ++n) {
            this.inv.setItem(n, Gui.pane(Material.BLUE_STAINED_GLASS_PANE));
        }
        this.inv.setItem(45, Gui.item(Material.ARROW, this.plugin.gui("back", new Object[0]), List.of()));
        this.inv.setItem(49, Gui.item(Material.SUNFLOWER, this.plugin.gui("shop.balance", "amount", this.plugin.money(d)), this.plugin.guiList("shop.balance-lore", new Object[0])));
        this.inv.setItem(53, Gui.item(Material.BARRIER, this.plugin.gui("close", new Object[0]), List.of()));
        Gui.border(this.inv, Material.BLUE_STAINED_GLASS_PANE);
        Gui.fill(this.inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
    }

    @Override
    public void click(Player player, int n, boolean bl, boolean bl2) {
        boolean bl3;
        if (n == 45) {
            MainMenu.open(this.plugin, player);
            return;
        }
        if (n == 53) {
            player.closeInventory();
            return;
        }
        Catalog.Rod rod = this.slots.get(n);
        if (rod == null) {
            return;
        }
        boolean bl4 = bl3 = rod.id().equals(this.pending) && System.currentTimeMillis() - this.pendingAt < 5000L;
        if (!bl3 && rod.price() > 0.0) {
            if (!this.plugin.economy().has((OfflinePlayer)player, rod.price())) {
                this.plugin.send((CommandSender)player, "error.not-enough-money", "amount", this.plugin.money(rod.price()));
                return;
            }
            this.pending = rod.id();
            this.pendingAt = System.currentTimeMillis();
            this.plugin.sound(player, "click");
            this.render(player);
            return;
        }
        this.pending = null;
        if (rod.price() == 0.0 && !ShopMenu.freeRodAllowed(this.plugin, player)) {
            this.render(player);
            return;
        }
        this.plugin.buy(player, rod);
        this.render(player);
    }

    // --- ChickenSMP: free rods (price 0) only once per cooldown, stored in free-rods.yml ---
    private static org.bukkit.configuration.file.YamlConfiguration FREE;

    static boolean freeRodAllowed(Tidal plugin, Player player) {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "free-rods.yml");
        if (FREE == null) FREE = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        long minutes = plugin.getConfig().getLong("free-rod-cooldown-minutes", 60L);
        if (minutes <= 0L || player.hasPermission("tidal.freerod.bypass")) return true;
        String key = player.getUniqueId().toString();
        long now = System.currentTimeMillis();
        long last = FREE.getLong(key, 0L);
        long left = last + minutes * 60000L - now;
        if (left > 0L) {
            long m = (left + 59999L) / 60000L;
            String prefix = plugin.getConfig().getString("messages.prefix", "");
            String msg = plugin.getConfig().getString("messages.free-rod-cooldown", "{prefix}&#FC0000You can take a free rod again in &#FCE300{minutes} min&#FC0000.");
            player.sendMessage(Text.color(msg.replace("{prefix}", prefix).replace("{minutes}", String.valueOf(m))));
            plugin.sound(player, "error");
            return false;
        }
        FREE.set(key, now);
        try { FREE.save(file); } catch (java.io.IOException e) { plugin.getLogger().warning("Could not save free-rods.yml: " + e); }
        return true;
    }
}

