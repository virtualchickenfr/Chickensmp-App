package net.chickensmp.acplus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import org.bukkit.scheduler.BukkitTask;

/**
 * Opens a menu (default /guide = /help) on a player's first join: once the resource pack has loaded
 * (or after a fallback delay for players without the pack, e.g. Bedrock) and once the player has left
 * the ChickenSMPAuth login limbo (cracked players log in first).
 */
final class JoinMenu implements Listener {
    private static final String LIMBO = "chickenauth_limbo";
    private final SusPlugin plugin;
    private final Set<UUID> packLoaded = new HashSet<>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    JoinMenu(SusPlugin plugin) { this.plugin = plugin; }

    private boolean enabled() { return plugin.getConfig().getBoolean("join-menu.enabled", true); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        if (!enabled()) return;
        Player p = e.getPlayer();
        if (plugin.getConfig().getBoolean("join-menu.first-join-only", true) && p.hasPlayedBefore()) return;
        UUID u = p.getUniqueId();
        int fallback = plugin.getConfig().getInt("join-menu.fallback-delay-ticks", 100);
        long started = System.currentTimeMillis();
        stop(u);
        tasks.put(u, Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long waited = (System.currentTimeMillis() - started) / 50;
            if (!p.isOnline() || waited > 20L * 60 * 5) { stop(u); return; }        // give up after 5 min
            if (p.getWorld().getName().equals(LIMBO)) return;                       // still logging in
            if (!packLoaded.contains(u) && waited < fallback) return;               // pack still loading
            if (p.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) return; // other menu open
            stop(u);
            String cmd = plugin.getConfig().getString("join-menu.command", "guide");
            p.performCommand(cmd.startsWith("/") ? cmd.substring(1) : cmd);
        }, 20L, 10L));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPack(PlayerResourcePackStatusEvent e) {
        if (e.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) packLoaded.add(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        stop(e.getPlayer().getUniqueId());
        packLoaded.remove(e.getPlayer().getUniqueId());
    }

    private void stop(UUID u) {
        BukkitTask t = tasks.remove(u);
        if (t != null) t.cancel();
    }
}
