package net.chickensmp.acplus;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Per-player view distance so the server holds up with many players: everyone gets
 * view-distance.default chunks; players with chickensmp.acplus.viewdistance.<n> get n
 * (highest listed value wins, so ranks/staff can see further). Re-applied every minute so rank changes count.
 */
final class ViewDistance implements Listener {
    private final SusPlugin plugin;

    ViewDistance(SusPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> Bukkit.getOnlinePlayers().forEach(this::apply), 1200L, 1200L);
    }

    private boolean enabled() { return plugin.getConfig().getBoolean("view-distance.enabled", true); }

    int distanceFor(Player p) {
        int d = plugin.getConfig().getInt("view-distance.default", 6);
        List<Integer> levels = new ArrayList<>(plugin.getConfig().getIntegerList("view-distance.levels"));
        if (levels.isEmpty()) levels = List.of(8, 10);
        for (int n : levels) if (n > d && p.hasPermission("chickensmp.acplus.viewdistance." + n)) d = n;
        return Math.max(2, Math.min(32, d));
    }

    void apply(Player p) {
        if (!enabled() || !p.isOnline()) return;
        int d = distanceFor(p);
        try {
            if (p.getViewDistance() != d) p.setViewDistance(d);
            if (p.getSendViewDistance() != d) p.setSendViewDistance(d);
        } catch (Throwable ignored) {
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> apply(p), 20L);
    }
}
