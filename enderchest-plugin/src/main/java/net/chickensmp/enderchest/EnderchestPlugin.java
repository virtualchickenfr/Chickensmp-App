package net.chickensmp.enderchest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public final class EnderchestPlugin extends JavaPlugin implements Listener {
    /** Marks our inventory; one open view per owner, shared by everyone looking at it. */
    static final class Holder implements InventoryHolder {
        final UUID owner;
        Inventory inv;
        Holder(UUID owner) { this.owner = owner; }
        @Override public Inventory getInventory() { return inv; }
    }

    private final Map<UUID, Holder> open = new HashMap<>();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        new File(getDataFolder(), "data").mkdirs();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ChickenSMPEnderchest enabled (" + size() + " slots).");
    }

    @Override
    public void onDisable() {
        for (Holder h : new ArrayList<>(open.values())) {
            for (var v : new ArrayList<>(h.inv.getViewers())) v.closeInventory();
            save(h);
        }
        open.clear();
    }

    int size() { return Math.max(3, Math.min(6, getConfig().getInt("rows", 6))) * 9; }
    Component c(String s) { return LEGACY.deserialize(s); }
    void msg(CommandSender s, String key, String player) {
        s.sendMessage(c(getConfig().getString("prefix", "") + getConfig().getString("messages." + key, key).replace("%player%", player)));
    }

    /** Opens the big ender chest of {@code owner} for {@code viewer}. Owner must be online (vanilla part lives on the player). */
    void openFor(Player viewer, Player owner) {
        Holder h = open.get(owner.getUniqueId());
        if (h == null) {
            h = new Holder(owner.getUniqueId());
            h.inv = Bukkit.createInventory(h, size(), c(getConfig().getString("title", "&8Ender Chest")));
            ItemStack[] vanilla = owner.getEnderChest().getContents();
            for (int i = 0; i < vanilla.length && i < 27; i++) h.inv.setItem(i, vanilla[i]);
            ItemStack[] extra = loadExtra(owner.getUniqueId());
            for (int i = 0; i < extra.length && 27 + i < size(); i++) h.inv.setItem(27 + i, extra[i]);
            // items stored beyond the current size (rows lowered) are kept in the file untouched
            open.put(owner.getUniqueId(), h);
        }
        viewer.openInventory(h.inv);
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1f);
    }

    private File file(UUID u) { return new File(new File(getDataFolder(), "data"), u + ".dat"); }

    private ItemStack[] loadExtra(UUID u) {
        File f = file(u);
        if (!f.exists()) return new ItemStack[0];
        try {
            return ItemStack.deserializeItemsFromBytes(Files.readAllBytes(f.toPath()));
        } catch (Exception e) {
            getLogger().severe("Could not read ender chest data of " + u + ": " + e);
            // keep the broken file, never overwrite it
            try { Files.copy(f.toPath(), new File(f.getPath() + ".broken-" + System.currentTimeMillis()).toPath()); } catch (Exception ignored) { }
            return new ItemStack[0];
        }
    }

    /** Writes slots 0-26 back into the vanilla ender chest and the rest into the data file. */
    private void save(Holder h) {
        Player owner = Bukkit.getPlayer(h.owner);
        ItemStack[] all = h.inv.getContents();
        if (owner != null) {
            Inventory ec = owner.getEnderChest();
            for (int i = 0; i < 27; i++) ec.setItem(i, all[i]);
        } else {
            getLogger().warning("Owner of ender chest " + h.owner + " went offline before saving; vanilla part kept from last save.");
        }
        ItemStack[] old = loadExtra(h.owner);
        int keep = Math.max(old.length, all.length - 27);
        ItemStack[] extra = new ItemStack[keep];
        for (int i = 0; i < keep; i++) {
            int slot = 27 + i;
            extra[i] = slot < all.length ? all[slot] : old[i];
            if (extra[i] == null) extra[i] = ItemStack.empty();
        }
        try {
            File f = file(h.owner), tmp = new File(f.getPath() + ".tmp");
            Files.write(tmp.toPath(), ItemStack.serializeItemsAsBytes(Arrays.asList(extra)));
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            getLogger().severe("Could not save ender chest of " + h.owner + ": " + e);
        }
    }

    // ------------------------------------------------------------------ events

    /** Any vanilla ender chest view (block or EconomyCore /ec) is replaced by the big one. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        if (e.getInventory().getType() != InventoryType.ENDER_CHEST || !(e.getPlayer() instanceof Player p)) return;
        if (e.getInventory().getHolder() instanceof Holder) return;
        // The vanilla ender chest belongs to its holder (staff tools may open someone else's).
        // Offline/unknown holders are left alone.
        if (!(e.getInventory().getHolder() instanceof Player owner) || !owner.isOnline()) return;
        e.setCancelled(true);
        Bukkit.getScheduler().runTask(this, () -> { if (p.isOnline() && owner.isOnline()) openFor(p, owner); });
    }

    /** Ender chest blocks also work where clicking is blocked (spawn region). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Block b = e.getClickedBlock();
        if (b == null || b.getType() != Material.ENDER_CHEST) return;
        Player p = e.getPlayer();
        if (p.isSneaking() && p.getInventory().getItemInMainHand().getType() != Material.AIR) return;
        if (e.useInteractedBlock() != Event.Result.DENY) return; // vanilla opens it -> onOpen swaps it
        if (!getConfig().getBoolean("open-blocks-everywhere", true)) return;
        if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;
        Bukkit.getScheduler().runTask(this, () -> { if (p.isOnline()) openFor(p, p); });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder h)) return;
        Bukkit.getScheduler().runTask(this, () -> {
            if (h.inv.getViewers().isEmpty() && open.get(h.owner) == h) { save(h); open.remove(h.owner); }
            else save(h);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        Holder h = open.get(e.getPlayer().getUniqueId());
        if (h == null) return;
        // owner leaves: close every view first, save while the owner is still online
        for (var v : new ArrayList<>(h.inv.getViewers())) if (v != e.getPlayer()) v.closeInventory();
        save(h);
        open.remove(h.owner);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
        if (a.length != 1) { msg(s, "usage", ""); return true; }
        Player t = Bukkit.getPlayerExact(a[0]);
        if (t == null) { msg(s, "not-online", a[0]); return true; }
        openFor(p, t);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length != 1) return List.of();
        List<String> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(a[0].toLowerCase())) out.add(p.getName());
        return out;
    }
}
