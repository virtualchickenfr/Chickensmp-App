package dev.killeffects;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Building blocks for the v4 effects. All positions are in the "killer frame": +z points at the killer,
 * +x to the killer's left... so every effect looks the same from where it is watched.
 */
final class Kit {
    private Kit() {}

    // EconomyCore palette
    static final int G = 0x00FC00, B = 0x00A4FC, Y = 0xFCE300, R = 0xFC0000, P = 0xFC00E3, PU = 0xA303F9, C = 0x1EB8E9, M = 0x00FC99, O = 0xF97603, W = 0xFFFFFF;

    // ------------------------------------------------------------------ frame

    /** Killer-frame offset -> world location. */
    static Location at(Fx fx, double x, double y, double z) {
        double f = Math.toRadians(fx.face());
        double c = Math.cos(f), s = Math.sin(f);
        return fx.base.clone().add(x * c + z * s, y, -x * s + z * c);
    }

    /** Killer-frame direction -> world vector. */
    static Vector dir(Fx fx, double x, double y, double z) {
        double f = Math.toRadians(fx.face());
        double c = Math.cos(f), s = Math.sin(f);
        return new Vector(x * c + z * s, y, -x * s + z * c);
    }

    /** A model whose +z faces the killer. */
    static Model root(Fx fx) {
        return fx.model(0, 0, 0).rot(fx.face(), 0, 0);
    }

    /** The victim (mannequin with their skin), facing the killer. */
    static Doll doll(Fx fx) {
        return new Doll(fx, 0, 0, 0);
    }

    /** Move the doll in the killer frame. */
    static void dollAt(Fx fx, Doll d, double x, double y, double z) {
        Location l = at(fx, x, y, z);
        d.at(l.getX() - fx.base.getX(), l.getY() - fx.base.getY(), l.getZ() - fx.base.getZ());
    }

    static double easeOut(double t) { return 1 - Math.pow(1 - t, 3); }
    static double easeIn(double t) { return t * t * t; }
    static double easeInOut(double t) { return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }
    static double back(double t) { double c1 = 1.70158, c3 = c1 + 1; return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2); }

    static Color col(int rgb) { return Color.fromRGB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255); }

    // ------------------------------------------------------------------ glow planes (text displays with a coloured background)

    /** A flat glowing rectangle. Sizes in blocks. FIXED planes use rotation, CENTER/VERTICAL face the viewer. */
    static final class Glow {
        final Fx fx;
        final TextDisplay d;
        final Display.Billboard bb;
        double w, h;
        int argb;
        Quaternionf rot = new Quaternionf();
        Location pos;
        float zoff;

        Glow(Fx fx, Location pos, double w, double h, int argb, Display.Billboard bb) {
            this.fx = fx;
            this.bb = bb;
            this.pos = pos.clone();
            this.pos.setYaw(0);
            this.pos.setPitch(0);
            this.w = w;
            this.h = h;
            this.argb = argb;
            this.d = fx.world.spawn(this.pos, TextDisplay.class, t -> {
                t.text(net.kyori.adventure.text.Component.text(" "));
                t.setDefaultBackground(false);
                t.setBackgroundColor(Color.fromARGB(argb));
                t.setShadowed(false);
                t.setSeeThrough(false);
                t.setLineWidth(200);
                fx.prepareDisplay(t);
                t.setBillboard(bb);
                t.setTransformation(tf(0.001, 0.001, rot));
            });
            fx.trackEntity(d);
        }

        private Transformation tf(double ww, double hh, Quaternionf r) {
            // " " has a background of 0.125 x 0.25 blocks: centre it on the entity position
            Vector3f sc = new Vector3f((float) (ww / 0.125), (float) (hh / 0.25), 1f);
            Vector3f t = new Quaternionf(r).transform(new Vector3f(0, (float) (-hh / 2), zoff));
            return new Transformation(t, new Quaternionf(r), sc, new Quaternionf());
        }

        Glow size(double w, double h) { this.w = w; this.h = h; return this; }
        Glow color(int argb) { this.argb = argb; return this; }
        Glow rot(Quaternionf q) { this.rot = q; return this; }

        Glow apply(int dur) {
            if (!d.isValid()) return this;
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(dur);
            d.setTransformation(tf(w, h, rot));
            d.setBackgroundColor(Color.fromARGB(argb));
            return this;
        }

        Glow move(Location l, int dur) {
            if (!d.isValid()) return this;
            l = l.clone();
            l.setYaw(0);
            l.setPitch(0);
            d.setTeleportDuration(Math.max(0, Math.min(59, dur)));
            d.teleport(l);
            pos = l;
            return this;
        }

        void remove() { if (d.isValid()) d.remove(); }
    }

    /** A soft round glow facing the viewer: 6 squares turned 15 degrees apart (reads as a circle). */
    static final class Disc {
        final List<Glow> layers = new ArrayList<>();
        Disc(Fx fx, Location at, int argb) { this(fx, at, argb, false); }

        /** flat = lying on the ground instead of facing the viewer. */
        Disc(Fx fx, Location at, int argb, boolean flat) {
            for (int i = 0; i < 6; i++) {
                Glow g = new Glow(fx, at, 0.001, 0.001, argb, flat ? Display.Billboard.FIXED : Display.Billboard.CENTER);
                g.rot = flat ? new Quaternionf().rotateY((float) Math.toRadians(15 * i)).rotateX((float) (-Math.PI / 2))
                        : new Quaternionf().rotateZ((float) Math.toRadians(15 * i));
                g.zoff = 0.004f * i;
                g.apply(0);
                layers.add(g);
            }
        }
        Disc set(double size, int argb, int dur) {
            for (Glow g : layers) g.size(size * 0.92, size * 0.92).color(argb).apply(dur);
            return this;
        }
        Disc move(Location l, int dur) { for (Glow g : layers) g.move(l, dur); return this; }
        void remove() { layers.forEach(Glow::remove); }
    }

    static Disc disc(Fx fx, Location at, int argb) { return new Disc(fx, at, argb); }

    /** A soft light beam from the sky: three planes (wide and faint, medium, thin bright core). */
    static final class Beam {
        final Glow outer, mid, core;
        final int rgb;
        Beam(Fx fx, Location ground, int rgb) {
            this.rgb = rgb & 0xFFFFFF;
            Location c = ground.clone().add(0, 15, 0);
            outer = new Glow(fx, c, 0.001, 30, this.rgb, Display.Billboard.VERTICAL);
            mid = new Glow(fx, c, 0.001, 30, this.rgb, Display.Billboard.VERTICAL);
            core = new Glow(fx, c, 0.001, 30, 0xFFFFFF, Display.Billboard.VERTICAL);
            mid.zoff = 0.01f;
            core.zoff = 0.02f;
            outer.apply(0); mid.apply(0); core.apply(0);
        }
        /** width = full width of the outer glow, k = brightness 0..1 */
        Beam set(double width, double k, int dur) {
            outer.size(Math.max(0.001, width), 30).color(((int) (0x1C * k)) << 24 | rgb).apply(dur);
            mid.size(Math.max(0.001, width * 0.55), 30).color(((int) (0x32 * k)) << 24 | rgb).apply(dur);
            core.size(Math.max(0.001, width * 0.16), 30).color(((int) (0x90 * k)) << 24 | 0xFFFFFF).apply(dur);
            return this;
        }
        void remove() { outer.remove(); mid.remove(); core.remove(); }
    }

    static Glow glow(Fx fx, Location at, double w, double h, int argb, Display.Billboard bb) {
        return new Glow(fx, at, w, h, argb, bb);
    }

    /** Horizontal ring of glowing segments. */
    static final class Ring {
        final List<Glow> segs = new ArrayList<>();
        final Fx fx;
        final Location c;
        double r, thick, spin;
        int argb;
        Quaternionf tilt = new Quaternionf();

        Ring(Fx fx, Location center, double radius, double thick, int n, int argb) {
            this.fx = fx;
            this.c = center.clone();
            this.r = radius;
            this.thick = thick;
            this.argb = argb;
            for (int i = 0; i < n; i++) segs.add(new Glow(fx, center, 0.001, 0.001, argb, Display.Billboard.FIXED));
        }

        Ring apply(int dur) {
            int n = segs.size();
            double len = 2 * Math.PI * r / n * 1.08;
            for (int i = 0; i < n; i++) {
                double a = spin + 2 * Math.PI * i / n;
                Vector3f off = tilt.transform(new Vector3f((float) (Math.cos(a) * r), 0, (float) (Math.sin(a) * r)));
                Location l = c.clone().add(off.x, off.y, off.z);
                Glow g = segs.get(i);
                g.rot = new Quaternionf(tilt).rotateY((float) (-a + Math.PI / 2)).rotateX((float) (-Math.PI / 2));
                g.size(Math.max(0.001, len), Math.max(0.001, thick)).color(argb);
                g.move(l, dur);
                g.apply(dur);
            }
            return this;
        }

        void remove() { segs.forEach(Glow::remove); }
    }

    // ------------------------------------------------------------------ debris with gravity

    /** Blocks that fly off, spin, bounce once and shrink away. */
    static void debris(Fx fx, Location from, Material mat, int n, double power, double up, double size, int life) {
        for (int i = 0; i < n; i++) {
            double sz = size * 0.7 * (0.6 + fx.rnd.nextDouble() * 0.7);
            Location l = from.clone().add(fx.rand(0.3), fx.rnd.nextDouble() * 0.3, fx.rand(0.3));
            Model m = fx.model(l.getX() - fx.base.getX(), l.getY() - fx.base.getY(), l.getZ() - fx.base.getZ());
            m.cube(mat, 0, 0, 0, sz);
            double a = fx.rnd.nextDouble() * Math.PI * 2;
            double sp = power * (0.5 + fx.rnd.nextDouble() * 0.8);
            Vector v = new Vector(Math.cos(a) * sp, up * (0.6 + fx.rnd.nextDouble() * 0.8), Math.sin(a) * sp);
            fly(fx, m, v, life, sz);
        }
    }

    /** Ballistic flight for a model (2-tick steps), lands on the ground (y = base), then shrinks away. */
    static void fly(Fx fx, Model m, Vector v0, int life, double sz) {
        final double[] s = {m.lpos.x, m.lpos.y, m.lpos.z, v0.getX(), v0.getY(), v0.getZ(), fx.rand(40), fx.rand(40), 0, 0};
        final double spinA = fx.rand(30), spinB = fx.rand(30);
        m.show(1);
        for (int k = 1; k <= life / 2; k++) {
            final int step = k;
            fx.later(1 + k * 2, () -> {
                s[0] += s[3] * 2; s[1] += s[4] * 2; s[2] += s[5] * 2;
                s[4] -= 0.08 * 2;
                s[3] *= 0.96; s[5] *= 0.96;
                if (s[1] < sz / 2 && s[4] < 0) {
                    s[1] = sz / 2;
                    s[4] = -s[4] * 0.3;
                    s[3] *= 0.5; s[5] *= 0.5;
                    s[9]++;
                }
                if (s[9] < 2) { s[6] += spinA; s[7] += spinB; }
                m.at(s[0], s[1], s[2]).rot(s[6], s[7], 0);
                if (step * 2 > life - 8) m.size(Math.max(0.001, (life - step * 2) / 8.0));
                m.apply(2);
            });
        }
        fx.later(life + 4, m::remove);
    }

    // ------------------------------------------------------------------ ground shock wave

    /** Ground blocks pop up in a ring that travels outwards, like a mace smash. */
    static void wave(Fx fx, double r0, double r1, int ticks, int rings, double height) {
        Material g = fx.groundBlock();
        for (int ri = 0; ri < rings; ri++) {
            double r = r0 + (r1 - r0) * ri / Math.max(1, rings - 1);
            int n = (int) Math.max(8, Math.round(2 * Math.PI * r / 0.9));
            int when = (int) Math.round((double) ticks * ri / Math.max(1, rings - 1));
            double hh = height * (1 - 0.55 * ri / Math.max(1, rings));
            for (int i = 0; i < n; i++) {
                double a = 2 * Math.PI * (i + (ri % 2) * 0.5) / n;
                double x = Math.cos(a) * r, z = Math.sin(a) * r;
                Model m = fx.model(x, -1.02, z);
                m.rot(Math.toDegrees(-a), fx.rand(8), fx.rand(8));
                m.boxC(g, 0, 0.5, 0, 0.92, 1.0, 0.92);
                m.show(0);
                final int at = when;
                fx.later(at + 2, () -> {
                    m.at(x, -1.02 + hh, z).rot(Math.toDegrees(-a), fx.rand(14), fx.rand(14)).apply(2);
                    fx.block(fx.base.clone().add(x, 0.1, z), g, 4, 0.25, 0.1);
                });
                fx.later(at + 5, () -> m.at(x, -1.02 + hh * 0.75, z).apply(3));
                fx.later(at + 9, () -> m.at(x, -1.05, z).rot(Math.toDegrees(-a), 0, 0).apply(5));
                fx.later(at + 16, m::remove);
            }
        }
    }

    // ------------------------------------------------------------------ fireworks and mobs

    /** A real firework explosion (no rocket flight). */
    static void firework(Fx fx, Location at, FireworkEffect.Type type, boolean trail, boolean flicker, int... colors) {
        if (fx.world == null) return;
        Color[] cs = new Color[colors.length];
        for (int i = 0; i < colors.length; i++) cs[i] = col(colors[i]);
        Firework fw = fx.world.spawn(at, Firework.class, f -> {
            FireworkMeta meta = f.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(type).withColor(cs).withFade(Color.WHITE).trail(trail).flicker(flicker).build());
            meta.setPower(0);
            f.setFireworkMeta(meta);
            f.setSilent(false);
            f.addScoreboardTag("killeffects");
            if (fx.viewer != null) f.setVisibleByDefault(false);
        });
        fx.trackEntity(fw);
        if (Trace.ON) Trace.marker(fx, "FIREWORK_" + type.name(), at, colors.length > 0 ? colors[0] : 0xFFFFFF);
        fx.later(1, () -> { if (fw.isValid()) fw.detonate(); });
    }

    /** A real mob as a prop: no AI, silent, can't be hurt, scaled. */
    static <T extends LivingEntity> T mob(Fx fx, Class<T> type, Location at, double scale) {
        T e = fx.world.spawn(at, type, false, m -> {
            m.setPersistent(false);
            m.setInvulnerable(true);
            m.setSilent(true);
            m.setGravity(false);
            m.setCollidable(false);
            m.setAI(false);
            m.setRemoveWhenFarAway(false);
            if (m instanceof Mob mob) mob.setAware(false);
            AttributeInstance a = m.getAttribute(Attribute.SCALE);
            if (a != null) a.setBaseValue(scale);
            if (fx.viewer != null) m.setVisibleByDefault(false);
        });
        fx.trackEntity(e);
        return e;
    }

    static void move(Entity e, Location l) {
        if (e.isValid()) e.teleport(l);
    }

    // ------------------------------------------------------------------ particles

    /** Coloured streaks that fly from `from` to `to` (TRAIL particle). */
    static void streak(Fx fx, Location from, Location to, int rgb, int ticks) {
        fx.particle(Particle.TRAIL, from, 1, 0, 0, 0, 0, new Particle.Trail(to, col(rgb), ticks));
    }

    /** n streaks from a sphere around c into c. */
    static void converge(Fx fx, Location c, double r, int n, int rgb, int ticks) {
        for (int i = 0; i < n; i++) {
            Vector v = new Vector(fx.rand(1), fx.rand(1), fx.rand(1)).normalize().multiply(r);
            streak(fx, c.clone().add(v), c, rgb, ticks);
        }
    }

    /** n streaks bursting out from c. */
    static void burst(Fx fx, Location c, double r, int n, int rgb, int ticks) {
        for (int i = 0; i < n; i++) {
            Vector v = new Vector(fx.rand(1), fx.rand(1) * 0.7 + 0.2, fx.rand(1)).normalize().multiply(r * (0.6 + fx.rnd.nextDouble() * 0.6));
            streak(fx, c, c.clone().add(v), rgb, ticks);
        }
    }

    /** Falling coloured confetti (falling dust of concrete powder). */
    static void confetti(Fx fx, Location c, int n, double spread) {
        Material[] m = {Material.LIME_CONCRETE_POWDER, Material.LIGHT_BLUE_CONCRETE_POWDER, Material.YELLOW_CONCRETE_POWDER,
                Material.MAGENTA_CONCRETE_POWDER, Material.PURPLE_CONCRETE_POWDER, Material.ORANGE_CONCRETE_POWDER, Material.RED_CONCRETE_POWDER};
        for (int i = 0; i < n; i++) {
            fx.particle(Particle.FALLING_DUST, c.clone().add(fx.rand(spread), fx.rand(spread * 0.4), fx.rand(spread)), 1, 0, 0, 0, 0,
                    m[fx.rnd.nextInt(m.length)].createBlockData());
        }
    }
}
