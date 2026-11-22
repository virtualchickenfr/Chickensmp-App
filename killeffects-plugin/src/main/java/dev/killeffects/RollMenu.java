/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package dev.killeffects;

import dev.killeffects.Crate;
import dev.killeffects.CratesMenu;
import dev.killeffects.Gui;
import dev.killeffects.KillEffect;
import dev.killeffects.KillEffects;
import dev.killeffects.Rarity;
import dev.killeffects.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

final class RollMenu
implements Gui {
    private static final int RESULT = 40;
    private static final int STOP = 36;
    private final KillEffects plugin;
    private final Player viewer;
    private final Crate crate;
    private final KillEffects.Prize prize;
    private final Inventory inv;
    private final List<KillEffect> reel = new ArrayList<KillEffect>();
    private boolean done;

    private RollMenu(KillEffects killEffects, Player player, Crate crate, KillEffects.Prize prize) {
        this.plugin = killEffects;
        this.viewer = player;
        this.crate = crate;
        this.prize = prize;
        this.inv = Bukkit.createInventory((InventoryHolder)this, (int)27, (String)killEffects.gui("roll.title", "crate", crate.name()));
    }

    static void open(KillEffects killEffects, Player player, Crate crate, KillEffects.Prize prize) {
        RollMenu rollMenu = new RollMenu(killEffects, player, crate, prize);
        rollMenu.build();
        rollMenu.frame(Material.BLACK_STAINED_GLASS_PANE);
        rollMenu.show(0);
        player.openInventory(rollMenu.inv);
        killEffects.sound(player, "crate-open");
        rollMenu.step(0);
    }

    public Inventory getInventory() {
        return this.inv;
    }

    private void build() {
        for (int i = 0; i < 46; ++i) {
            KillEffect killEffect = this.crate.roll(this.plugin.random, this.plugin.byRarity());
            this.reel.add(killEffect == null ? this.prize.effect() : killEffect);
        }
        this.reel.set(40, this.prize.effect());
        if (this.prize.effect().rarity().ordinal() < Rarity.LEGENDARY.ordinal() && this.plugin.random.nextInt(100) < 55) {
            for (Rarity rarity : new Rarity[]{Rarity.MYTHIC, Rarity.LEGENDARY}) {
                List<KillEffect> list = this.plugin.byRarity().get((Object)rarity);
                if (list.isEmpty()) continue;
                this.reel.set(this.plugin.random.nextBoolean() ? 41 : 39, list.get(this.plugin.random.nextInt(list.size())));
                break;
            }
        }
    }

    private void frame(Material material) {
        for (int i = 0; i < 9; ++i) {
            this.inv.setItem(i, Gui.pane(material));
            this.inv.setItem(18 + i, Gui.pane(material));
        }
        this.inv.setItem(4, Gui.item(Material.HOPPER, this.plugin.gui("roll.pointer", new Object[0]), List.of()));
        this.inv.setItem(22, Gui.item(Material.HOPPER, this.plugin.gui("roll.pointer", new Object[0]), List.of()));
    }

    private void show(int n) {
        for (int i = 0; i < 9; ++i) {
            KillEffect killEffect = this.reel.get(n + i);
            ItemStack itemStack = this.plugin.icon(killEffect, true, this.plugin.gui("roll.item", "effect", killEffect.colored()), List.of(Text.color(killEffect.rarity().label())));
            this.inv.setItem(9 + i, itemStack);
        }
    }

    private void step(int n) {
        this.show(n);
        if (this.viewer.isOnline()) {
            this.plugin.playSound(this.viewer, this.plugin.getConfig().getString("sounds.roll-tick"), (float)n / 36.0f * 0.9f);
        }
        if (n >= 36) {
            this.finish();
            return;
        }
        double d = (double)n / 36.0;
        long l = 1L + Math.round(Math.pow(d, 3.0) * 9.0);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.step(n + 1), l);
    }

    private void finish() {
        if (this.done) {
            return;
        }
        this.done = true;
        KillEffect killEffect = this.prize.effect();
        Rarity rarity = killEffect.rarity();
        this.frame(rarity.pane);
        ItemStack itemStack = this.plugin.icon(killEffect, true, this.plugin.gui("roll.item", "effect", killEffect.colored()), this.plugin.effectLore(killEffect));
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setEnchantmentGlintOverride(Boolean.valueOf(true));
            itemStack.setItemMeta(itemMeta);
        }
        this.inv.setItem(13, itemStack);
        if (this.viewer.isOnline()) {
            this.viewer.sendTitle(Text.color(rarity.label()), Text.color(killEffect.colored() + (this.prize.duplicate() ? (this.prize.refund() > 0.0 ? this.plugin.raw("gui.roll.duplicate-suffix").replace("{money}", this.plugin.money(this.prize.refund())) : this.plugin.raw("gui.roll.duplicate-suffix-nothing")) : "")), 5, 50, 15);
            this.plugin.sound(this.viewer, "win-" + rarity.key());
            if (rarity.ordinal() >= Rarity.EPIC.ordinal()) {
                this.viewer.spawnParticle(Particle.TOTEM_OF_UNDYING, this.viewer.getLocation().add(0.0, 1.0, 0.0), 30 + rarity.ordinal() * 20, 0.6, 0.8, 0.6, 0.4);
            }
        }
        this.plugin.announce(this.viewer, this.crate, this.prize);
    }

    @Override
    public void click(Player player, int n, boolean bl, boolean bl2) {
        if (this.done && n == 13) {
            CratesMenu.open(this.plugin, player);
        }
    }
}

