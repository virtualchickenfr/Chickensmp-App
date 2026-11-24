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
package dev.cookie;

import dev.cookie.AdminMenu;
import dev.cookie.BuildingsMenu;
import dev.cookie.CookieClicker;
import dev.cookie.Gui;
import dev.cookie.Model;
import dev.cookie.PrestigeMenu;
import dev.cookie.SellMenu;
import dev.cookie.TopMenu;
import dev.cookie.UpgradesMenu;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class MainMenu
implements Gui,
CookieClicker.Live {
    private static final int PROFILE = 4;
    private static final int COOKIE = 13;
    private static final int BUILDINGS = 20;
    private static final int UPGRADES = 21;
    private static final int SELL = 22;
    private static final int TOP = 23;
    private static final int PRESTIGE = 24;
    private static final int HELP = 31;
    private static final int CLOSE = 40;
    private static final int ADMIN = 44;
    private final CookieClicker plugin;
    private final Player viewer;
    private final Inventory inv;

    private MainMenu(CookieClicker cookieClicker, Player player) {
        this.plugin = cookieClicker;
        this.viewer = player;
        this.inv = Bukkit.createInventory((InventoryHolder)this, (int)45, (String)cookieClicker.gui("main.title", new Object[0]));
    }

    static void open(CookieClicker cookieClicker, Player player) {
        MainMenu mainMenu = new MainMenu(cookieClicker, player);
        mainMenu.render();
        player.openInventory(mainMenu.inv);
        cookieClicker.sound(player, "open");
    }

    public Inventory getInventory() {
        return this.inv;
    }

    @Override
    public void refresh() {
        this.inv.setItem(13, this.cookie());
        this.inv.setItem(4, this.profile());
    }

    private void render() {
        Model.Profile profile = this.plugin.profileOrCreate(this.viewer.getUniqueId(), this.viewer.getName());
        Model.Rules rules = this.plugin.rules();
        this.inv.setItem(4, this.profile());
        this.inv.setItem(13, this.cookie());
        if (!rules.buildings.isEmpty()) this.inv.setItem(20, Gui.item(Material.BRICKS, this.plugin.gui("main.buildings", new Object[0]), this.plugin.guiList("main.buildings-lore", "owned", profile.buildingCount(), "cps", this.plugin.num(rules.baseCps(profile)))));
        this.inv.setItem(21, Gui.item(Material.ANVIL, this.plugin.gui("main.upgrades", new Object[0]), this.plugin.guiList("main.upgrades-lore", new Object[0])));
        this.inv.setItem(22, Gui.item(Material.EMERALD, this.plugin.gui("main.sell", new Object[0]), this.plugin.guiList("main.sell-lore", "price", this.priceText(rules.sellPrice(profile)))));
        this.inv.setItem(23, Gui.item(Material.GOLD_BLOCK, this.plugin.gui("main.top", new Object[0]), this.plugin.guiList("main.top-lore", new Object[0])));
        int n = rules.prestigeGain(profile);
        this.inv.setItem(24, Gui.item(Material.NETHER_STAR, this.plugin.gui("main.prestige", new Object[0]), this.plugin.guiList(n > 0 ? "main.prestige-ready" : "main.prestige-lore", "level", profile.prestige, "gain", n, "next", this.plugin.num(rules.nextPrestigeAt(profile)))));
        this.inv.setItem(31, Gui.item(Material.OAK_SIGN, this.plugin.gui("main.help", new Object[0]), this.plugin.guiList("main.help-lore", new Object[0])));
        this.inv.setItem(40, Gui.item(Material.BARRIER, this.plugin.gui("close", new Object[0]), List.of()));
        Gui.border(this.inv, Material.BROWN_STAINED_GLASS_PANE);
        if (this.viewer.hasPermission("cookie.admin")) {
            this.inv.setItem(44, Gui.item(Material.COMMAND_BLOCK, this.plugin.gui("main.admin", new Object[0]), this.plugin.guiList("main.admin-lore", new Object[0])));
        }
        Gui.fill(this.inv, Material.ORANGE_STAINED_GLASS_PANE);
    }

    private ItemStack cookie() {
        ItemStack itemStack;
        ItemMeta itemMeta;
        Model.Profile profile = this.plugin.profileOrCreate(this.viewer.getUniqueId(), this.viewer.getName());
        Model.Rules rules = this.plugin.rules();
        long l = System.currentTimeMillis();
        ArrayList<String> arrayList = new ArrayList<String>(this.plugin.guiList("main.cookie-lore", "cps", this.plugin.num(rules.cps(profile, l)), "per_click", this.plugin.num(rules.perClick(profile, l)), "golden", Model.shortNum(rules.goldenChance(profile)), "crit", Model.shortNum(rules.critChance(profile)), "auto", Model.shortNum(rules.autoClicks(profile)), "bonus", Model.shortNum((double)profile.prestige * rules.prestigeBonus)));
        if (profile.frenzyUntil > l) {
            arrayList.add(this.plugin.gui("main.frenzy", "seconds", (profile.frenzyUntil - l + 999L) / 1000L));
        }
        if (profile.clickFrenzyUntil > l) {
            arrayList.add(this.plugin.gui("main.click-frenzy", "seconds", (profile.clickFrenzyUntil - l + 999L) / 1000L));
        }
        if (this.plugin.getConfig().getBoolean("clicking.menu-cookie", false)) {
            arrayList.addAll(this.plugin.guiList("main.cookie-click", new Object[0]));
        }
        if ((itemMeta = (itemStack = Gui.item(Material.COOKIE, this.plugin.gui("main.cookie", "cookies", this.plugin.num(profile.cookies)), arrayList)).getItemMeta()) != null && (profile.frenzyUntil > l || profile.clickFrenzyUntil > l)) {
            itemMeta.setEnchantmentGlintOverride(Boolean.valueOf(true));
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private ItemStack profile() {
        Model.Profile profile = this.plugin.profileOrCreate(this.viewer.getUniqueId(), this.viewer.getName());
        return Gui.head(this.viewer.getUniqueId(), this.plugin.gui("main.profile", "player", this.viewer.getName()), this.plugin.guiList("main.profile-lore", "all_time", this.plugin.num(profile.allTime), "baked", this.plugin.num(profile.baked), "clicks", this.plugin.num(profile.clicks), "golden", profile.golden, "buildings", profile.buildingCount(), "sold", this.plugin.num(profile.sold), "prestige", profile.prestige, "money", this.plugin.money(profile.money)));
    }

    private String priceText(double d) {
        return d >= 0.01 ? this.plugin.money(d) : this.plugin.gui("main.price-per-k", "price", this.plugin.money(d * 1000.0));
    }

    @Override
    public void click(Player player, int n, boolean bl, boolean bl2) {
        switch (n) {
            case 13: {
                if (!this.plugin.getConfig().getBoolean("clicking.menu-cookie", false)) break;
                this.plugin.click(player, null);
                this.refresh();
                break;
            }
            case 20: {
                if (this.plugin.rules().buildings.isEmpty()) break;
                BuildingsMenu.open(this.plugin, player);
                break;
            }
            case 21: {
                UpgradesMenu.open(this.plugin, player);
                break;
            }
            case 22: {
                SellMenu.open(this.plugin, player);
                break;
            }
            case 23: {
                TopMenu.open(this.plugin, player, "baked");
                break;
            }
            case 24: {
                PrestigeMenu.open(this.plugin, player);
                break;
            }
            case 44: {
                if (!player.hasPermission("cookie.admin")) break;
                AdminMenu.open(this.plugin, player);
                break;
            }
            case 40: {
                player.closeInventory();
                break;
            }
        }
    }
}

