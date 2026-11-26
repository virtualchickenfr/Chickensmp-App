package dev.killeffects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Mystery Roll: pay (default 50M) for one random kill effect you don't own yet. A big public
 * animation plays in front of the player (everyone around sees it), great pulls are announced in
 * chat, and there is a chance to get two effects.
 */
final class MysteryRoll {
    private static final Set<UUID> BUSY = new HashSet<>();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private MysteryRoll() {}

    static boolean enabled(KillEffects p) { return p.getConfig().getBoolean("mystery-roll.enabled", true); }
    static double price(KillEffects p) { return p.getConfig().getDouble("mystery-roll.price", 50_000_000); }
    static double doubleChance(KillEffects p) { return p.getConfig().getDouble("mystery-roll.double-chance", 10); }

    private static String prefix(KillEffects p) { return p.raw("messages.prefix"); }
    private static void say(KillEffects p, Player pl, String text) { pl.sendMessage(Text.color(prefix(p) + text)); }

    /** Effects the player can still get (not owned, not exclusive). */
    static List<KillEffect> pool(KillEffects p, Player pl) {
        List<KillEffect> l = new ArrayList<>();
        for (KillEffect e : p.effects().values()) {
            if (Effects3.EXCLUSIVE.contains(e.id()) || p.owns(pl, e)) continue;
            l.add(e);
        }
        return l;
    }

    /** Pays, picks the prize(s), saves them and starts the show. */
    static void start(KillEffects p, Player pl) {
        if (!enabled(p)) return;
        if (BUSY.contains(pl.getUniqueId())) { say(p, pl, "&fYour roll is still running."); return; }
        List<KillEffect> pool = pool(p, pl);
        if (pool.isEmpty()) { say(p, pl, "&fYou already own &#FCE300every &fkill effect!"); p.sound(pl, "error"); return; }
        double price = price(p);
        if (price > 0) {
            if (p.economy() == null) { say(p, pl, "&fThe economy is not available."); return; }
            if (!p.economy().has((OfflinePlayer) pl, price)) {
                say(p, pl, "&fYou need &#FCE300" + p.money(price) + " &ffor a Mystery Roll.");
                p.sound(pl, "error");
                return;
            }
            if (!p.economy().withdrawPlayer((OfflinePlayer) pl, price).transactionSuccess()) { say(p, pl, "&fPayment failed."); return; }
        }
        Collections.shuffle(pool, p.random);
        List<KillEffect> won = new ArrayList<>();
        won.add(pool.get(0));
        boolean dbl = pool.size() > 1 && p.random.nextDouble() * 100 < doubleChance(p);
        if (dbl) won.add(pool.get(1));
        Store.Profile profile = p.profileOrCreate(pl.getUniqueId(), pl.getName());
        for (KillEffect e : won) profile.owned.add(e.id());
        profile.opened++;
        p.save(pl.getUniqueId());
        StringBuilder log = new StringBuilder();
        for (KillEffect e : won) log.append(' ').append(e.id());
        p.getLogger().info("Mystery Roll: " + pl.getName() + " paid " + price + " ->" + log);
        pl.closeInventory();
        BUSY.add(pl.getUniqueId());
        try {
            show(p, pl, won);
        } catch (Exception ex) {
            p.getLogger().warning("Mystery Roll animation failed: " + ex);
            BUSY.remove(pl.getUniqueId());
            reveal(p, pl, won);
        }
    }

    // ------------------------------------------------------------------ the show

    private static ItemStack icon(KillEffect e) { return new ItemStack(e.fallback()); }

    private static ItemDisplay item(Fx fx, Location at, ItemStack it) {
        ItemDisplay d = fx.world.spawn(at, ItemDisplay.class, x -> {
            x.setItemStack(it);
            fx.prepareDisplay(x);
            x.setBillboard(Display.Billboard.FIXED);
            x.setTransformation(tf(0.001f, 0));
        });
        fx.trackEntity(d);
        return d;
    }

    private static Transformation tf(float scale, double yawDeg) {
        return new Transformation(new Vector3f(), new Quaternionf().rotateY((float) Math.toRadians(yawDeg)),
                new Vector3f(scale, scale, scale), new Quaternionf());
    }

    private static void pose(ItemDisplay d, float scale, double yaw, int dur) {
        if (!d.isValid()) return;
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(dur);
        d.setTransformation(tf(scale, yaw));
    }

    private static TextDisplay label(Fx fx, Location at, String legacy) {
        TextDisplay t = fx.world.spawn(at, TextDisplay.class, x -> {
            x.text(LEGACY.deserialize(Text.color(legacy)));
            x.setDefaultBackground(false);
            x.setBackgroundColor(Color.fromARGB(0x50000000));
            x.setShadowed(true);
            x.setAlignment(TextDisplay.TextAlignment.CENTER);
            fx.prepareDisplay(x);
            x.setBillboard(Display.Billboard.CENTER);
            x.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.001f, 0.001f, 0.001f), new Quaternionf()));
        });
        fx.trackEntity(t);
        return t;
    }

    private static void scaleText(TextDisplay t, float s, int dur) {
        if (!t.isValid()) return;
        t.setInterpolationDelay(0);
        t.setInterpolationDuration(dur);
        t.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(s, s, s), new Quaternionf()));
    }

    private static void beamColor(Kit.Beam b, int rgb, double k, double w, int dur) {
        b.outer.size(w, 30).color(((int) (0x1C * k)) << 24 | rgb).apply(dur);
        b.mid.size(w * 0.55, 30).color(((int) (0x32 * k)) << 24 | rgb).apply(dur);
        b.core.size(w * 0.16, 30).color(((int) (0x90 * k)) << 24 | 0xFFFFFF).apply(dur);
    }

    private static void show(KillEffects p, Player pl, List<KillEffect> won) {
        Location eye = pl.getLocation();
        Vector fwd = eye.getDirection().setY(0);
        if (fwd.lengthSquared() < 1e-4) fwd = new Vector(0, 0, 1);
        fwd.normalize();
        Location base = eye.clone().add(fwd.clone().multiply(3));
        base.setY(Math.floor(eye.getY() + 0.01));
        // stand on something solid if the spot in front floats
        for (int i = 0; i < 3 && !base.clone().add(0, -0.5, 0).getBlock().getType().isSolid(); i++) base.add(0, -1, 0);
        base.setYaw(0);
        base.setPitch(0);
        if (Trace.ON) Trace.name = "mystery";
        Fx fx = new Fx(p, base, null, p.customModels());
        fx.faceTarget = pl.getLocation();
        KillEffect first = won.get(0);
        int rgb = first.rarity().rgb;
        List<KillEffect> all = new ArrayList<>(p.effects().values());
        all.removeIf(e -> Effects3.EXCLUSIVE.contains(e.id()));

        // opening
        fx.sound("minecraft:block.ender_chest.open", 1.2f, 0.6f);
        fx.sound("minecraft:block.beacon.activate", 1.4f, 0.8f);
        fx.sound(2, "minecraft:block.respawn_anchor.charge", 1.2f, 1.0f);
        Kit.Beam beam = new Kit.Beam(fx, base.clone().subtract(fwd.clone().multiply(0.7)), 0xFFFFFF);
        fx.later(2, () -> beamColor(beam, 0xFFFFFF, 0.8, 1.6, 8));
        Kit.Ring ring = new Kit.Ring(fx, base.clone().add(0, 0.06, 0), 1.8, 0.12, 24, 0x00FFFFFF);
        Kit.Ring ring2 = new Kit.Ring(fx, base.clone().add(0, 0.07, 0), 1.1, 0.07, 18, 0x00FFFFFF);
        ring.apply(0);
        ring2.apply(0);
        Location orbAt = base.clone().add(0, 1.8, 0);
        ItemDisplay orb = item(fx, orbAt, icon(all.get(p.random.nextInt(all.size()))));
        fx.later(1, () -> pose(orb, 1.3f, 0, 10));
        Kit.Disc glow = new Kit.Disc(fx, orbAt.clone().add(0, 0.15, 0).subtract(fwd.clone().multiply(0.5)), 0x00FFFFFF);
        fx.later(1, () -> glow.set(1.5, 0x14FFFFFF, 10));

        // the reel: icons swap fast, then slow down; beam and rings flash in the shown rarity colour
        int[] t = {0};
        TextDisplay[] names = new TextDisplay[1];
        int spin = 0;
        int tick = 4;
        int n = 0;
        while (tick < 88) {
            final int at = tick;
            final boolean last = tick + Math.max(2, (int) Math.round(1 + Math.pow(tick / 88.0, 3) * 10)) >= 88;
            final KillEffect show = last ? first : all.get(p.random.nextInt(all.size()));
            final int idx = n++;
            fx.later(at, () -> {
                if (orb.isValid()) orb.setItemStack(icon(show));
                int c = show.rarity().rgb;
                beamColor(beam, c, 0.9, 1.6, 2);
                ring.argb = 0xC0 << 24 | c;
                ring2.argb = 0xA0 << 24 | 0xFFFFFF;
                glow.set(1.5, 0x16 << 24 | c, 2);
                fx.sound("minecraft:block.note_block.hat", 1f, 0.7f + Math.min(1.3f, idx * 0.05f));
                fx.sound("minecraft:block.note_block.pling", 0.5f, 0.6f + Math.min(1.4f, idx * 0.06f));
                fx.dust(orbAt.clone().add(fx.rand(0.6), fx.rand(0.6), fx.rand(0.6)), c, 1.2f, 6, 0.1);
            });
            tick += Math.max(2, (int) Math.round(1 + Math.pow(tick / 88.0, 3) * 10));
        }
        // continuous motion: rings spin, orb turns and bobs, sparks rise
        fx.anim(1, 100, 1, f -> {
            ring.r = 1.8 - 0.5 * f;
            ring.spin += 0.10 + 0.25 * f;
            ring2.spin -= 0.16 + 0.3 * f;
            ring.apply(1);
            ring2.apply(1);
            t[0]++;
            if (orb.isValid()) {
                orb.setTeleportDuration(1);
                Location l = orbAt.clone().add(0, Math.sin(t[0] * 0.18) * 0.12, 0);
                orb.teleport(l);
                if (t[0] < 92) pose(orb, 1.3f, t[0] * (8 + 22 * f), 1); else pose(orb, 2.4f, t[0] * 4, 1);
            }
            if (t[0] % 2 == 0) fx.p(Particle.END_ROD, base.clone().add(fx.rand(1.4), 0.2, fx.rand(1.4)), 1, 0, 0.06);
        });
        fx.repeat(8, 10, i -> Kit.converge(fx, orbAt, 2.8, 4, 0xFFFFFF, 10));
        fx.later(76, () -> {
            fx.sound("minecraft:entity.warden.sonic_charge", 1f, 1.4f);
            fx.sound("minecraft:block.beacon.power_select", 1.2f, 1.2f);
        });

        // REVEAL
        fx.later(92, () -> {
            if (orb.isValid()) orb.setItemStack(icon(first));
            pose(orb, 2.4f, 0, 4);
            beamColor(beam, rgb, 1.0, 2.4, 3);
            glow.set(2.0, 0x14 << 24 | rgb, 3);
            ring.argb = 0x00 << 24 | rgb;
            ring.r = 6;
            ring.apply(10);
            fx.particle(Particle.FLASH, orbAt, 1, 0, 0, 0, 0, Kit.col(rgb));
            Kit.burst(fx, orbAt, 4.5, 40, rgb, 14);
            Kit.firework(fx, orbAt.clone().add(0, 0.5, 0), first.rarity().ordinal() >= Rarity.LEGENDARY.ordinal() ? FireworkEffect.Type.BALL_LARGE : FireworkEffect.Type.BURST, true, true, rgb, 0xFFFFFF);
            fx.p(Particle.TOTEM_OF_UNDYING, orbAt, 40 + first.rarity().ordinal() * 25, 0.6, 0.5);
            fx.sound("minecraft:entity.player.levelup", 1.2f, 1.2f);
            fx.sound("minecraft:ui.toast.challenge_complete", 1f, 1.0f);
            if (first.rarity().ordinal() >= Rarity.LEGENDARY.ordinal()) {
                fx.sound("minecraft:entity.lightning_bolt.thunder", 1f, 1.2f);
                fx.sound("minecraft:item.totem.use", 1f, 1.0f);
                Kit.wave(fx, 1.0, 5.0, 14, 5, 0.6);
                fx.repeat(4, 6, i -> Kit.firework(fx, orbAt.clone().add(fx.rand(2.5), 1.5 + fx.rnd.nextDouble() * 2, fx.rand(2.5)), FireworkEffect.Type.STAR, true, true, rgb, 0xFFFFFF));
            }
            TextDisplay name = label(fx, orbAt.clone().add(0, 1.6, 0), first.rarity().label() + "\n&f" + first.colored());
            names[0] = name;
            scaleText(name, 1.4f, 6);
        });
        int end = 150;
        if (won.size() > 1) {
            KillEffect second = won.get(1);
            int rgb2 = second.rarity().rgb;
            end = 180;
            fx.later(116, () -> {
                TextDisplay dbl = label(fx, orbAt.clone().add(0, 2.6, 0), "&#FCE300&l✦ DOUBLE ROLL! ✦");
                scaleText(dbl, 2.2f, 5);
                fx.sound("minecraft:entity.player.levelup", 1.2f, 1.6f);
                fx.sound("minecraft:block.amethyst_block.resonate", 1.2f, 1.4f);
                fx.repeat(6, 3, i -> fx.sound("minecraft:block.note_block.bell", 1f, 1.0f + i * 0.15f));
            });
            Vector side = new Vector(-fwd.getZ(), 0, fwd.getX()).multiply(1.6);
            fx.later(126, () -> {
                // the orb splits: the first prize moves aside, the second appears on the other side
                Location a = orbAt.clone().add(side);
                Location b = orbAt.clone().subtract(side);
                orb.setTeleportDuration(6);
                orb.teleport(a);
                if (names[0] != null && names[0].isValid()) {
                    names[0].setTeleportDuration(6);
                    names[0].teleport(a.clone().add(0, 1.6, 0));
                }
                ItemDisplay orb2 = item(fx, b, icon(second));
                fx.later(1, () -> pose(orb2, 2.4f, 0, 6));
                fx.anim(8, 44, 1, f -> pose(orb2, 2.4f, f * 180, 1));
                fx.particle(Particle.FLASH, b, 1, 0, 0, 0, 0, Kit.col(rgb2));
                Kit.burst(fx, b, 4, 30, rgb2, 14);
                Kit.firework(fx, b.clone().add(0, 0.5, 0), FireworkEffect.Type.BURST, true, true, rgb2, 0xFFFFFF);
                fx.p(Particle.TOTEM_OF_UNDYING, b, 60, 0.6, 0.5);
                fx.sound("minecraft:ui.toast.challenge_complete", 1f, 1.3f);
                TextDisplay name2 = label(fx, b.clone().add(0, 1.6, 0), second.rarity().label() + "\n&f" + second.colored());
                scaleText(name2, 1.4f, 6);
            });
        }
        final int fin = end;
        fx.later(fin - 30, () -> {
            BUSY.remove(pl.getUniqueId());
            if (pl.isOnline()) reveal(p, pl, won);
        });
        // fade out
        fx.later(fin - 10, () -> {
            beamColor(beam, rgb, 0, 0.01, 10);
            glow.set(0.01, 0x00FFFFFF, 10);
            for (org.bukkit.entity.Entity e : new ArrayList<>(fx.spawned)) {
                if (e instanceof ItemDisplay id) pose(id, 0.001f, 0, 10);
                if (e instanceof TextDisplay td && td.getText() != null && !td.getText().equals(" ")) scaleText(td, 0.001f, 10);
            }
            fx.p(Particle.CLOUD, orbAt, 20, 0.5, 0.03);
            fx.sound("minecraft:block.beacon.deactivate", 1f, 1.2f);
        });
        fx.later(fin + 2, fx::cleanup);
    }

    /** Title, chat message and the broadcast for great pulls. */
    static void reveal(KillEffects p, Player pl, List<KillEffect> won) {
        KillEffect first = won.get(0);
        String sub = first.colored() + (won.size() > 1 ? " &f+ " + won.get(1).colored() : "");
        pl.sendTitle(Text.color(won.size() > 1 ? "&#FCE300&lDOUBLE ROLL!" : first.rarity().label()), Text.color(sub), 5, 60, 15);
        for (KillEffect e : won) {
            say(p, pl, "&fYou got " + e.colored() + " &f(" + e.rarity().label() + "&f)! Select it in &#FCE300/ke&f.");
            p.sound(pl, "win-" + e.rarity().key());
        }
        Rarity from = Rarity.parse(p.getConfig().getString("mystery-roll.broadcast-from", "LEGENDARY"));
        boolean big = false;
        for (KillEffect e : won) if (from != null && e.rarity().ordinal() >= from.ordinal()) big = true;
        if (big || won.size() > 1) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < won.size(); i++) {
                if (i > 0) s.append(" &f+ ");
                s.append(won.get(i).colored()).append(" &f(").append(won.get(i).rarity().label()).append("&f)");
            }
            p.broadcast(Text.color(prefix(p) + "&#FCE300" + pl.getName() + " &fpulled " + s + " &ffrom a &#A303F9&lMystery Roll"
                    + (won.size() > 1 ? " &fand got a &#FCE300&lDOUBLE&f!" : "&f!")));
        }
    }
}
