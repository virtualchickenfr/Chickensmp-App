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
 */
package dev.killeffects;

import dev.killeffects.CollectionMenu;
import dev.killeffects.Crate;
import dev.killeffects.Gui;
import dev.killeffects.KillEffect;
import dev.killeffects.KillEffects;
import dev.killeffects.Rarity;
import dev.killeffects.RollMenu;
import dev.killeffects.Store;
import dev.killeffects.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class CratesMenu
implements Gui {
    private static final int BACK = 18;
    private static final int CLOSE = 26;
    private final KillEffects plugin;
    private final Player viewer;
    private final Inventory inv;
    private final Map<Integer, Crate> slots = new HashMap<Integer, Crate>();
    private String armed;
    private long armedAt;

    private CratesMenu(KillEffects killEffects, Player player) {
        this.plugin = killEffects;
        this.viewer = player;
        this.inv = Bukkit.createInventory((InventoryHolder)this, (int)27, (String)killEffects.gui("crates.title", new Object[0]));
    }

    static void open(KillEffects killEffects, Player player) {
        CratesMenu cratesMenu = new CratesMenu(killEffects, player);
        cratesMenu.render();
        player.openInventory(cratesMenu.inv);
        killEffects.sound(player, "open");
    }

    public Inventory getInventory() {
        return this.inv;
    }

    private static int[] layout(int n) {
        int[] nArray;
        switch (n) {
            case 1: {
                int[] nArray2 = new int[1];
                nArray = nArray2;
                nArray2[0] = 13;
                break;
            }
            case 2: {
                int[] nArray3 = new int[2];
                nArray3[0] = 11;
                nArray = nArray3;
                nArray3[1] = 15;
                break;
            }
            case 3: {
                int[] nArray4 = new int[3];
                nArray4[0] = 11;
                nArray4[1] = 13;
                nArray = nArray4;
                nArray4[2] = 15;
                break;
            }
            case 4: {
                int[] nArray5 = new int[4];
                nArray5[0] = 10;
                nArray5[1] = 12;
                nArray5[2] = 14;
                nArray = nArray5;
                nArray5[3] = 16;
                break;
            }
            default: {
                int[] nArray6 = new int[9];
                nArray6[0] = 9;
                nArray6[1] = 11;
                nArray6[2] = 13;
                nArray6[3] = 15;
                nArray6[4] = 17;
                nArray6[5] = 10;
                nArray6[6] = 12;
                nArray6[7] = 14;
                nArray = nArray6;
                nArray6[8] = 16;
            }
        }
        return nArray;
    }

    private void render() {
        for (int i = 0; i < 27; ++i) {
            this.inv.setItem(i, null);
        }
        this.slots.clear();
        Store.Profile profile = this.plugin.profileOrCreate(this.viewer.getUniqueId(), this.viewer.getName());
        ArrayList<Crate> arrayList = new ArrayList<Crate>(this.plugin.crates().values());
        int[] nArray = CratesMenu.layout(arrayList.size());
        boolean bl = System.currentTimeMillis() - this.armedAt < 5000L;
        for (int i = 0; i < arrayList.size() && i < nArray.length; ++i) {
            Object object;
            Crate crate = (Crate)arrayList.get(i);
            ArrayList<String> arrayList2 = new ArrayList<String>();
            for (String object2 : crate.lore()) {
                arrayList2.add(Text.color(object2));
            }
            arrayList2.addAll(this.plugin.guiList("crates.chances-header", new Object[0]));
            for (Map.Entry<Rarity, Double> entry : crate.percents(this.plugin.byRarity()).entrySet()) {
                Rarity rr = entry.getKey();
                long l = this.plugin.byRarity().get(rr).stream().filter(killEffect -> this.plugin.owns(this.viewer, (KillEffect)killEffect)).count();
                arrayList2.add(Text.color(KillEffects.fill(this.plugin.raw("gui.crates.chance-line"), "rarity", rr.color + rr.display, "chance", CratesMenu.pct((Double)entry.getValue()), "have", l, "total", this.plugin.byRarity().get(rr).size())));
            }
            int n = profile.keys(crate.id());
            boolean bl2 = crate.price() > 0.0 && this.plugin.economy() != null;
            arrayList2.addAll(this.plugin.guiList("crates.price-lore", "keys", n, "price", bl2 ? this.plugin.money(crate.price()) : this.plugin.raw("gui.crates.keys-only")));
            object = n > 0 ? "crates.open-key" : (bl2 ? (bl && crate.id().equals(this.armed) ? "crates.confirm" : "crates.open-money") : "crates.no-key");
            arrayList2.addAll(this.plugin.guiList((String)object, "price", this.plugin.money(crate.price())));
            ItemStack itemStack = this.plugin.textured("crate/" + crate.texture(), Material.CHEST, crate.name(), arrayList2);
            if (n > 0) {
                itemStack.setAmount(Math.min(64, n));
                ItemMeta itemMeta = itemStack.getItemMeta();
                if (itemMeta != null) {
                    itemMeta.setEnchantmentGlintOverride(Boolean.valueOf(true));
                    itemStack.setItemMeta(itemMeta);
                }
            }
            this.inv.setItem(nArray[i], itemStack);
            this.slots.put(nArray[i], crate);
        }
        this.inv.setItem(18, Gui.item(Material.ARROW, this.plugin.gui("back", new Object[0]), List.of()));
        this.inv.setItem(26, Gui.item(Material.BARRIER, this.plugin.gui("close", new Object[0]), List.of()));
        Gui.fill(this.inv, Material.BLACK_STAINED_GLASS_PANE);
    }

    static String pct(double d) {
        return d >= 10.0 ? String.format(Locale.ROOT, "%.0f", d) : (d >= 1.0 ? String.format(Locale.ROOT, "%.1f", d) : String.format(Locale.ROOT, "%.2f", d));
    }

    @Override
    public void click(Player player, int n, boolean bl, boolean bl2) {
        if (n == 18) {
            CollectionMenu.open(this.plugin, player, 0, null);
            return;
        }
        if (n == 26) {
            player.closeInventory();
            return;
        }
        Crate crate = this.slots.get(n);
        if (crate == null) {
            return;
        }
        Store.Profile profile = this.plugin.profileOrCreate(player.getUniqueId(), player.getName());
        if (profile.keys(crate.id()) <= 0 && crate.price() > 0.0 && this.plugin.economy() != null) {
            long l = System.currentTimeMillis();
            if (!crate.id().equals(this.armed) || l - this.armedAt >= 5000L) {
                this.armed = crate.id();
                this.armedAt = l;
                this.plugin.sound(player, "click");
                this.render();
                return;
            }
        }
        this.armed = null;
        KillEffects.Prize prize = this.plugin.openCrate(player, crate, false);
        if (prize == null) {
            this.render();
            return;
        }
        RollMenu.open(this.plugin, player, crate, prize);
    }
}

