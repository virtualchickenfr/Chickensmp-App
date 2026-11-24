package net.chickensmp.admin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Virtual stacked spawners (SmartSpawner / DonutSMP style): the block spawns no mobs, it fills a loot
 * storage and an XP bank while a player is nearby. Right-click opens the storage GUI, right-click with a
 * spawner of the same type stacks it, a silk-touch pickaxe breaks it (sneak = up to 64 at once).
 */
public final class Spawners implements Listener {

    public static final class Drop {
        final Material material; final int min, max; final double chance;
        Drop(Material m, int min, int max, double chance) { this.material = m; this.min = min; this.max = max; this.chance = chance; }
    }

    public static final class Mob {
        final String id, name, color; final EntityType entity; final Material icon;
        final List<Drop> drops; final int xpMin, xpMax;
        Mob(String id, String name, String color, EntityType entity, Material icon, List<Drop> drops, int xpMin, int xpMax) {
            this.id = id; this.name = name; this.color = color; this.entity = entity; this.icon = icon;
            this.drops = drops; this.xpMin = xpMin; this.xpMax = xpMax;
        }
        public String id() { return id; }
        public String name() { return name; }
        public String color() { return color; }
        public Material icon() { return icon; }
    }

    public static final class Data {
        final String world; final int x, y, z;
        String mob; int stack; long xp; long next;
        final LinkedHashMap<Material, Long> items = new LinkedHashMap<>();
        Data(String world, int x, int y, int z) { this.world = world; this.x = x; this.y = y; this.z = z; }
        String key() { return world + ";" + x + ";" + y + ";" + z; }
        long stored() { long n = 0; for (long v : items.values()) n += v; return n; }
        Location loc() { World w = Bukkit.getWorld(world); return w == null ? null : new Location(w, x, y, z); }
    }

    private final AdminPlugin plugin;
    private final NamespacedKey key;
    private final Map<String, Mob> mobs = new LinkedHashMap<>();
    private final Map<String, Data> data = new HashMap<>();
    private final Map<Material, Double> worth = new HashMap<>();
    private double defaultWorth = 0;
    private boolean dirty;

    public Spawners(AdminPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "spawner");
    }

    private String prefix() { return plugin.getConfig().getString("spawners.prefix", "&#00A4FC&lSPAWNER &r&7» &f"); }
    private void msg(Player p, String s) { p.sendMessage(Text.c(prefix() + s)); }
    private ConfigurationSection cfg() { return plugin.getConfig().getConfigurationSection("spawners"); }

    public void enable() {
        reload();
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> { if (dirty) save(true); }, 20L * 120, 20L * 120);
        // spawner blocks keep their mob picture
        Bukkit.getScheduler().runTaskLater(plugin, () -> data.values().forEach(this::paint), 60L);
    }

    public void disable() { save(false); }

    public int count() { return data.size(); }
    public Set<String> mobIds() { return mobs.keySet(); }
    public Collection<Mob> mobs() { return mobs.values(); }
    public Mob mob(String id) { return mobs.get(id); }

    // ---------------------------------------------------------------- config

    public void reload() {
        mobs.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "mobs.yml"));
        ConfigurationSection root = y.getConfigurationSection("mobs");
        if (root != null) for (String id : root.getKeys(false)) {
            ConfigurationSection c = root.getConfigurationSection(id);
            if (c == null) continue;
            EntityType et;
            try { et = EntityType.valueOf(c.getString("entity", id).toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { plugin.getLogger().warning("mobs.yml: unknown entity " + id); continue; }
            Material icon = Material.matchMaterial(c.getString("icon", et.name() + "_SPAWN_EGG"));
            if (icon == null) icon = Material.SPAWNER;
            List<Drop> drops = new ArrayList<>();
            for (Map<?, ?> d : c.getMapList("drops")) {
                Material m = Material.matchMaterial(String.valueOf(d.get("item")));
                if (m == null) { plugin.getLogger().warning("mobs.yml: unknown item in " + id + ": " + d.get("item")); continue; }
                int min = num(d.get("min"), 1), max = Math.max(min, num(d.get("max"), min));
                double chance = d.get("chance") instanceof Number n ? n.doubleValue() : 100;
                drops.add(new Drop(m, min, max, chance));
            }
            String[] xp = c.getString("xp", "1-3").split("-");
            int xmin = Integer.parseInt(xp[0].trim()), xmax = xp.length > 1 ? Integer.parseInt(xp[1].trim()) : xmin;
            mobs.put(id.toLowerCase(Locale.ROOT), new Mob(id.toLowerCase(Locale.ROOT), c.getString("name", id), c.getString("color", Text.BLUE), et, icon, drops, xmin, xmax));
        }
        loadWorth();
    }

    private static int num(Object o, int def) { return o instanceof Number n ? n.intValue() : def; }

    /** Sell prices: EconomyCore worth.yml (same prices as /sell), otherwise spawners.sell-prices. */
    private void loadWorth() {
        worth.clear();
        defaultWorth = cfg().getDouble("sell-default-price", 0);
        File ec = new File(plugin.getDataFolder().getParentFile(), "EconomyCore/economy/worth/worth.yml");
        if (cfg().getBoolean("use-economycore-worth", true) && ec.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(ec);
            double mul = y.getDouble("MULTIPLIER", 1.0);
            if (mul <= 0) mul = 1;
            for (String k : y.getKeys(true)) {
                if (!y.isDouble(k) && !y.isInt(k)) continue;
                String last = k.substring(k.lastIndexOf('.') + 1);
                Material m = Material.matchMaterial(last);
                if (m != null) worth.put(m, y.getDouble(k) * mul);
            }
            File ecc = new File(ec.getParentFile(), "config.yml");
            if (ecc.exists()) defaultWorth = YamlConfiguration.loadConfiguration(ecc).getDouble("default-price", defaultWorth) * mul;
        }
        ConfigurationSection own = cfg().getConfigurationSection("sell-prices");
        if (own != null) for (String k : own.getKeys(false)) {
            Material m = Material.matchMaterial(k);
            if (m != null) worth.putIfAbsent(m, own.getDouble(k));
        }
    }

    public double price(Material m) { return worth.getOrDefault(m, defaultWorth); }

    // ---------------------------------------------------------------- storage

    private File file() { return new File(plugin.getDataFolder(), "spawners.yml"); }

    private void load() {
        data.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
        ConfigurationSection root = y.getConfigurationSection("spawners");
        if (root == null) return;
        for (String k : root.getKeys(false)) {
            ConfigurationSection c = root.getConfigurationSection(k);
            String[] p = k.split(";");
            if (c == null || p.length != 4) continue;
            try {
                Data d = new Data(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                d.mob = c.getString("mob");
                d.stack = Math.max(1, c.getInt("stack", 1));
                d.xp = c.getLong("xp");
                ConfigurationSection it = c.getConfigurationSection("items");
                if (it != null) for (String m : it.getKeys(false)) {
                    Material mat = Material.matchMaterial(m);
                    if (mat != null && it.getLong(m) > 0) d.items.put(mat, it.getLong(m));
                }
                data.put(d.key(), d);
            } catch (NumberFormatException ignored) {}
        }
    }

    /** Snapshot on the main thread, write (atomic) async unless the server is stopping. */
    public void save(boolean async) {
        dirty = false;
        YamlConfiguration y = new YamlConfiguration();
        for (Data d : data.values()) {
            String b = "spawners." + d.key();
            y.set(b + ".mob", d.mob);
            y.set(b + ".stack", d.stack);
            y.set(b + ".xp", d.xp);
            for (var e : d.items.entrySet()) y.set(b + ".items." + e.getKey().name(), e.getValue());
        }
        String text = y.saveToString();
        Runnable write = () -> {
            synchronized (Spawners.class) {
                try {
                    plugin.getDataFolder().mkdirs();
                    File tmp = new File(plugin.getDataFolder(), "spawners.yml.tmp");
                    Files.writeString(tmp.toPath(), text);
                    Files.move(tmp.toPath(), file().toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save spawners.yml: " + e);
                }
            }
        };
        if (async && plugin.isEnabled()) Bukkit.getScheduler().runTaskAsynchronously(plugin, write); else write.run();
    }

    private static String key(Block b) { return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ(); }
    public Data at(Block b) { return b.getType() == Material.SPAWNER ? data.get(key(b)) : null; }

    private void paint(Data d) {
        Location l = d.loc();
        Mob m = mobs.get(d.mob);
        if (l == null || m == null || !l.isChunkLoaded()) return;
        Block b = l.getBlock();
        if (b.getState() instanceof CreatureSpawner cs) {
            try {
                cs.setSpawnedType(m.entity);
                cs.setRequiredPlayerRange(16);
                cs.update(true, false);
            } catch (Exception ignored) {}
        }
    }

    // ---------------------------------------------------------------- items

    public ItemStack item(String mobId) {
        Mob m = mobs.get(mobId);
        ItemStack it = new ItemStack(Material.SPAWNER);
        ItemMeta meta = it.getItemMeta();
        String name = m == null ? mobId : m.name;
        String color = m == null ? Text.BLUE : m.color;
        meta.displayName(Text.c(Text.title(color, name + " Spawner")));
        meta.lore(Text.c(List.of("&7Virtual Spawner", "", "&#" + color + "Information",
                "&fPlace it to collect &#FCE300" + name + " &fdrops.", "&fRight-click a placed one to stack.",
                "&fNeeds a &#FCE300Silk Touch &fpickaxe to break.")));
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, mobId);
        it.setItemMeta(meta);
        return it;
    }

    /** Our spawner item, or one from the old Spawner plugin (any "spawner:*" string key holding a mob name). */
    public String mobOf(ItemStack it) {
        if (it == null || it.getType() != Material.SPAWNER || !it.hasItemMeta()) return null;
        PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();
        String own = pdc.get(key, PersistentDataType.STRING);
        if (own != null) return mobs.containsKey(own) ? own : null;
        for (NamespacedKey k : pdc.getKeys()) {
            if (!k.getNamespace().equals("spawner")) continue;
            try {
                String v = pdc.get(k, PersistentDataType.STRING);
                if (v != null && mobs.containsKey(v.toLowerCase(Locale.ROOT))) return v.toLowerCase(Locale.ROOT);
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    public long capacity(Data d) { return (long) cfg().getInt("storage-per-spawner", 576) * d.stack; }
    public long xpCapacity(Data d) { return (long) cfg().getInt("xp-per-spawner", 1000) * d.stack; }
    public int maxStack() { return cfg().getInt("max-stack", 10000); }

    // ---------------------------------------------------------------- production

    private void tick() {
        long now = System.currentTimeMillis();
        double range = cfg().getDouble("activation-range", 48);
        int minS = cfg().getInt("delay-min-seconds", 60), maxS = Math.max(minS, cfg().getInt("delay-max-seconds", 120));
        Set<Data> changed = new HashSet<>();
        for (Data d : data.values()) {
            if (now < d.next) continue;
            World w = Bukkit.getWorld(d.world);
            if (w == null || !w.isChunkLoaded(d.x >> 4, d.z >> 4)) continue;
            boolean near = false;
            for (Player p : w.getPlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR) continue;
                Location l = p.getLocation();
                double dx = l.getX() - d.x - 0.5, dy = l.getY() - d.y - 0.5, dz = l.getZ() - d.z - 0.5;
                if (dx * dx + dy * dy + dz * dz <= range * range) { near = true; break; }
            }
            if (!near) continue;
            if (d.next == 0) { d.next = now + ThreadLocalRandom.current().nextInt(minS, maxS + 1) * 1000L; continue; }
            d.next = now + ThreadLocalRandom.current().nextInt(minS, maxS + 1) * 1000L;
            if (produce(d)) changed.add(d);
        }
        if (!changed.isEmpty()) {
            dirty = true;
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof SpawnerMenu sm && changed.contains(sm.data)) sm.render();
        }
    }

    private boolean produce(Data d) {
        Mob m = mobs.get(d.mob);
        if (m == null) return false;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int perMin = cfg().getInt("mobs-per-cycle-min", 1), perMax = Math.max(perMin, cfg().getInt("mobs-per-cycle-max", 3));
        long kills = 0;
        if (d.stack <= 64) for (int i = 0; i < d.stack; i++) kills += r.nextInt(perMin, perMax + 1);
        else kills = Math.round(d.stack * (perMin + perMax) / 2.0 + r.nextGaussian() * Math.sqrt(d.stack));
        if (kills <= 0) return false;
        long cap = capacity(d), stored = d.stored();
        boolean any = false;
        for (Drop drop : m.drops) {
            if (stored >= cap) break;
            long amount = amount(drop, kills, r);
            amount = Math.min(amount, cap - stored);
            if (amount <= 0) continue;
            d.items.merge(drop.material, amount, Long::sum);
            stored += amount;
            any = true;
        }
        long xp = kills <= 256 ? sum(kills, m.xpMin, m.xpMax, r) : Math.round(kills * (m.xpMin + m.xpMax) / 2.0);
        long newXp = Math.min(xpCapacity(d), d.xp + xp);
        if (newXp != d.xp) { d.xp = newXp; any = true; }
        return any;
    }

    private static long sum(long n, int min, int max, ThreadLocalRandom r) {
        long s = 0;
        for (long i = 0; i < n; i++) s += r.nextInt(min, max + 1);
        return s;
    }

    private static long amount(Drop d, long kills, ThreadLocalRandom r) {
        double p = d.chance / 100.0;
        if (kills <= 256) {
            long a = 0;
            for (long i = 0; i < kills; i++) if (r.nextDouble() < p) a += r.nextInt(d.min, d.max + 1);
            return a;
        }
        double mean = kills * p, sd = Math.sqrt(kills * p * (1 - p));
        long hits = Math.max(0, Math.round(mean + r.nextGaussian() * sd));
        return Math.round(hits * (d.min + d.max) / 2.0);
    }

    // ---------------------------------------------------------------- block events

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        ItemStack it = e.getItemInHand();
        String mob = mobOf(it);
        if (mob == null) return;
        Block b = e.getBlockPlaced();
        if (!plugin.getConfig().getStringList("spawners.disabled-worlds").isEmpty()
                && plugin.getConfig().getStringList("spawners.disabled-worlds").stream().anyMatch(w -> w.equalsIgnoreCase(b.getWorld().getName()))) {
            e.setCancelled(true);
            msg(e.getPlayer(), "&#FC0000Spawners can't be placed in this world.");
            return;
        }
        Data d = new Data(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        d.mob = mob;
        d.stack = 1;
        data.put(d.key(), d);
        dirty = true;
        Bukkit.getScheduler().runTask(plugin, () -> paint(d));
        plugin.log(e.getPlayer().getName() + " placed a " + mob + " spawner at " + d.key());
        msg(e.getPlayer(), "&fPlaced a &#" + mobs.get(mob).color + mobs.get(mob).name + " Spawner&f. Right-click it to open.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        Data d = at(e.getClickedBlock());
        if (d == null) return;
        Player p = e.getPlayer();
        if (e.getHand() != EquipmentSlot.HAND) {
            if (!p.isSneaking()) e.setCancelled(true);
            return;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        String handMob = mobOf(hand);
        if (handMob != null) {
            e.setCancelled(true);
            if (!handMob.equals(d.mob)) { msg(p, "&#FC0000You can only stack spawners of the same type."); return; }
            int add = p.isSneaking() ? hand.getAmount() : 1;
            add = Math.min(add, maxStack() - d.stack);
            if (add <= 0) { msg(p, "&#FC0000This spawner is full &7(max &#FCE300" + Text.num(maxStack()) + "&7)."); return; }
            hand.setAmount(hand.getAmount() - add);
            d.stack += add;
            dirty = true;
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.4f, 1.6f);
            msg(p, "&fStacked &#FCE300" + add + "x&f. Now &#FCE300×" + Text.num(d.stack) + "&f.");
            plugin.log(p.getName() + " stacked " + add + " onto " + d.mob + " spawner " + d.key() + " (now " + d.stack + ")");
            refresh(d);
            return;
        }
        if (hand.getType().name().endsWith("_SPAWN_EGG")) { e.setCancelled(true); return; }
        if (p.isSneaking() && hand.getType().isBlock() && !hand.getType().isAir()) return; // place a block against it
        e.setCancelled(true);
        new SpawnerMenu(this, d, 0).open(p);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() != Material.SPAWNER) return;
        Player p = e.getPlayer();
        Data d = at(b);
        ItemStack tool = p.getInventory().getItemInMainHand();
        boolean silk = tool.getType().name().endsWith("_PICKAXE") && tool.containsEnchantment(Enchantment.SILK_TOUCH);
        boolean needSilk = cfg().getBoolean("silk-touch-only", true) && p.getGameMode() != GameMode.CREATIVE;
        if (d == null) {
            // natural (dungeon) spawner: silk touch gives a spawner of that mob
            if (silk && cfg().getBoolean("natural-silk-drop", true) && b.getState() instanceof CreatureSpawner cs && cs.getSpawnedType() != null) {
                String mob = cs.getSpawnedType().name().toLowerCase(Locale.ROOT);
                if (mobs.containsKey(mob)) {
                    e.setExpToDrop(0);
                    e.setDropItems(false);
                    b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), item(mob));
                    plugin.log(p.getName() + " mined a natural " + mob + " spawner at " + key(b));
                }
            }
            return;
        }
        if (needSilk && !silk) {
            e.setCancelled(true);
            msg(p, "&#FC0000You need a pickaxe with &#FCE300Silk Touch&#FC0000.");
            return;
        }
        int take = p.isSneaking() ? Math.min(64, d.stack) : 1;
        if (take >= d.stack && d.stored() > 0) {
            e.setCancelled(true);
            msg(p, "&#FC0000Empty the spawner first &7(sell or take the items).");
            return;
        }
        e.setExpToDrop(0);
        e.setDropItems(false);
        if (take < d.stack) {
            e.setCancelled(true);
            d.stack -= take;
            refresh(d);
        } else {
            if (d.xp > 0) p.giveExp((int) Math.min(Integer.MAX_VALUE, d.xp), true);
            data.remove(d.key());
            closeViewers(d);
        }
        dirty = true;
        ItemStack it = item(d.mob);
        it.setAmount(take);
        plugin.give(p, it);
        msg(p, "&fPicked up &#FCE300" + take + "x &fspawner" + (data.containsKey(d.key()) ? ". &#FCE300×" + Text.num(d.stack) + " &fleft." : "."));
        plugin.log(p.getName() + " broke " + take + "x " + d.mob + " spawner at " + d.key() + (data.containsKey(d.key()) ? " (" + d.stack + " left)" : " (removed)"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) { e.blockList().removeIf(b -> at(b) != null); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) { e.blockList().removeIf(b -> at(b) != null); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(SpawnerSpawnEvent e) {
        if (e.getSpawner() != null && at(e.getSpawner().getBlock()) != null) e.setCancelled(true);
    }

    // ---------------------------------------------------------------- GUI actions

    void refresh(Data d) {
        for (Player p : Bukkit.getOnlinePlayers())
            if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof SpawnerMenu sm && sm.data == d) sm.render();
    }

    private void closeViewers(Data d) {
        for (Player p : Bukkit.getOnlinePlayers())
            if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof SpawnerMenu sm && sm.data == d) p.closeInventory();
    }

    boolean valid(Data d) { return data.get(d.key()) == d; }

    /** Moves up to `amount` of a material from the storage into the player's inventory. */
    void take(Player p, Data d, Material m, long amount) {
        if (!valid(d)) { p.closeInventory(); return; }
        long have = d.items.getOrDefault(m, 0L);
        amount = Math.min(amount, have);
        long moved = 0;
        while (moved < amount) {
            int n = (int) Math.min(m.getMaxStackSize(), amount - moved);
            Map<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack(m, n));
            int notAdded = left.values().stream().mapToInt(ItemStack::getAmount).sum();
            moved += n - notAdded;
            if (notAdded > 0) break;
        }
        if (moved <= 0) { msg(p, "&#FC0000Your inventory is full."); return; }
        if (have - moved <= 0) d.items.remove(m); else d.items.put(m, have - moved);
        dirty = true;
        refresh(d);
    }

    void sellAll(Player p, Data d) {
        if (!valid(d)) { p.closeInventory(); return; }
        if (!Eco.available()) { msg(p, "&#FC0000Selling is not available right now."); return; }
        double total = 0;
        long count = 0;
        Map<Material, Long> sold = new LinkedHashMap<>();
        for (var e : d.items.entrySet()) {
            double price = price(e.getKey());
            if (price <= 0) continue;
            total += price * e.getValue();
            count += e.getValue();
            sold.put(e.getKey(), e.getValue());
        }
        if (count == 0) { msg(p, "&#FC0000Nothing to sell."); return; }
        sold.keySet().forEach(d.items::remove);
        if (!Eco.deposit(p, total)) {
            sold.forEach((m, n) -> d.items.merge(m, n, Long::sum));
            msg(p, "&#FC0000Selling failed, nothing was sold.");
            return;
        }
        dirty = true;
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        msg(p, "&fSold &#FCE300" + Text.num(count) + " &fitems for &#00FC00$" + Text.num(Math.round(total * 100) / 100.0) + "&f.");
        plugin.log(p.getName() + " sold " + count + " items from " + d.mob + " spawner " + d.key() + " for $" + Text.num(total));
        refresh(d);
    }

    double value(Data d) {
        double t = 0;
        for (var e : d.items.entrySet()) t += price(e.getKey()) * e.getValue();
        return t;
    }

    void collectXp(Player p, Data d) {
        if (!valid(d)) { p.closeInventory(); return; }
        if (d.xp <= 0) { msg(p, "&#FC0000No XP stored."); return; }
        int xp = (int) Math.min(Integer.MAX_VALUE, d.xp);
        d.xp = 0;
        dirty = true;
        p.giveExp(xp, true);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        msg(p, "&fCollected &#00FC00" + Text.num(xp) + " XP&f.");
        refresh(d);
    }

    /** Drops the items shown on one page at the player's feet. */
    void dropPage(Player p, Data d, List<ItemStack> page) {
        if (!valid(d)) { p.closeInventory(); return; }
        int dropped = 0;
        for (ItemStack it : page) {
            long have = d.items.getOrDefault(it.getType(), 0L);
            int n = (int) Math.min(have, it.getAmount());
            if (n <= 0) continue;
            if (have - n <= 0) d.items.remove(it.getType()); else d.items.put(it.getType(), have - n);
            p.getWorld().dropItemNaturally(p.getLocation(), new ItemStack(it.getType(), n));
            dropped += n;
        }
        if (dropped > 0) { dirty = true; msg(p, "&fDropped &#FCE300" + Text.num(dropped) + " &fitems."); }
        refresh(d);
    }
}
