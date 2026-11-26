package net.chickensmp.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.persistence.PersistentDataType;

/**
 * Cracked players wait in an empty void world ("nothing") until they /login or /register, then go back
 * to where they were. The return location is also kept in the player's PDC so a crash can't strand them.
 */
final class Limbo implements Listener {
    static final String WORLD = "chickenauth_limbo";
    private final AuthPlugin plugin;
    private final NamespacedKey key;
    private final Map<UUID, Location> back = new HashMap<>();
    private World world;

    Limbo(AuthPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "return");
    }

    static final class Void extends ChunkGenerator {
        @Override public boolean shouldGenerateNoise() { return false; }
        @Override public boolean shouldGenerateSurface() { return false; }
        @Override public boolean shouldGenerateCaves() { return false; }
        @Override public boolean shouldGenerateDecorations() { return false; }
        @Override public boolean shouldGenerateMobs() { return false; }
        @Override public boolean shouldGenerateStructures() { return false; }
        @Override public Location getFixedSpawnLocation(World w, Random r) { return new Location(w, 0.5, 100, 0.5); }
    }

    boolean enabled() { return plugin.getConfig().getBoolean("limbo", true); }

    World world() {
        if (world != null) return world;
        world = Bukkit.getWorld(WORLD);
        if (world == null) {
            world = new WorldCreator(WORLD).generator(new Void()).type(WorldType.FLAT).generateStructures(false).createWorld();
        }
        if (world != null) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setTime(18000);
            world.setStorm(false);
            world.setAutoSave(false);
        }
        return world;
    }

    boolean inLimbo(Player p) { return back.containsKey(p.getUniqueId()); }

    /** Spot for this player (spread out so players don't see each other). */
    private Location spot(Player p) {
        int slot = Math.floorMod(p.getUniqueId().hashCode(), 512);
        Location l = new Location(world(), slot * 64 + 0.5, 100, 0.5, 0, 0);
        l.clone().add(0, -1, 0).getBlock().setType(Material.BARRIER, false);
        return l;
    }

    /** Called when a cracked player has to log in. */
    void enter(Player p) {
        if (!enabled() || world() == null || inLimbo(p)) return;
        Location from = restoreTarget(p);
        if (from == null) from = p.getLocation();
        back.put(p.getUniqueId(), from.clone());
        p.getPersistentDataContainer().set(key, PersistentDataType.STRING, encode(from));
        p.teleport(spot(p));
        p.setFallDistance(0);
    }

    /** Back to the world after a successful login. */
    void leave(Player p) {
        Location to = back.remove(p.getUniqueId());
        if (to == null) to = restoreTarget(p);
        p.getPersistentDataContainer().remove(key);
        if (to != null && to.getWorld() != null) {
            p.teleport(to);
            p.setFallDistance(0);
        }
    }

    /** A return point left over from a crash (PDC), or null. */
    private Location restoreTarget(Player p) {
        String s = p.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return s == null ? null : decode(s);
    }

    /** Players still carrying a return point (crash) but not in limbo: put them back. */
    void fixLeftover(Player p) {
        if (inLimbo(p)) return;
        Location to = restoreTarget(p);
        if (to == null && !p.getWorld().getName().equals(WORLD)) return;
        p.getPersistentDataContainer().remove(key);
        if (to == null) to = Bukkit.getWorlds().get(0).getSpawnLocation();
        if (to.getWorld() != null) p.teleport(to);
    }

    /** While in limbo, other plugins' teleports (first-join spawn etc.) only change the return point. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        if (!inLimbo(p)) return;
        Location to = e.getTo();
        if (to == null || to.getWorld() == null || to.getWorld().getName().equals(WORLD)) return;
        if (ours) return;
        back.put(p.getUniqueId(), to.clone());
        p.getPersistentDataContainer().set(key, PersistentDataType.STRING, encode(to));
        e.setCancelled(true);
    }

    private boolean ours;

    void leaveNow(Player p) {
        ours = true;
        try { leave(p); } finally { ours = false; }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (!inLimbo(p)) return;
        // leave the player where they came from, so the saved position is never the void
        leaveNow(p);
    }

    private static String encode(Location l) {
        return l.getWorld().getName() + ";" + l.getX() + ";" + l.getY() + ";" + l.getZ() + ";" + l.getYaw() + ";" + l.getPitch();
    }

    private static Location decode(String s) {
        try {
            String[] a = s.split(";");
            World w = Bukkit.getWorld(a[0]);
            if (w == null) return null;
            return new Location(w, Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]), Float.parseFloat(a[4]), Float.parseFloat(a[5]));
        } catch (Exception ex) {
            return null;
        }
    }
}
