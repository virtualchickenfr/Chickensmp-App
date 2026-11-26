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
        // crates were removed (2026-11-26): this menu only holds the Mystery Roll
        if (MysteryRoll.enabled(this.plugin)) this.inv.setItem(13, this.mysteryItem());
        this.inv.setItem(18, Gui.item(Material.ARROW, this.plugin.gui("back", new Object[0]), List.of()));
        this.inv.setItem(26, Gui.item(Material.BARRIER, this.plugin.gui("close", new Object[0]), List.of()));
        Gui.fill(this.inv, Material.BLACK_STAINED_GLASS_PANE);
    }

    private static final String MYSTERY = "\u0000mystery";

    private ItemStack mysteryItem() {
        boolean armedNow = MYSTERY.equals(this.armed) && System.currentTimeMillis() - this.armedAt < 5000L;
        int left = MysteryRoll.pool(this.plugin, this.viewer).size();
        double dc = MysteryRoll.doubleChance(this.plugin);
        List<String> lore = new ArrayList<String>();
        lore.add(Text.color("&7Kill Effects"));
        lore.add("");
        lore.add(Text.color("&#A303F9&lINFORMATION"));
        lore.add(Text.color("&#A303F9\u25AA &fGet a &#FCE300random &fkill effect"));
        lore.add(Text.color("&#A303F9\u25AA &fOnly effects you &#FCE300don't own &fyet"));
        lore.add(Text.color("&#A303F9\u25AA &fA big show everyone around sees"));
        lore.add(Text.color("&#A303F9\u25AA &#FCE300Legendary &fand &#FC0000Mythic &fpulls go in chat"));
        lore.add(Text.color("&#A303F9\u25AA &#FCE300" + CratesMenu.pct(dc) + "% &fchance for a &#FCE300&lDOUBLE"));
        lore.add("");
        lore.add(Text.color("&fLeft to get: &#FCE300" + left));
        lore.add(Text.color("&fPrice: &#00FC00" + this.plugin.money(MysteryRoll.price(this.plugin))));
        lore.add("");
        lore.add(Text.color(left == 0 ? "&#FC0000You own every kill effect!" : (armedNow ? "&#00FC00\u25B6 &#00FC00&l&nCLICK&r &#00FC00again to pay" : "&#A303F9\u25B6 &#A303F9&l&nCLICK&r &#A303F9to roll")));
        ItemStack it = Gui.item(Material.NETHER_STAR, Text.color("&#A303F9&lMYSTERY ROLL"), lore);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setEnchantmentGlintOverride(Boolean.valueOf(true)); it.setItemMeta(m); }
        return it;
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
        if (n == 13 && MysteryRoll.enabled(this.plugin)) {
            long now = System.currentTimeMillis();
            if (!MYSTERY.equals(this.armed) || now - this.armedAt >= 5000L) {
                this.armed = MYSTERY;
                this.armedAt = now;
                this.plugin.sound(player, "click");
                this.render();
                return;
            }
            this.armed = null;
            MysteryRoll.start(this.plugin, player);
            if (player.getOpenInventory().getTopInventory().getHolder() == this) this.render();
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

