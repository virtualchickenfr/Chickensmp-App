package dev.killeffects;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A model built from block displays (and a few item displays) that moves, rotates and scales as one piece.
 * All coordinates are in blocks, relative to the effect location (the victim's feet).
 * Models can have children (a rotor on a helicopter); a child follows its parent.
 */
final class Model {
    private static final float TINY = 0.001f;

    final Fx fx;
    final Model parent;
    final List<Model> kids = new ArrayList<>();
    final List<Part> parts = new ArrayList<>();
    final Vector3f lpos;
    Quaternionf lrot = new Quaternionf();
    float scale = 1f;
    private boolean shown;

    static final class Part {
        final Display d;
        final Vector3f off;
        final Vector3f size;
        final boolean centered;
        boolean free;

        Part(Display d, Vector3f off, Vector3f size, boolean centered) {
            this.d = d;
            this.off = off;
            this.size = size;
            this.centered = centered;
        }
    }

    Model(Fx fx, double x, double y, double z) {
        this(fx, null, x, y, z);
    }

    private Model(Fx fx, Model parent, double x, double y, double z) {
        this.fx = fx;
        this.parent = parent;
        this.lpos = new Vector3f((float) x, (float) y, (float) z);
    }

    /** A child model whose pivot sits at (x,y,z) in this model's local space. */
    Model child(double x, double y, double z) {
        Model m = new Model(fx, this, x, y, z);
        kids.add(m);
        return m;
    }

    // ------------------------------------------------------------------ building

    /** Box with its lower corner at (x,y,z) and size (sx,sy,sz). mat = Material or block data string. */
    Model box(Object mat, double x, double y, double z, double sx, double sy, double sz) {
        if (fx.world == null) return this;
        BlockData data = mat instanceof Material m ? m.createBlockData() : Bukkit.createBlockData(String.valueOf(mat));
        BlockDisplay bd = fx.world.spawn(spawnLoc(), BlockDisplay.class, e -> {
            e.setBlock(data);
            fx.prepareDisplay(e);
            e.setTransformation(tiny());
        });
        fx.trackEntity(bd);
        parts.add(new Part(bd, new Vector3f((float) x, (float) y, (float) z), new Vector3f((float) sx, (float) sy, (float) sz), false));
        return this;
    }

    /** Box centred on (cx,cy,cz). */
    Model boxC(Object mat, double cx, double cy, double cz, double sx, double sy, double sz) {
        return box(mat, cx - sx / 2, cy - sy / 2, cz - sz / 2, sx, sy, sz);
    }

    /** Cube of size s centred on (cx,cy,cz). */
    Model cube(Object mat, double cx, double cy, double cz, double s) {
        return boxC(mat, cx, cy, cz, s, s, s);
    }

    /** An item (rendered flat or as a block item) centred on (cx,cy,cz). */
    Model item(ItemStack stack, double cx, double cy, double cz, double size) {
        if (fx.world == null) return this;
        ItemDisplay id = fx.world.spawn(spawnLoc(), ItemDisplay.class, e -> {
            e.setItemStack(stack);
            e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            fx.prepareDisplay(e);
            e.setTransformation(tiny());
        });
        fx.trackEntity(id);
        parts.add(new Part(id, new Vector3f((float) cx, (float) cy, (float) cz), new Vector3f((float) size, (float) size, (float) size), true));
        return this;
    }

    Model item(Material m, double cx, double cy, double cz, double size) {
        return item(new ItemStack(m), cx, cy, cz, size);
    }

    /** The victim's head (or the viewer's in a preview), centred on (cx,cy,cz). size 1 = one block. */
    Model head(double cx, double cy, double cz, double size) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        Player who = fx.victim != null ? fx.victim : fx.viewer;
        if (who != null && skull.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(who);
            skull.setItemMeta(meta);
        }
        // a player head item rendered FIXED is half a block big
        return item(skull, cx, cy, cz, size * 2.0);
    }

    /** Text made of blocks. Rows top to bottom, '#' = block. One pixel = px blocks, lower left at (x,y,z) facing +z. */
    Model pixels(Object mat, String[] rows, double x, double y, double z, double px, double depth) {
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int c = 0;
            while (c < row.length()) {
                if (row.charAt(c) != '#') { c++; continue; }
                int start = c;
                while (c < row.length() && row.charAt(c) == '#') c++;
                double yy = y + (rows.length - 1 - r) * px;
                box(mat, x + start * px, yy, z, (c - start) * px, px, depth);
            }
        }
        return this;
    }

    private Location spawnLoc() {
        Location l = fx.base.clone();
        l.setYaw(0f);
        l.setPitch(0f);
        return l;
    }

    private static Transformation tiny() {
        return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(TINY, TINY, TINY), new Quaternionf());
    }

    // ------------------------------------------------------------------ transform

    Model at(double x, double y, double z) {
        lpos.set((float) x, (float) y, (float) z);
        return this;
    }

    Model by(double x, double y, double z) {
        lpos.add((float) x, (float) y, (float) z);
        return this;
    }

    /** Absolute rotation in degrees: yaw around Y, then pitch around X, then roll around Z. */
    Model rot(double yaw, double pitch, double roll) {
        lrot = new Quaternionf().rotateY((float) Math.toRadians(yaw)).rotateX((float) Math.toRadians(pitch)).rotateZ((float) Math.toRadians(roll));
        return this;
    }

    Model size(double s) {
        scale = (float) Math.max(TINY, s);
        return this;
    }

    Vector3f effPos() {
        if (parent == null) return new Vector3f(lpos);
        Vector3f p = new Vector3f(lpos).mul(parent.effScale());
        parent.effRot().transform(p);
        return p.add(parent.effPos());
    }

    Quaternionf effRot() {
        return parent == null ? new Quaternionf(lrot) : parent.effRot().mul(lrot);
    }

    float effScale() {
        return parent == null ? scale : parent.effScale() * scale;
    }

    /** Send the current state to the clients, interpolated over dur ticks. Children follow. */
    Model apply(int dur) {
        Vector3f pos = effPos();
        Quaternionf rot = effRot();
        float s = effScale();
        for (Part p : parts) {
            if (p.free || p.d == null || !p.d.isValid()) continue;
            Vector3f t = new Vector3f(p.off).mul(s);
            rot.transform(t);
            t.add(pos);
            Vector3f sc = new Vector3f(p.size).mul(s);
            sc.set(Math.max(TINY, sc.x), Math.max(TINY, sc.y), Math.max(TINY, sc.z));
            push(p.d, t, rot, sc, dur);
        }
        for (Model k : kids) k.apply(dur);
        return this;
    }

    private static void push(Display d, Vector3f t, Quaternionf r, Vector3f s, int dur) {
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(Math.max(0, dur));
        d.setTransformation(new Transformation(t, new Quaternionf(r), s, new Quaternionf()));
    }

    /** Grow from nothing into the current state (call once after building). */
    Model show(int dur) {
        if (shown) return this;
        shown = true;
        fx.later(2, () -> apply(dur));
        for (Model k : kids) k.shown = true;
        return this;
    }

    /** Everything shrinks to nothing, then the entities are removed. */
    void vanish(int delay, int dur) {
        fx.later(delay, () -> {
            float old = scale;
            scale = TINY;
            apply(dur);
            scale = old;
            fx.later(dur + 1, this::remove);
        });
    }

    /** Every block flies away on its own, spinning and shrinking. */
    void shatter(int delay, int dur, double power, double up) {
        fx.later(delay, () -> {
            Vector3f center = effPos();
            shatterParts(center, dur, power, up);
            fx.later(dur + 1, this::remove);
        });
    }

    private void shatterParts(Vector3f center, int dur, double power, double up) {
        Vector3f pos = effPos();
        Quaternionf rot = effRot();
        float s = effScale();
        for (Part p : parts) {
            if (p.d == null || !p.d.isValid()) continue;
            p.free = true;
            Vector3f t = new Vector3f(p.off).mul(s);
            rot.transform(t);
            t.add(pos);
            Vector3f sc = new Vector3f(p.size).mul(s);
            Vector3f mid = new Vector3f(t).add(p.centered ? new Vector3f() : rot.transform(new Vector3f(sc).mul(0.5f)));
            Vector3f dir = new Vector3f(mid).sub(center);
            if (dir.lengthSquared() < 0.0001f) dir.set((float) fx.rand(1), 0.5f, (float) fx.rand(1));
            dir.normalize().add((float) fx.rand(0.5), (float) (up + fx.rnd.nextDouble() * 0.5), (float) fx.rand(0.5));
            dir.normalize().mul((float) (power * (0.6 + fx.rnd.nextDouble() * 0.7)));
            Quaternionf spin = new Quaternionf().rotateXYZ((float) fx.rand(3), (float) fx.rand(3), (float) fx.rand(3)).mul(rot);
            Vector3f end = new Vector3f(t).add(dir);
            push(p.d, end, spin, new Vector3f(TINY, TINY, TINY), dur);
        }
        for (Model k : kids) k.shatterParts(center, dur, power, up);
    }

    void remove() {
        for (Part p : parts) {
            if (p.d != null && p.d.isValid()) p.d.remove();
        }
        for (Model k : kids) k.remove();
    }

    /** Location of a point in this model's local space (for particles). */
    Location where(double x, double y, double z) {
        Vector3f v = new Vector3f((float) x, (float) y, (float) z).mul(effScale());
        effRot().transform(v);
        v.add(effPos());
        return fx.base.clone().add(v.x, v.y, v.z);
    }
}
