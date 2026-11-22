/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package dev.killeffects;

import dev.killeffects.Crate;
import dev.killeffects.CratesMenu;
import dev.killeffects.Gui;
import dev.killeffects.KillEffect;
import dev.killeffects.KillEffects;
import dev.killeffects.Rarity;
import dev.killeffects.Store;
import dev.killeffects.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class CollectionMenu
implements Gui {
    private static final int PER_PAGE = 45;
    private static final int PREV = 45;
    private static final int FILTER = 46;
    private static final int SELECTED = 47;
    private static final int CRATES = 49;
    private static final int PROFILE = 51;
    private static final int NEXT = 53;
    private final KillEffects plugin;
    private final Player viewer;
    private final Inventory inv;
    private final Map<Integer, KillEffect> slots = new HashMap<Integer, KillEffect>();
    private int page;
    private Rarity filter;

    private CollectionMenu(KillEffects killEffects, Player player, int n, Rarity rarity) {
        this.plugin = killEffects;
        this.viewer = player;
        this.page = n;
        this.filter = rarity;
        this.inv = Bukkit.createInventory((InventoryHolder)this, (int)54, (String)killEffects.gui("collection.title", new Object[0]));
    }

    static void open(KillEffects killEffects, Player player, int n, Rarity rarity) {
        CollectionMenu collectionMenu = new CollectionMenu(killEffects, player, n, rarity);
        collectionMenu.render();
        player.openInventory(collectionMenu.inv);
        killEffects.sound(player, "open");
    }

    public Inventory getInventory() {
        return this.inv;
    }

    private List<KillEffect> visible() {
        ArrayList<KillEffect> arrayList = new ArrayList<KillEffect>();
        for (KillEffect killEffect3 : this.plugin.effects().values()) {
            if (this.filter != null && killEffect3.rarity() != this.filter) continue;
            arrayList.add(killEffect3);
        }
        arrayList.sort((killEffect, killEffect2) -> killEffect.rarity() != killEffect2.rarity() ? Integer.compare(killEffect.rarity().ordinal(), killEffect2.rarity().ordinal()) : 0);
        return arrayList;
    }

    private int pages() {
        return Math.max(1, (this.visible().size() + 45 - 1) / 45);
    }

    private void render() {
        int n;
        int n2;
        for (int i = 0; i < 54; ++i) {
            this.inv.setItem(i, null);
        }
        this.slots.clear();
        Store.Profile profile = this.plugin.profileOrCreate(this.viewer.getUniqueId(), this.viewer.getName());
        KillEffect killEffect = this.plugin.selected(this.viewer);
        List<KillEffect> list = this.visible();
        this.page = Math.max(0, Math.min(this.page, this.pages() - 1));
        for (n2 = 0; n2 < 45 && (n = this.page * 45 + n2) < list.size(); ++n2) {
            ItemStack itemStack;
            KillEffect killEffect2 = list.get(n);
            boolean bl = this.plugin.owns(this.viewer, killEffect2);
            ArrayList<String> arrayList = new ArrayList<String>(this.plugin.effectLore(killEffect2));
            if (bl) {
                boolean bl2 = killEffect != null && killEffect.id().equals(killEffect2.id());
                arrayList.addAll(this.plugin.guiList(bl2 ? "collection.selected-lore" : "collection.owned-lore", new Object[0]));
                itemStack = this.plugin.icon(killEffect2, true, this.plugin.gui("collection.name", "effect", killEffect2.colored()), arrayList);
                if (bl2) {
                    CollectionMenu.glint(itemStack);
                }
            } else {
                arrayList.addAll(this.plugin.guiList("collection.locked-lore", new Object[0]));
                itemStack = this.plugin.icon(killEffect2, false, this.plugin.gui("collection.locked-name", "effect", killEffect2.name()), arrayList);
            }
            this.inv.setItem(n2, itemStack);
            this.slots.put(n2, killEffect2);
        }
        for (n2 = 45; n2 < 54; ++n2) {
            this.inv.setItem(n2, Gui.pane(Material.BLACK_STAINED_GLASS_PANE));
        }
        if (this.page > 0) {
            this.inv.setItem(45, Gui.item(Material.ARROW, this.plugin.gui("previous-page", "page", this.page), List.of()));
        }
        if (this.page < this.pages() - 1) {
            this.inv.setItem(53, Gui.item(Material.ARROW, this.plugin.gui("next-page", "page", this.page + 2), List.of()));
        }
        this.inv.setItem(46, this.filterItem());
        this.inv.setItem(47, killEffect == null ? Gui.item(Material.GRAY_DYE, this.plugin.gui("collection.none-selected", new Object[0]), this.plugin.guiList("collection.none-selected-lore", new Object[0])) : this.plugin.icon(killEffect, true, this.plugin.gui("collection.current", "effect", killEffect.colored()), this.plugin.guiList("collection.current-lore", new Object[0])));
        Crate crate = this.plugin.crates().isEmpty() ? null : this.plugin.crates().values().iterator().next();
        this.inv.setItem(49, this.plugin.textured("crate/" + (crate == null ? "common" : crate.texture()), Material.ENDER_CHEST, this.plugin.gui("collection.crates", new Object[0]), this.plugin.guiList("collection.crates-lore", new Object[0])));
        n = this.plugin.ownedCount(this.viewer);
        int n3 = this.plugin.effects().size();
        this.inv.setItem(51, Gui.head(this.viewer.getUniqueId(), this.plugin.gui("collection.profile", "player", this.viewer.getName()), this.plugin.guiList("collection.profile-lore", "owned", n, "total", n3, "percent", n3 == 0 ? 0 : n * 100 / n3, "kills", KillEffects.num(profile.kills), "opened", KillEffects.num(profile.opened))));
    }

    private ItemStack filterItem() {
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add(Text.color((this.filter == null ? "&f\u00bb " : "&8  ") + this.plugin.raw("gui.collection.filter-all")));
        for (Rarity rarity : Rarity.values()) {
            arrayList.add(Text.color((this.filter == rarity ? "&f\u00bb " : "&8  ") + rarity.color + rarity.display + " &8(" + this.plugin.byRarity().get((Object)rarity).size() + ")"));
        }
        arrayList.addAll(this.plugin.guiList("collection.filter-lore", new Object[0]));
        return Gui.item(Material.HOPPER, this.plugin.gui("collection.filter", new Object[0]), arrayList);
    }

    private static void glint(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setEnchantmentGlintOverride(Boolean.valueOf(true));
            itemStack.setItemMeta(itemMeta);
        }
    }

    @Override
    public void click(Player player, int n, boolean bl, boolean bl2) {
        switch (n) {
            case 45: {
                if (this.page > 0) {
                    --this.page;
                    this.plugin.sound(player, "click");
                    this.render();
                }
                return;
            }
            case 53: {
                if (this.page < this.pages() - 1) {
                    ++this.page;
                    this.plugin.sound(player, "click");
                    this.render();
                }
                return;
            }
            case 46: {
                Rarity[] rarityArray = Rarity.values();
                int n2 = this.filter == null ? -1 : this.filter.ordinal();
                int n3 = n2 = bl ? n2 - 1 : n2 + 1;
                if (n2 < -1) {
                    n2 = rarityArray.length - 1;
                }
                if (n2 >= rarityArray.length) {
                    n2 = -1;
                }
                this.filter = n2 == -1 ? null : rarityArray[n2];
                this.page = 0;
                this.plugin.sound(player, "click");
                this.render();
                return;
            }
            case 47: {
                if (this.plugin.selected(player) != null) {
                    this.plugin.select(player, null);
                    this.render();
                }
                return;
            }
            case 49: {
                CratesMenu.open(this.plugin, player);
                return;
            }
        }
        KillEffect killEffect = this.slots.get(n);
        if (killEffect == null) {
            return;
        }
        if (bl && !bl2) {
            if (!player.hasPermission("killeffects.preview")) {
                this.plugin.send((CommandSender)player, "error.no-permission", new Object[0]);
                return;
            }
            player.closeInventory();
            if (this.plugin.preview(player, killEffect)) {
                this.plugin.send((CommandSender)player, "preview", "effect", Text.color(killEffect.colored()));
            }
            return;
        }
        if (this.plugin.owns(player, killEffect)) {
            KillEffect killEffect2 = this.plugin.selected(player);
            this.plugin.select(player, (KillEffect)(killEffect2 != null && killEffect2.id().equals(killEffect.id()) ? null : killEffect));
            this.render();
            return;
        }
        this.plugin.send((CommandSender)player, "error.locked", "effect", Text.color(killEffect.colored()));
        this.plugin.sound(player, "error");
    }
}

