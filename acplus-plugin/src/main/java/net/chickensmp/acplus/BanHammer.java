package net.chickensmp.acplus;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

/** The Banhammer: a mace that catches cheaters with a CAUGHT! title and a cage, then punishes them. */
final class BanHammer implements Listener, CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    record Mode(String name, String color, String command, boolean cage) {}

    private final class Capture {
        final UUID target;
        final String targetName;
        final UUID staff;
        final String staffName;
        final Mode mode;
        final Location center;
        final List<BlockState> saved = new ArrayList<>();
        final List<BukkitTask> tasks = new ArrayList<>();
        int secondsLeft;

        Capture(Player t, Player s, Mode m, Location c) {
            target = t.getUniqueId(); targetName = t.getName();
            staff = s.getUniqueId(); staffName = s.getName();
            mode = m; center = c;
        }
    }

    private final SusPlugin plugin;
    private final NamespacedKey keyHammer, keyMode;
    private final Map<UUID, Capture> caged = new HashMap<>();
    private final Map<UUID, Map.Entry<UUID, Long>> pendingConfirm = new HashMap<>();
    private List<Mode> modes = new ArrayList<>();

    BanHammer(SusPlugin plugin) {
        this.plugin = plugin;
        keyHammer = new NamespacedKey(plugin, "banhammer");
        keyMode = new NamespacedKey(plugin, "banhammer_mode");
        load();
    }

    void load() {
        modes = new ArrayList<>();
        for (Map<?, ?> m : plugin.getConfig().getMapList("banhammer.modes")) {
            Object cmd = m.get("command");
            Object cage = m.get("cage");
            modes.add(new Mode(String.valueOf(m.get("name")),
                    m.get("color") == null ? "<white>" : String.valueOf(m.get("color")),
                    cmd == null ? "" : String.valueOf(cmd),
                    cage == null || Boolean.parseBoolean(String.valueOf(cage))));
        }
        if (modes.isEmpty()) modes.add(new Mode("Cage only", "<yellow>", "", true));
    }

    private ConfigurationSection cfg() {
        ConfigurationSection c = plugin.getConfig().getConfigurationSection("banhammer");
        return c != null ? c : plugin.getConfig().createSection("banhammer");
    }

    private static Component mini(String s) {
        return MM.deserialize(s).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private String fill(String s, String player, String staff, Mode mode) {
        return s.replace("{player}", MM.escapeTags(player)).replace("{staff}", MM.escapeTags(staff))
                .replace("{mode}", MM.escapeTags(mode.name()));
    }

    // ------------------------------------------------------------ item

    ItemStack createHammer(int modeIndex) {
        ItemStack it = new ItemStack(Material.MACE);
        updateItem(it, modeIndex);
        return it;
    }

    private void updateItem(ItemStack it, int modeIndex) {
        ItemMeta meta = it.getItemMeta();
        Mode mode = modes.get(modeIndex);
        meta.displayName(mini(cfg().getString("name", "<red><bold>BANHAMMER") + " <dark_gray>|</dark_gray> " + mode.color() + mode.name()));
        List<Component> lore = new ArrayList<>();
        lore.add(mini("<gray>Hit a player to catch them.</gray>"));
        lore.add(mini("<gray>Right-click to switch the mode.</gray>"));
        lore.add(Component.empty());
        for (int i = 0; i < modes.size(); i++) {
            Mode m = modes.get(i);
            lore.add(mini(i == modeIndex ? "<white>» </white>" + m.color() + "<bold>" + MM.escapeTags(m.name()) : "<dark_gray>  " + MM.escapeTags(m.name())));
        }
        meta.lore(lore);
        meta.setUnbreakable(true);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(keyHammer, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(keyMode, PersistentDataType.INTEGER, modeIndex);
        it.setItemMeta(meta);
    }

    private boolean isHammer(ItemStack it) {
        return it != null && it.getType() == Material.MACE && it.hasItemMeta()
                && it.getItemMeta().getPersistentDataContainer().has(keyHammer, PersistentDataType.BYTE);
    }

    private int modeOf(ItemStack it) {
        Integer i = it.getItemMeta().getPersistentDataContainer().get(keyMode, PersistentDataType.INTEGER);
        return i == null || i < 0 || i >= modes.size() ? 0 : i;
    }

    // ------------------------------------------------------------ switching + hitting

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack it = e.getItem();
        if (!isHammer(it)) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!p.hasPermission("chickensmp.acplus.banhammer")) return;
        int next = (modeOf(it) + 1) % modes.size();
        updateItem(it, next);
        Mode m = modes.get(next);
        p.sendActionBar(mini("<gray>Banhammer mode:</gray> " + m.color() + "<bold>" + MM.escapeTags(m.name())));
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player staff)) return;
        ItemStack it = staff.getInventory().getItemInMainHand();
        if (!isHammer(it)) return;
        e.setCancelled(true);
        if (!(e.getEntity() instanceof Player target)) return;
        if (!staff.hasPermission("chickensmp.acplus.banhammer")) {
            staff.sendMessage(mini("<red>You can't use the Banhammer.</red>"));
            return;
        }
        if (target.hasPermission("chickensmp.acplus.bypass") && !target.equals(staff)) {
            staff.sendMessage(mini("<red>" + MM.escapeTags(target.getName()) + " can't be hit by the Banhammer.</red>"));
            return;
        }
        if (caged.containsKey(target.getUniqueId())) {
            staff.sendActionBar(mini("<gray>" + MM.escapeTags(target.getName()) + " is already caught.</gray>"));
            return;
        }
        Mode mode = modes.get(modeOf(it));
        long confirmMs = cfg().getLong("confirm-seconds", 4) * 1000L;
        if (confirmMs > 0) {
            Map.Entry<UUID, Long> pend = pendingConfirm.get(staff.getUniqueId());
            long now = System.currentTimeMillis();
            if (pend == null || !pend.getKey().equals(target.getUniqueId()) || now - pend.getValue() > confirmMs) {
                pendingConfirm.put(staff.getUniqueId(), Map.entry(target.getUniqueId(), now));
                staff.sendActionBar(mini("<yellow>Hit </yellow><white>" + MM.escapeTags(target.getName())
                        + "</white><yellow> again to confirm:</yellow> " + mode.color() + "<bold>" + MM.escapeTags(mode.name())));
                staff.playSound(staff.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.6f);
                return;
            }
            pendingConfirm.remove(staff.getUniqueId());
        }
        catchPlayer(target, staff, mode);
    }

    // ------------------------------------------------------------ the catch

    private void catchPlayer(Player target, Player staff, Mode mode) {
        String title = fill(cfg().getString("title", "<dark_red><bold>CAUGHT!"), target.getName(), staff.getName(), mode);
        String sub = fill(cfg().getString("subtitle", "<red>You were caught by {staff}"), target.getName(), staff.getName(), mode);
        Location loc = target.getLocation();
        World w = target.getWorld();

        target.showTitle(Title.title(mini(title), mini(sub),
                Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(4), Duration.ofMillis(800))));
        w.strikeLightningEffect(loc);
        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.8f);
        w.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.4f);
        w.spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 1, 0), 1);
        w.spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 80, 0.6, 1, 0.6, new Particle.DustOptions(Color.RED, 2f));

        String bc = cfg().getString("broadcast", "");
        if (bc != null && !bc.isBlank()) Bukkit.broadcast(mini(fill(bc, target.getName(), staff.getName(), mode)));
        staff.sendMessage(mini("<gray>Caught </gray><white>" + MM.escapeTags(target.getName()) + "</white><gray> -</gray> "
                + mode.color() + MM.escapeTags(mode.name())));

        if (!mode.cage()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> punish(target.getUniqueId(), target.getName(), staff.getUniqueId(), mode), 30L);
            return;
        }

        // Center the player on their block and build the cage around them.
        Location center = loc.getBlock().getLocation().add(0.5, 0, 0.5);
        center.setYaw(loc.getYaw());
        center.setPitch(loc.getPitch());
        target.teleport(center);
        target.setFallDistance(0);
        target.setInvulnerable(true);
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 70, 0, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0, false, false));

        Capture c = new Capture(target, staff, mode, center);
        c.secondsLeft = Math.max(5, cfg().getInt("cage-seconds", 60));
        caged.put(target.getUniqueId(), c);

        Material bars = mat("cage-bars", Material.IRON_BARS);
        Material floor = mat("cage-floor", Material.OBSIDIAN);
        Material roof = mat("cage-roof", Material.RED_STAINED_GLASS);
        Block base = center.getBlock();

        // Clear the space inside right away so they can't suffocate.
        for (int dy = 0; dy <= 2; dy++) place(c, base.getRelative(0, dy, 0), Material.AIR);

        // Build layer by layer: floor, three rings of bars, roof.
        int[][] ring = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
        for (int layer = 0; layer <= 4; layer++) {
            final int l = layer;
            c.tasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!caged.containsKey(c.target)) return;
                if (l == 0) {
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) place(c, base.getRelative(dx, -1, dz), floor);
                    w.playSound(center, Sound.BLOCK_ANVIL_LAND, 1f, 0.6f);
                } else if (l <= 3) {
                    for (int[] r : ring) place(c, base.getRelative(r[0], l - 1, r[1]), bars);
                    w.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.7f + l * 0.15f);
                } else {
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) place(c, base.getRelative(dx, 3, dz), roof);
                    w.playSound(center, Sound.BLOCK_IRON_DOOR_CLOSE, 1.2f, 0.6f);
                    w.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1f);
                    plugin.getLogger().info("Caged " + c.targetName + " at " + base.getX() + " " + base.getY() + " " + base.getZ());
                }
                w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 1, 0), 25, 0.8, 1, 0.8, 0.02);
            }, 6L + layer * 6L));
        }

        // Every second: countdown, particles, keep them in place.
        c.tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player t = Bukkit.getPlayer(c.target);
            if (t == null || !caged.containsKey(c.target)) return;
            if (c.secondsLeft <= 0) { finish(c, true); return; }
            String what = c.mode.command().isBlank() ? "Released in" : "Punishment in";
            t.sendActionBar(mini("<dark_red><bold>CAUGHT</bold></dark_red> <dark_gray>|</dark_gray> <gray>" + what + "</gray> <red><bold>" + c.secondsLeft + "s"));
            if (c.secondsLeft % 15 == 0 && c.secondsLeft != Math.max(5, cfg().getInt("cage-seconds", 60))) {
                t.showTitle(Title.title(mini(title), mini("<gray>" + what + " <red>" + c.secondsLeft + "s"),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))));
            }
            if (c.secondsLeft <= 5) t.playSound(t.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f + c.secondsLeft * 0.1f);
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2 / 16) * i;
                w.spawnParticle(Particle.DUST, center.clone().add(Math.cos(a) * 1.6, 0.1 + (c.secondsLeft % 3), Math.sin(a) * 1.6),
                        1, new Particle.DustOptions(Color.RED, 1.4f));
            }
            if (t.getLocation().distanceSquared(center) > 0.5) t.teleport(center);
            c.secondsLeft--;
        }, 20L, 20L));
    }

    private Material mat(String key, Material def) {
        Material m = Material.matchMaterial(cfg().getString(key, def.name()));
        return m == null || !m.isBlock() ? def : m;
    }

    private void place(Capture c, Block b, Material m) {
        if (b.getY() < b.getWorld().getMinHeight() || b.getY() >= b.getWorld().getMaxHeight()) return;
        if (b.getType() == m) return;
        BlockState st = b.getState();
        if (st instanceof TileState) return; // never touch chests, signs etc.
        if (c.saved.stream().noneMatch(s -> s.getLocation().equals(b.getLocation()))) c.saved.add(st);
        b.setType(m, false);
    }

    private void restore(Capture c) {
        for (int i = c.saved.size() - 1; i >= 0; i--) c.saved.get(i).update(true, false);
        c.saved.clear();
    }

    /** End a capture: tear down the cage, then (optionally) run the punishment. */
    private void finish(Capture c, boolean punish) {
        if (caged.remove(c.target) == null) return;
        c.tasks.forEach(BukkitTask::cancel);
        restore(c);
        plugin.getLogger().info("Cage removed for " + c.targetName);
        Player t = Bukkit.getPlayer(c.target);
        if (t != null) {
            t.setInvulnerable(false);
            t.removePotionEffect(PotionEffectType.GLOWING);
            t.getWorld().playSound(t.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.6f);
            t.getWorld().spawnParticle(Particle.CLOUD, t.getLocation().add(0, 1, 0), 40, 0.8, 1, 0.8, 0.05);
        }
        if (punish) punish(c.target, c.targetName, c.staff, c.mode);
    }

    private void punish(UUID target, String targetName, UUID staffId, Mode mode) {
        if (mode.command() == null || mode.command().isBlank()) return;
        String cmd = mode.command().replace("{player}", targetName);
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        Player staff = Bukkit.getPlayer(staffId);
        CommandSender as = staff != null ? staff : Bukkit.getConsoleSender();
        plugin.getLogger().info("Banhammer: " + as.getName() + " -> /" + cmd);
        Bukkit.dispatchCommand(as, cmd);
        plugin.onPossibleBan(cmd);
    }

    /** Called on plugin disable: tear down all cages and still run their punishments. */
    void shutdown() {
        for (Capture c : new ArrayList<>(caged.values())) finish(c, true);
    }

    // ------------------------------------------------------------ freeze the caught player

    private boolean isCaged(Player p) {
        return caged.containsKey(p.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!isCaged(e.getPlayer())) return;
        Location f = e.getFrom(), t = e.getTo();
        if (f.getX() != t.getX() || f.getY() != t.getY() || f.getZ() != t.getZ()) {
            Location keep = f.clone();
            keep.setYaw(t.getYaw());
            keep.setPitch(t.getPitch());
            e.setTo(keep);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!isCaged(e.getPlayer())) return;
        e.setCancelled(true);
        e.getPlayer().sendActionBar(mini("<red>You can't do that while caught.</red>"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        if (caged.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUse(PlayerInteractEvent e) { if (isCaged(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUseEntity(PlayerInteractEntityEvent e) { if (isCaged(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent e) { if (isCaged(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent e) { if (isCaged(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent e) { if (isCaged(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInv(InventoryOpenEvent e) { if (e.getPlayer() instanceof Player p && isCaged(p)) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTeleport(PlayerTeleportEvent e) {
        Capture c = caged.get(e.getPlayer().getUniqueId());
        if (c != null && e.getTo().distanceSquared(c.center) > 0.5) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && isCaged(p)) e.setCancelled(true);
        if (e instanceof EntityDamageByEntityEvent ee && ee.getDamager() instanceof Player p && isCaged(p)) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pendingConfirm.remove(e.getPlayer().getUniqueId());
        Capture c = caged.get(e.getPlayer().getUniqueId());
        if (c == null) return;
        // Logging out does not save them: punish right away.
        e.getPlayer().setInvulnerable(false);
        Bukkit.getScheduler().runTask(plugin, () -> finish(c, true));
    }

    // ------------------------------------------------------------ /banhammer

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length >= 2 && a[0].equalsIgnoreCase("release")) {
            Player t = Bukkit.getPlayerExact(a[1]);
            Capture c = t == null ? null : caged.get(t.getUniqueId());
            if (c == null) { s.sendMessage(mini("<red>" + MM.escapeTags(a[1]) + " is not caught.</red>")); return true; }
            finish(c, false);
            s.sendMessage(mini("<green>Released " + MM.escapeTags(t.getName()) + " without punishment.</green>"));
            t.sendMessage(mini("<green>You were released.</green>"));
            return true;
        }
        if (a.length >= 1 && a[0].equalsIgnoreCase("list")) {
            if (caged.isEmpty()) { s.sendMessage(mini("<gray>Nobody is caught right now.</gray>")); return true; }
            for (Capture c : caged.values())
                s.sendMessage(mini("<red>" + MM.escapeTags(c.targetName) + "</red> <gray>by " + MM.escapeTags(c.staffName)
                        + ", " + c.mode.color() + MM.escapeTags(c.mode.name()) + "</gray> <gray>(" + c.secondsLeft + "s left)</gray>"));
            return true;
        }
        if (!(s instanceof Player p)) { s.sendMessage("Usage: /banhammer [release <player>|list]"); return true; }
        p.getInventory().addItem(createHammer(0)).values().forEach(rest -> p.getWorld().dropItem(p.getLocation(), rest));
        p.sendMessage(mini("<gray>Here is your</gray> " + cfg().getString("name", "<red><bold>BANHAMMER") + "<gray>. Right-click switches the mode.</gray>"));
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.8f);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        if (a.length == 1) return List.of("release", "list").stream().filter(x -> x.startsWith(a[0].toLowerCase(Locale.ROOT))).toList();
        if (a.length == 2 && a[0].equalsIgnoreCase("release")) {
            List<String> out = new ArrayList<>();
            for (Capture c : caged.values()) out.add(c.targetName);
            return out;
        }
        return List.of();
    }
}
