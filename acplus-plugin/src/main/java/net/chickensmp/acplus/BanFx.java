package net.chickensmp.acplus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Ban animation: when staff (or the console) bans an online player, the command is held back for a few
 * seconds while the player is lifted into a red beam, gets a BANNED title and is struck by lightning.
 * Then the original command runs unchanged. The Banhammer has its own animation and is not affected.
 */
final class BanFx implements Listener {

    private static final Pattern BAN = Pattern.compile("^/?(?:[\\w-]+:)?(ban|tempban|ipban|tempipban|banip)\\s+(\\S+)(.*)$", Pattern.CASE_INSENSITIVE);
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final SusPlugin plugin;
    /** Commands we are re-running ourselves (must not be caught again). */
    private final Set<String> passing = new HashSet<>();
    /** Players that are currently being animated. */
    private final Set<java.util.UUID> busy = new HashSet<>();

    BanFx(SusPlugin plugin) { this.plugin = plugin; }

    private boolean enabled() { return plugin.getConfig().getBoolean("ban-animation.enabled", true); }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        if (catchBan(e.getPlayer(), e.getMessage())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent e) {
        if (catchBan(e.getSender(), e.getCommand())) e.setCancelled(true);
    }

    /** True if the command is a ban of an online player and the animation took it over. */
    private boolean catchBan(CommandSender sender, String raw) {
        if (!enabled()) return false;
        String cmd = raw.startsWith("/") ? raw.substring(1) : raw;
        if (passing.remove(cmd)) return false;
        Matcher m = BAN.matcher(cmd.trim());
        if (!m.find()) return false;
        if ((" " + m.group(3) + " ").matches("(?is).*\\s-s\\s.*")) return false; // silent ban: no show
        Player target = Bukkit.getPlayerExact(m.group(2));
        if (target == null || busy.contains(target.getUniqueId()) || target.equals(sender)) return false;
        // only when the sender may actually use that ban command
        String label = cmd.trim().split("\\s+")[0];
        Command c = Bukkit.getCommandMap().getCommand(label);
        if (c == null || !c.testPermissionSilent(sender)) return false;
        String by = sender instanceof Player p ? p.getName() : "Console";
        String reason = reasonOf(m.group(3));
        plugin.getLogger().info("Ban animation: " + by + " -> /" + cmd);
        play(target, by, reason, () -> {
            passing.add(cmd);
            Bukkit.dispatchCommand(sender instanceof Player p && !p.isOnline() ? Bukkit.getConsoleSender() : sender, cmd);
            passing.remove(cmd);
            plugin.onPossibleBan(cmd);
        });
        return true;
    }

    /** The reason part of "/ban name [duration] reason". */
    private static String reasonOf(String rest) {
        String r = rest.trim();
        if (r.isEmpty()) return "";
        String[] p = r.split("\\s+", 2);
        if (p[0].matches("(?i)\\d+[smhdwy]|\\d+mo|perm(anent)?|-s|-p")) r = p.length > 1 ? p[1] : "";
        return r.replaceAll("(?i)\\s-[sp]\\b", "").trim();
    }

    // ------------------------------------------------------------ the animation

    private void play(Player t, String by, String reason, Runnable ban) {
        busy.add(t.getUniqueId());
        int total = Math.max(40, plugin.getConfig().getInt("ban-animation.ticks", 70));
        Location base = t.getLocation().clone();
        List<Entity> spawned = new ArrayList<>();
        boolean[] done = {false};
        Runnable finish = () -> {
            if (done[0]) return;
            done[0] = true;
            busy.remove(t.getUniqueId());
            for (Entity e : spawned) if (e.isValid()) e.remove();
            if (t.isOnline()) {
                t.removePotionEffect(PotionEffectType.LEVITATION);
                t.removePotionEffect(PotionEffectType.SLOWNESS);
                t.setInvulnerable(false);
            }
            try { ban.run(); } catch (Exception ex) { plugin.getLogger().warning("Ban after animation failed: " + ex); }
        };

        // freeze and lift
        t.setInvulnerable(true);
        t.closeInventory();
        t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, total + 20, 255, false, false, false));
        t.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, total, 0, false, false, false));
        t.showTitle(Title.title(MM.deserialize("<color:#FC0000><bold>BANNED</bold></color>"),
                MM.deserialize(reason.isEmpty() ? "<white>by <color:#FCE300>" + esc(by) : "<white>" + esc(reason)),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(total * 50L), Duration.ofMillis(400))));
        t.getWorld().playSound(base, Sound.BLOCK_BEACON_ACTIVATE, 1.4f, 0.5f);
        t.getWorld().playSound(base, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.4f, 0.6f);
        t.playSound(t.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.8f);
        t.spawnParticle(Particle.ELDER_GUARDIAN, t.getLocation(), 1);

        // red beam from the sky (3 soft layers) and two rings on the ground
        Location sky = base.clone().add(0, 15, 0);
        TextDisplay outer = glow(sky, 0x00FC0000, Display.Billboard.VERTICAL, spawned);
        TextDisplay mid = glow(sky, 0x00FC0000, Display.Billboard.VERTICAL, spawned);
        TextDisplay core = glow(sky, 0x00FFFFFF, Display.Billboard.VERTICAL, spawned);
        later(2, () -> {
            size(outer, 2.6, 30, 0x24FC0000, 8);
            size(mid, 1.3, 30, 0x40FC0000, 8);
            size(core, 0.35, 30, 0x90FFD0D0, 8);
        });
        List<TextDisplay> ring = new ArrayList<>();
        for (int i = 0; i < 20; i++) ring.add(glow(base.clone().add(0, 0.05, 0), 0x00FC0000, Display.Billboard.FIXED, spawned));

        for (int k = 0; k < total; k++) {
            final int tick = k;
            later(k, () -> {
                if (done[0]) return;
                if (!t.isOnline()) { finish.run(); return; }
                Location at = t.getLocation();
                double f = (double) tick / total;
                // ring spins and shrinks towards the player
                double r = 2.2 - 1.2 * f;
                for (int i = 0; i < ring.size(); i++) {
                    double a = tick * 0.15 + 2 * Math.PI * i / ring.size();
                    Location l = base.clone().add(Math.cos(a) * r, 0.05, Math.sin(a) * r);
                    TextDisplay d = ring.get(i);
                    if (!d.isValid()) continue;
                    d.setTeleportDuration(1);
                    d.teleport(l);
                    Quaternionf q = new Quaternionf().rotateY((float) (-a + Math.PI / 2)).rotateX((float) (-Math.PI / 2));
                    double len = 2 * Math.PI * r / ring.size() * 1.1;
                    d.setInterpolationDelay(0);
                    d.setInterpolationDuration(1);
                    d.setTransformation(tf(len, 0.14, q));
                    d.setBackgroundColor(Color.fromARGB((int) (0xC0 * Math.min(1, tick / 6.0)) << 24 | 0xFC0000));
                }
                // energy rushes in
                for (int i = 0; i < 2; i++) {
                    double a = Math.random() * Math.PI * 2;
                    Location from = at.clone().add(Math.cos(a) * 3, Math.random() * 2.5, Math.sin(a) * 3);
                    at.getWorld().spawnParticle(Particle.TRAIL, from, 1, 0, 0, 0, 0,
                            new Particle.Trail(at.clone().add(0, 1, 0), Color.fromRGB(tick % 2 == 0 ? 0xFC0000 : 0xFF6A00), 10));
                }
                at.getWorld().spawnParticle(Particle.DUST, at.clone().add(0, 1, 0), 3, 0.35, 0.6, 0.35, 0, new Particle.DustOptions(Color.fromRGB(0xFC0000), 1.3f));
                if (tick % 3 == 0) at.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, at.clone().add(0, 1, 0), 4, 0.4, 0.6, 0.4, 0.05);
                if (tick % 10 == 0) at.getWorld().playSound(at, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.2f, (float) (0.5 + f));
                if (tick == total / 2) at.getWorld().playSound(at, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.2f, 0.8f);
            });
        }

        // the strike
        later(total, () -> {
            if (done[0]) return;
            Location at = t.isOnline() ? t.getLocation() : base;
            at.getWorld().strikeLightningEffect(at);
            at.getWorld().spawnParticle(Particle.FLASH, at.clone().add(0, 1, 0), 1, 0, 0, 0, 0, Color.fromRGB(0xFF4040));
            at.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, at.clone().add(0, 1, 0), 1);
            at.getWorld().spawnParticle(Particle.DUST, at.clone().add(0, 1, 0), 80, 0.8, 1.2, 0.8, 0, new Particle.DustOptions(Color.fromRGB(0xFC0000), 2f));
            at.getWorld().spawnParticle(Particle.LARGE_SMOKE, at.clone().add(0, 1, 0), 25, 0.6, 0.8, 0.6, 0.04);
            at.getWorld().playSound(at, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.4f, 0.7f);
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
            at.getWorld().playSound(at, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.4f, 0.6f);
            finish.run();
        });
        // safety: whatever happens, the ban runs
        later(total + 40, finish);
    }

    private static String esc(String s) { return MM.escapeTags(s); }

    private void later(int ticks, Runnable r) { Bukkit.getScheduler().runTaskLater(plugin, r, Math.max(1, ticks)); }

    private TextDisplay glow(Location at, int argb, Display.Billboard bb, List<Entity> spawned) {
        Location l = at.clone();
        l.setYaw(0);
        l.setPitch(0);
        TextDisplay d = l.getWorld().spawn(l, TextDisplay.class, t -> {
            t.text(Component.text(" "));
            t.setDefaultBackground(false);
            t.setBackgroundColor(Color.fromARGB(argb));
            t.setShadowed(false);
            t.setPersistent(false);
            t.setBillboard(bb);
            t.setBrightness(new Display.Brightness(15, 15));
            t.setViewRange(2f);
            t.setTransformation(tf(0.001, 0.001, new Quaternionf()));
        });
        spawned.add(d);
        return d;
    }

    private static void size(TextDisplay d, double w, double h, int argb, int dur) {
        if (!d.isValid()) return;
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(dur);
        d.setTransformation(tf(w, h, new Quaternionf()));
        d.setBackgroundColor(Color.fromARGB(argb));
    }

    /** " " has a background of 0.125 x 0.25 blocks; centre it on the entity. */
    private static Transformation tf(double w, double h, Quaternionf r) {
        Vector3f t = new Quaternionf(r).transform(new Vector3f(0, (float) (-h / 2), 0));
        return new Transformation(t, new Quaternionf(r), new Vector3f((float) (w / 0.125), (float) (h / 0.25), 1f), new Quaternionf());
    }

    /** Called on disable: nothing to clean up beyond the entities (they are not persistent). */
    void shutdown() { busy.clear(); }

    static boolean isBanCommand(String s) { return BAN.matcher(s.toLowerCase(Locale.ROOT)).find(); }
}
