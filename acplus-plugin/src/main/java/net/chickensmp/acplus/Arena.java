package net.chickensmp.acplus;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.time.Duration;
import java.util.*;

/**
 * PvP area: the configured world outside the safe region. Player-placed blocks are removed
 * after a while (the old block comes back); the map itself can't be broken.
 */
final class Arena implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private record Placed(BlockState before, long expires) {}

    private final SusPlugin plugin;
    /** key = world:x:y:z */
    private final Map<String, Placed> placed = new LinkedHashMap<>();
    private final Map<UUID, Boolean> lastInArena = new HashMap<>();
    private boolean enabled;
    private String worldName, region;
    private int[] min, max;
    private long lifetimeMs;
    private boolean wg;

    Arena(SusPlugin plugin) {
        this.plugin = plugin;
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::sweep, 40L, 40L);
    }

    void load() {
        ConfigurationSection c = plugin.getConfig().getConfigurationSection("arena");
        if (c == null) c = plugin.getConfig().createSection("arena");
        enabled = c.getBoolean("enabled", true);
        worldName = c.getString("world", "Spawn1");
        region = c.getString("safe-region", "spawn");
        min = toArr(c.getIntegerList("safe-min"), new int[]{-213, -64, 385});
        max = toArr(c.getIntegerList("safe-max"), new int[]{-134, 319, 464});
        lifetimeMs = Math.max(1, c.getLong("block-lifetime-minutes", 20)) * 60_000L;
        if (c.getLong("block-lifetime-seconds", 0) > 0) lifetimeMs = c.getLong("block-lifetime-seconds") * 1000L; // for tests
        wg = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    private static int[] toArr(List<Integer> l, int[] def) {
        return l != null && l.size() == 3 ? new int[]{l.get(0), l.get(1), l.get(2)} : def;
    }

    private String cfg(String k, String d) {
        return plugin.getConfig().getString("arena." + k, d);
    }

    /** True when this block is in the PvP area (right world, outside the spawn region). */
    boolean inArena(Block b) {
        return inArena(b.getWorld(), b.getX(), b.getY(), b.getZ());
    }

    boolean inArena(World w, int x, int y, int z) {
        if (!enabled || w == null || !w.getName().equalsIgnoreCase(worldName)) return false;
        if (wg) {
            try {
                Boolean in = WgHook.inRegion(w, region, x, y, z);
                if (in != null) return !in;
            } catch (Throwable t) {
                wg = false;
                plugin.getLogger().warning("WorldGuard lookup failed, using safe-min/safe-max: " + t);
            }
        }
        boolean inSafe = x >= Math.min(min[0], max[0]) && x <= Math.max(min[0], max[0])
                && y >= Math.min(min[1], max[1]) && y <= Math.max(min[1], max[1])
                && z >= Math.min(min[2], max[2]) && z <= Math.max(min[2], max[2]);
        return !inSafe;
    }

    private static String key(Block b) {
        return b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    private void track(BlockState before) {
        String k = key(before.getBlock());
        // Keep the oldest "before" state if the spot is built over again, but restart the timer.
        Placed old = placed.remove(k);
        placed.put(k, new Placed(old != null ? old.before() : before, System.currentTimeMillis() + lifetimeMs));
    }

    private boolean isPlaced(Block b) {
        return placed.containsKey(key(b));
    }

    private void sweep() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Placed>> it = placed.entrySet().iterator();
        int n = 0;
        while (it.hasNext()) {
            Map.Entry<String, Placed> e = it.next();
            if (e.getValue().expires() > now) continue;
            it.remove();
            revert(e.getValue().before());
            if (++n >= 400) break; // spread big cleanups over several ticks
        }
    }

    private void revert(BlockState before) {
        Block b = before.getBlock();
        if (!b.getChunk().isLoaded()) b.getChunk().load();
        before.update(true, false);
        if (plugin.getConfig().getBoolean("arena.vanish-effect", true))
            b.getWorld().spawnParticle(Particle.POOF, b.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.01);
    }

    /** On shutdown every placed block goes away right now. */
    void shutdown() {
        for (Placed p : placed.values()) revert(p.before());
        placed.clear();
    }

    int count() {
        return placed.size();
    }

    // ------------------------------------------------------------ building

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!inArena(e.getBlockPlaced())) return;
        if (e instanceof BlockMultiPlaceEvent m) {
            for (BlockState s : m.getReplacedBlockStates()) track(s);
        } else {
            track(e.getBlockReplacedState());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent e) {
        Block b = e.getBlock();
        if (inArena(b)) track(b.getState());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        Block b = e.getBlock();
        if (!inArena(b) || isPlaced(b) || e.getPlayer().hasPermission("chickensmp.acplus.arena.bypass")) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!inArena(b)) return;
        if (isPlaced(b)) {
            // Broken by a player: the spot goes back to what it was, no timer needed any more.
            Placed p = placed.remove(key(b));
            e.setDropItems(true);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (b.getType().isAir()) p.before().update(true, false);
            });
            return;
        }
        if (e.getPlayer().hasPermission("chickensmp.acplus.arena.bypass")) return;
        e.setCancelled(true);
        e.getPlayer().sendActionBar(MM.deserialize(cfg("cant-break", "<red>You can only break blocks that players placed here.")));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> inArena(b) && !isPlaced(b));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> inArena(b) && !isPlaced(b));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (inArena(e.getBlock()) && !isPlaced(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (e.getCause() == BlockIgniteEvent.IgniteCause.SPREAD && inArena(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        // Water/lava from placed buckets: track what it flows into so it goes away too.
        Block from = e.getBlock(), to = e.getToBlock();
        if (inArena(to) && isPlaced(from) && !isPlaced(to)) track(to.getState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onForm(BlockFormEvent e) {
        // Cobblestone/obsidian made by placed lava/water also disappears.
        Block b = e.getBlock();
        if (inArena(b) && !isPlaced(b)) {
            for (Block n : new Block[]{b.getRelative(1, 0, 0), b.getRelative(-1, 0, 0), b.getRelative(0, 0, 1),
                    b.getRelative(0, 0, -1), b.getRelative(0, 1, 0)}) {
                if (isPlaced(n)) { track(b.getState()); break; }
            }
        }
    }

    // ------------------------------------------------------------ titles when crossing the border

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location t = e.getTo();
        if (e.getFrom().getBlockX() == t.getBlockX() && e.getFrom().getBlockZ() == t.getBlockZ()
                && e.getFrom().getBlockY() == t.getBlockY() && e.getFrom().getWorld() == t.getWorld()) return;
        check(e.getPlayer(), t);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        check(e.getPlayer(), e.getTo());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastInArena.remove(e.getPlayer().getUniqueId());
    }

    private void check(Player p, Location to) {
        boolean now = inArena(to.getWorld(), to.getBlockX(), to.getBlockY(), to.getBlockZ());
        Boolean before = lastInArena.put(p.getUniqueId(), now);
        if (before == null || before == now) return;
        String min = String.valueOf(lifetimeMs / 60_000L);
        String title = now ? cfg("enter-title", "<red><bold>PVP AREA") : cfg("leave-title", "<green><bold>SAFE ZONE");
        String sub = (now ? cfg("enter-subtitle", "") : cfg("leave-subtitle", "")).replace("{minutes}", min);
        p.showTitle(Title.title(MM.deserialize(title), MM.deserialize(sub),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1600), Duration.ofMillis(400))));
        p.playSound(p.getLocation(), now ? Sound.ITEM_ARMOR_EQUIP_IRON : Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, now ? 0.8f : 1.4f);
    }
}
