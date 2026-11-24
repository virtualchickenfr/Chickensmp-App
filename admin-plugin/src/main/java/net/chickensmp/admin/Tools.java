package net.chickensmp.admin;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.*;

/** Shard tools: 3x3 pickaxe, 3x3 shovel, tree axe, infinite rocket, sponge bucket. */
public final class Tools implements Listener {

    public static final List<String> IDS = List.of("pickaxe", "shovel", "axe", "rocket", "bucket");

    private final AdminPlugin plugin;
    private final NamespacedKey key;
    private final Set<UUID> busy = new HashSet<>();
    private final Map<UUID, Long> bucketCooldown = new HashMap<>();

    public Tools(AdminPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "tool");
    }

    private ConfigurationSection cfg(String id) {
        return plugin.getConfig().getConfigurationSection("tools." + id);
    }

    public String name(String id) {
        ConfigurationSection c = cfg(id);
        return c == null ? id : c.getString("name", id);
    }

    public ItemStack create(String id) {
        ConfigurationSection c = cfg(id);
        if (c == null || !IDS.contains(id)) return null;
        Material m = Material.matchMaterial(c.getString("material", "STONE"));
        if (m == null) return null;
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Text.c(c.getString("name", id)));
        meta.lore(Text.c(c.getStringList("lore")));
        for (String e : c.getStringList("enchantments")) {
            String[] p = e.split(":");
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(p[0].toLowerCase(Locale.ROOT)));
            if (ench != null) meta.addEnchant(ench, p.length > 1 ? Integer.parseInt(p[1]) : 1, true);
        }
        if (c.getBoolean("unbreakable", true)) meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        if (c.getBoolean("glow", true)) meta.setEnchantmentGlintOverride(true);
        if (meta instanceof FireworkMeta fm) fm.setPower(Math.max(1, Math.min(3, c.getInt("flight-duration", 3))));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        it.setItemMeta(meta);
        return it;
    }

    public String id(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private boolean blockedWorld(Player p) {
        return plugin.getConfig().getStringList("tools.disabled-worlds").stream().anyMatch(w -> w.equalsIgnoreCase(p.getWorld().getName()));
    }

    // ---------------------------------------------------------------- 3x3 pickaxe / shovel, tree axe

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (busy.contains(p.getUniqueId())) return;
        ItemStack hand = p.getInventory().getItemInMainHand();
        String id = id(hand);
        if (id == null || p.isSneaking() || blockedWorld(p)) return;
        Block center = e.getBlock();
        if (id.equals("pickaxe") || id.equals("shovel")) {
            if (!center.isPreferredTool(hand)) return;
            BlockFace face = face(p);
            List<Block> around = new ArrayList<>();
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                Block t = switch (face) {
                    case UP, DOWN -> center.getRelative(a, 0, b);
                    case NORTH, SOUTH -> center.getRelative(a, b, 0);
                    default -> center.getRelative(0, a, b);
                };
                if (mineable(t, hand)) around.add(t);
            }
            busy.add(p.getUniqueId());
            try {
                for (Block t : around) p.breakBlock(t);
            } finally {
                busy.remove(p.getUniqueId());
            }
        } else if (id.equals("axe") && Tag.LOGS.isTagged(center.getType())) {
            List<Block> tree = tree(center);
            if (tree.isEmpty()) return;
            int perTick = Math.max(1, plugin.getConfig().getInt("tools.axe.blocks-per-tick", 4));
            UUID uid = p.getUniqueId();
            new org.bukkit.scheduler.BukkitRunnable() {
                int i = 0;
                @Override public void run() {
                    Player q = Bukkit.getPlayer(uid);
                    if (q == null || !"axe".equals(id(q.getInventory().getItemInMainHand()))) { cancel(); return; }
                    busy.add(uid);
                    try {
                        for (int n = 0; n < perTick && i < tree.size(); n++, i++) {
                            Block b = tree.get(i);
                            if (Tag.LOGS.isTagged(b.getType())) q.breakBlock(b);
                        }
                    } finally {
                        busy.remove(uid);
                    }
                    if (i >= tree.size()) cancel();
                }
            }.runTaskTimer(plugin, 1L, 1L);
        }
    }

    private boolean mineable(Block b, ItemStack hand) {
        Material m = b.getType();
        if (m.isAir() || b.isLiquid() || m == Material.SPAWNER || m == Material.TRIAL_SPAWNER || m == Material.VAULT) return false;
        float h = m.getHardness();
        if (h < 0 || h >= plugin.getConfig().getDouble("tools.max-hardness", 30)) return false;
        return b.isPreferredTool(hand);
    }

    private BlockFace face(Player p) {
        RayTraceResult r = p.rayTraceBlocks(6, FluidCollisionMode.NEVER);
        if (r != null && r.getHitBlockFace() != null) return r.getHitBlockFace();
        float pitch = p.getLocation().getPitch();
        if (pitch > 45 || pitch < -45) return BlockFace.UP;
        return p.getFacing();
    }

    /** All logs connected to the start (diagonals too), only if natural leaves touch them (so houses are safe). */
    private List<Block> tree(Block start) {
        int max = plugin.getConfig().getInt("tools.axe.max-logs", 256);
        List<Block> out = new ArrayList<>();
        Set<Block> seen = new HashSet<>();
        Deque<Block> q = new ArrayDeque<>();
        q.add(start);
        seen.add(start);
        boolean leaves = false;
        while (!q.isEmpty() && out.size() < max) {
            Block b = q.poll();
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                Block n = b.getRelative(x, y, z);
                if (!leaves && Tag.LEAVES.isTagged(n.getType()) && n.getBlockData() instanceof Leaves l && !l.isPersistent()) leaves = true;
                if (n.getY() < start.getY() || !seen.add(n)) continue;
                if (Tag.LOGS.isTagged(n.getType())) { q.add(n); out.add(n); }
            }
        }
        boolean needLeaves = plugin.getConfig().getBoolean("tools.axe.require-leaves", true);
        if (needLeaves && !leaves) return List.of();
        out.sort(Comparator.comparingInt(Block::getY));
        return out;
    }

    // ---------------------------------------------------------------- infinite rocket

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBoost(PlayerElytraBoostEvent e) {
        if ("rocket".equals(id(e.getItemStack()))) e.setShouldConsume(false);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCrossbow(EntityLoadCrossbowEvent e) {
        if (e.getEntity() instanceof Player p && "rocket".equals(id(p.getInventory().getItemInOffHand()))) e.setCancelled(true);
    }

    // ---------------------------------------------------------------- interact: rocket on ground, sponge bucket

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack it = e.getItem();
        String id = id(it);
        if (id == null) return;
        Player p = e.getPlayer();
        if (id.equals("rocket")) {
            // only for flying: no free firework spam on the ground
            if (!p.isGliding()) e.setCancelled(true);
            return;
        }
        if (!id.equals("bucket")) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND && "bucket".equals(id(p.getInventory().getItemInMainHand()))) return;
        long now = System.currentTimeMillis();
        Long last = bucketCooldown.get(p.getUniqueId());
        if (last != null && now - last < 400) return;
        bucketCooldown.put(p.getUniqueId(), now);
        if (blockedWorld(p)) return;
        RayTraceResult r = p.rayTraceBlocks(6, FluidCollisionMode.ALWAYS);
        if (r == null || r.getHitBlock() == null) return;
        Block hit = r.getHitBlock();
        if (!wet(hit)) {
            if (r.getHitBlockFace() != null && wet(hit.getRelative(r.getHitBlockFace()))) hit = hit.getRelative(r.getHitBlockFace());
            else return;
        }
        int removed = absorb(p, hit);
        if (removed > 0) {
            p.getWorld().playSound(hit.getLocation(), Sound.BLOCK_SPONGE_ABSORB, 1f, 1f);
            p.getWorld().spawnParticle(Particle.CLOUD, hit.getLocation().add(0.5, 0.5, 0.5), 20, 1.5, 1, 1.5, 0.02);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFill(PlayerBucketFillEvent e) {
        if ("bucket".equals(id(e.getPlayer().getInventory().getItem(e.getHand())))) e.setCancelled(true);
    }

    private boolean wet(Block b) {
        Material m = b.getType();
        boolean lava = plugin.getConfig().getBoolean("tools.bucket.absorb-lava", true);
        if (m == Material.WATER || (lava && m == Material.LAVA) || m == Material.KELP || m == Material.KELP_PLANT
                || m == Material.SEAGRASS || m == Material.TALL_SEAGRASS || m == Material.BUBBLE_COLUMN) return true;
        return b.getBlockData() instanceof Waterlogged w && w.isWaterlogged();
    }

    /** Like a sponge: removes connected water (and lava) within the radius, up to max-blocks. */
    private int absorb(Player p, Block start) {
        int radius = plugin.getConfig().getInt("tools.bucket.radius", 6);
        int max = plugin.getConfig().getInt("tools.bucket.max-blocks", 128);
        Deque<Block> q = new ArrayDeque<>();
        Set<Block> seen = new HashSet<>();
        q.add(start);
        seen.add(start);
        int removed = 0;
        while (!q.isEmpty() && removed < max) {
            Block b = q.poll();
            if (!wet(b)) continue;
            if (!allowed(p, b)) continue;
            BlockData d = b.getBlockData();
            if (d instanceof Waterlogged w && w.isWaterlogged() && !(b.getType() == Material.WATER)) {
                w.setWaterlogged(false);
                b.setBlockData(w, true);
            } else {
                b.setType(Material.AIR, true);
            }
            removed++;
            for (BlockFace f : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block n = b.getRelative(f);
                int dist = Math.abs(n.getX() - start.getX()) + Math.abs(n.getY() - start.getY()) + Math.abs(n.getZ() - start.getZ());
                if (dist <= radius && seen.add(n)) q.add(n);
            }
        }
        return removed;
    }

    /** Asks the other plugins (WorldGuard, arena protection …) whether this player may change the block. */
    private boolean allowed(Player p, Block b) {
        BlockBreakEvent test = new BlockBreakEvent(b, p);
        test.setDropItems(false);
        test.setExpToDrop(0);
        busy.add(p.getUniqueId());
        try {
            Bukkit.getPluginManager().callEvent(test);
        } finally {
            busy.remove(p.getUniqueId());
        }
        return !test.isCancelled();
    }
}
