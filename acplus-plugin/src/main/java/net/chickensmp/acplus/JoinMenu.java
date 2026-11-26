package net.chickensmp.acplus;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

/**
 * Opens a menu (default /guide = /help) when a player joins: right after the resource pack has loaded,
 * or after a fallback delay for players without the pack (Bedrock). Once per join.
 */
final class JoinMenu implements Listener {
    private final SusPlugin plugin;
    private final Set<UUID> pending = new HashSet<>();

    JoinMenu(SusPlugin plugin) { this.plugin = plugin; }

    private boolean enabled() { return plugin.getConfig().getBoolean("join-menu.enabled", true); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        if (!enabled()) return;
        Player p = e.getPlayer();
        pending.add(p.getUniqueId());
        int fallback = plugin.getConfig().getInt("join-menu.fallback-delay-ticks", 100);
        Bukkit.getScheduler().runTaskLater(plugin, () -> open(p), fallback);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPack(PlayerResourcePackStatusEvent e) {
        if (e.getStatus() != PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) return;
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> open(p), 10);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { pending.remove(e.getPlayer().getUniqueId()); }

    private void open(Player p) {
        if (!p.isOnline() || !pending.remove(p.getUniqueId())) return;
        // don't cover another menu (login screen, staff tools ...)
        if (p.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) return;
        String cmd = plugin.getConfig().getString("join-menu.command", "guide");
        p.performCommand(cmd.startsWith("/") ? cmd.substring(1) : cmd);
    }
}
