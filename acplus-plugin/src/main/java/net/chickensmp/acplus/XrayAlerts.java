package net.chickensmp.acplus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

/**
 * X-ray helper for GrimAC (Grim doesn't detect X-ray): counts how many separate ore veins a player
 * finds in a short time. Too many -> staff alert + an "XRay" flag in /sus.
 */
final class XrayAlerts implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private record Vein(Material type, Location at, long time) {}

    private final SusPlugin plugin;
    private final Map<UUID, Deque<Vein>> veins = new HashMap<>();
    private final Map<UUID, Long> lastAlert = new HashMap<>();
    private final Map<Material, Double> ores = new EnumMap<>(Material.class);
    private boolean enabled;
    private long windowMs, cooldownMs;
    private double threshold;
    private int maxY;

    XrayAlerts(SusPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    void load() {
        ConfigurationSection c = plugin.getConfig().getConfigurationSection("xray-alerts");
        if (c == null) c = plugin.getConfig().createSection("xray-alerts");
        enabled = c.getBoolean("enabled", true);
        windowMs = Math.max(1, c.getLong("window-minutes", 10)) * 60_000L;
        cooldownMs = Math.max(0, c.getLong("alert-cooldown-minutes", 5)) * 60_000L;
        threshold = c.getDouble("alert-points", 8);
        maxY = c.getInt("max-y", 32);
        ores.clear();
        ConfigurationSection o = c.getConfigurationSection("ores");
        if (o != null) {
            for (String k : o.getKeys(false)) {
                Material m = Material.matchMaterial(k);
                if (m != null) ores.put(m, o.getDouble(k));
            }
        } else {
            ores.put(Material.DIAMOND_ORE, 1.0);
            ores.put(Material.DEEPSLATE_DIAMOND_ORE, 1.0);
            ores.put(Material.ANCIENT_DEBRIS, 2.0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!enabled) return;
        Material type = e.getBlock().getType();
        Double pts = ores.get(type);
        if (pts == null) return;
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (e.getBlock().getY() > maxY && type != Material.ANCIENT_DEBRIS) return;

        long now = System.currentTimeMillis();
        Location at = e.getBlock().getLocation();
        Deque<Vein> list = veins.computeIfAbsent(p.getUniqueId(), u -> new ArrayDeque<>());
        while (!list.isEmpty() && now - list.peekFirst().time() > windowMs) list.removeFirst();

        // Same vein = same ore type within 3 blocks of an ore they mined in the last minute.
        for (Vein v : list) {
            if (v.type() == type && now - v.time() < 60_000L && v.at().getWorld() == at.getWorld()
                    && v.at().distanceSquared(at) <= 9) {
                list.addLast(new Vein(type, at, v.time())); // keep the vein's start time, don't count again
                return;
            }
        }
        list.addLast(new Vein(type, at, now));

        // Count distinct veins (entries whose time is their own start) and their points.
        double score = 0;
        Map<Material, Integer> perType = new EnumMap<>(Material.class);
        Set<Long> seen = new HashSet<>();
        for (Vein v : list) {
            if (!seen.add(v.time() * 31 + v.type().ordinal())) continue;
            score += ores.getOrDefault(v.type(), 1.0);
            perType.merge(v.type(), 1, Integer::sum);
        }
        if (score < threshold) return;
        Long last = lastAlert.get(p.getUniqueId());
        if (last != null && now - last < cooldownMs) return;
        lastAlert.put(p.getUniqueId(), now);

        StringBuilder found = new StringBuilder();
        perType.forEach((m, n) -> {
            if (found.length() > 0) found.append(", ");
            found.append(n).append("x ").append(m.name().toLowerCase(Locale.ROOT).replace("deepslate_", "").replace('_', ' '));
        });
        String name = MM.escapeTags(p.getName());
        long min = windowMs / 60_000L;
        Component msg = MM.deserialize(plugin.getConfig().getString("messages.prefix", "")
                + "<gold><bold>X-RAY?</bold></gold> <white>" + name + "</white> <gray>found <white>" + MM.escapeTags(found.toString())
                + "</white> veins in " + min + "m at Y " + at.getBlockY() + "</gray> "
                + "<click:run_command:'/sus tp " + name + "'><green>[TP]</green></click> "
                + "<click:run_command:'/sus watch " + name + "'><aqua>[Watch]</aqua></click>");
        for (Player s : Bukkit.getOnlinePlayers()) if (s.hasPermission("chickensmp.acplus.alerts")) s.sendMessage(msg);
        Bukkit.getConsoleSender().sendMessage(msg);
        plugin.recordFlag(p.getUniqueId(), p.getName(), "XRay", 'A', "xray", false, (int) score, found + " in " + min + "m");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // keep data for the window, but don't let the maps grow forever
        if (veins.size() > 500) { veins.clear(); lastAlert.clear(); }
    }
}
