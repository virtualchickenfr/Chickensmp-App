package dev.killeffects;

import org.bukkit.Material;

/**
 * A posable copy of a player: the real head (player head item) plus torso, arms and legs built from
 * blocks in the colours of their skin. Every limb is a child Model with its pivot at the joint.
 * Local space: feet at (0,0,0), facing +z. Player proportions (1 skin pixel = PX blocks).
 */
final class Rig {
    static final double PX = 0.9 / 16.0;

    final Fx fx;
    final Model root;
    final Model body;
    final Model head;
    final Model rarm;
    final Model larm;
    final Model rleg;
    final Model lleg;
    final Skin skin;

    Rig(Fx fx, double x, double y, double z) {
        this.fx = fx;
        this.skin = Skin.of(fx.victim != null ? fx.victim : fx.viewer);
        double p = PX;
        double aw = skin.slim ? 3 : 4;
        root = fx.model(x, y, z);
        rleg = root.child(-2 * p, 12 * p, 0);
        lleg = root.child(2 * p, 12 * p, 0);
        limb(rleg, Skin.Part.RLEG, 4);
        limb(lleg, Skin.Part.LLEG, 4);
        body = root.child(0, 12 * p, 0);
        for (int band = 0; band < 4; band++) {
            body.box(skin.get(Skin.Part.TORSO, band), -4 * p, (12 - (band + 1) * 3) * p, -2 * p, 8 * p, 3 * p, 4 * p);
        }
        head = body.child(0, 12 * p, 0);
        head.head(0, 4 * p, 0, 8 * p);
        rarm = body.child(-(4 + aw / 2) * p, 12 * p, 0);
        larm = body.child((4 + aw / 2) * p, 12 * p, 0);
        limb(rarm, Skin.Part.RARM, aw);
        limb(larm, Skin.Part.LARM, aw);
        root.rot(fx.face(), 0, 0);
    }

    /** A limb hanging down from its joint, built from 4 colour bands. */
    private void limb(Model m, Skin.Part part, double w) {
        double p = PX;
        for (int band = 0; band < 4; band++) {
            m.box(skin.get(part, band), -w / 2 * p, -(band + 1) * 3 * p, -2 * p, w * p, 3 * p, 4 * p);
        }
    }

    /** Rotate the whole rig around its point (0,h,0) and put that point at (cx,cy,cz). Degrees. */
    Rig place(double cx, double cy, double cz, double yaw, double pitch, double roll, double h) {
        org.joml.Quaternionf q = new org.joml.Quaternionf().rotateY((float) Math.toRadians(yaw)).rotateX((float) Math.toRadians(pitch)).rotateZ((float) Math.toRadians(roll));
        org.joml.Vector3f v = q.transform(new org.joml.Vector3f(0, (float) h, 0));
        root.at(cx - v.x, cy - v.y, cz - v.z).rot(yaw, pitch, roll);
        return this;
    }

    Model[] limbs() {
        return new Model[]{head, rarm, larm, rleg, lleg, body};
    }

    /** Arms straight out to the sides. */
    Rig tpose() {
        rarm.rot(0, 0, -90);
        larm.rot(0, 0, 90);
        return this;
    }

    /** Arms up in the air. */
    Rig armsUp() {
        rarm.rot(0, 0, -160);
        larm.rot(0, 0, 160);
        return this;
    }

    /** Back to standing. */
    Rig stand() {
        for (Model m : limbs()) m.rot(0, 0, 0);
        return this;
    }

    /** Walking / running swing, phase in radians. */
    Rig walk(double phase, double amount) {
        double a = Math.sin(phase) * amount;
        rarm.rot(0, a, 0);
        larm.rot(0, -a, 0);
        rleg.rot(0, -a, 0);
        lleg.rot(0, a, 0);
        return this;
    }

    Rig show(int dur) {
        root.size(0.01).show(0);
        fx.later(2, () -> root.size(1).apply(dur));
        return this;
    }

    Rig apply(int dur) {
        root.apply(dur);
        return this;
    }

    /** Swap every block of the rig (e.g. to ash). */
    void recolor(Material m) {
        recolor(root, m);
    }

    private static void recolor(Model model, Material m) {
        for (Model.Part p : model.parts) {
            if (p.d instanceof org.bukkit.entity.BlockDisplay bd && bd.isValid()) bd.setBlock(m.createBlockData());
        }
        for (Model k : model.kids) recolor(k, m);
    }
}
