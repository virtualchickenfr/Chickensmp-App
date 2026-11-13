package net.chickensmp.acplus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** /staff: spectator (fly through blocks) + invisible to everyone without chickensmp.acplus.see. */
final class StaffMode implements Listener, CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String SEE = "chickensmp.acplus.see";

    private record Saved(GameMode mode, Location location, boolean allowFlight, boolean flying) {}

    private final SusPlugin plugin;
    private final Map<UUID, Saved> active = new HashMap<>();

    StaffMode(SusPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            String bar = plugin.getConfig().getString("staff-mode.actionbar", "<aqua><bold>STAFF MODE");
            for (UUID id : active.keySet()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.sendActionBar(MM.deserialize(bar));
            }
        }, 40L, 40L);
    }

    boolean isActive(Player p) {
        return active.containsKey(p.getUniqueId());
    }

    private String msg(String key, String def) {
        return plugin.getConfig().getString("staff-mode." + key, def);
    }

    private void toNonStaff(String template, Player p) {
        if (!plugin.getConfig().getBoolean("staff-mode.fake-messages", true)) return;
        Component c = MM.deserialize(template.replace("{player}", MM.escapeTags(p.getName())));
        for (Player o : Bukkit.getOnlinePlayers()) if (!o.hasPermission(SEE) && !o.equals(p)) o.sendMessage(c);
    }

    void enable(Player p) {
        active.put(p.getUniqueId(), new Saved(p.getGameMode(), p.getLocation().clone(), p.getAllowFlight(), p.isFlying()));
        p.setMetadata("vanished", new FixedMetadataValue(plugin, true));
        for (Player o : Bukkit.getOnlinePlayers()) if (!o.equals(p) && !o.hasPermission(SEE)) o.hidePlayer(plugin, p);
        p.setGameMode(GameMode.SPECTATOR);
        p.sendMessage(MM.deserialize(msg("enabled-message", "<green>Staff mode on.")));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.6f);
        toNonStaff(msg("fake-quit", "<yellow>{player} left the game"), p);
    }

    void disable(Player p, boolean quitting) {
        Saved s = active.remove(p.getUniqueId());
        if (s == null) return;
        // Back to where they started, so they never end up inside a wall.
        p.teleport(s.location());
        p.setGameMode(s.mode());
        p.setAllowFlight(s.allowFlight());
        if (s.allowFlight()) p.setFlying(s.flying());
        p.removeMetadata("vanished", plugin);
        for (Player o : Bukkit.getOnlinePlayers()) if (!o.equals(p)) o.showPlayer(plugin, p);
        if (!quitting) {
            p.sendMessage(MM.deserialize(msg("disabled-message", "<red>Staff mode off.")));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.6f);
            toNonStaff(msg("fake-join", "<yellow>{player} joined the game"), p);
        }
    }

    void shutdown() {
        for (UUID id : active.keySet().toArray(new UUID[0])) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) disable(p, true); else active.remove(id);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent e) {
        Player j = e.getPlayer();
        if (j.hasPermission(SEE)) return;
        for (UUID id : active.keySet()) {
            Player s = Bukkit.getPlayer(id);
            if (s != null) j.hidePlayer(plugin, s);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent e) {
        if (!isActive(e.getPlayer())) return;
        e.quitMessage(null); // they already "left" when staff mode started
        disable(e.getPlayer(), true);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Only players can use staff mode."); return true; }
        if (isActive(p)) disable(p, false); else enable(p);
        return true;
    }
}
