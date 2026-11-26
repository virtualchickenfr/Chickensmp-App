package dev.killeffects;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

/**
 * Test-only recorder (-Dke.trace=<dir>): writes every tick of an effect (entities + particles) as JSON lines,
 * so it can be rendered to pictures offline. Does nothing on the live server.
 */
final class Trace {
    static final String DIR = System.getProperty("ke.trace");
    static final boolean ON = DIR != null;
    static String name = "effect";
    private static Fx current;
    private static int startTick;
    private static final List<String> lines = new ArrayList<>();

    private Trace() {}

    static void start(Fx fx) {
        current = fx;
        startTick = Bukkit.getCurrentTick();
        lines.clear();
        String file = DIR + "/" + name + ".jsonl";
        for (int t = 0; t <= Fx.MAX_TICKS; t++) {
            final int tick = t;
            Bukkit.getScheduler().runTaskLater(fx.plugin, () -> frame(fx, tick, file), t);
        }
    }

    private static String v(double... d) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < d.length; i++) { if (i > 0) sb.append(','); sb.append(String.format(java.util.Locale.US, "%.4f", d[i])); }
        return sb.append(']').toString();
    }

    private static String q(String s) { return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + '"'; }

    private static void frame(Fx fx, int tick, String file) {
        if (current != fx) return;
        StringBuilder sb = new StringBuilder("{\"tick\":" + tick + ",\"face\":" + fx.face() + ",\"e\":[");
        boolean first = true;
        Location b = fx.base;
        for (Entity e : fx.spawned) {
            if (!e.isValid()) continue;
            Location l = e.getLocation();
            StringBuilder o = new StringBuilder("{\"id\":" + e.getEntityId() + ",");
            o.append("\"p\":").append(v(l.getX() - b.getX(), l.getY() - b.getY(), l.getZ() - b.getZ())).append(",\"yaw\":").append(l.getYaw()).append(",\"pitch\":").append(l.getPitch());
            if (e instanceof Display d) {
                Transformation tr = d.getTransformation();
                o.append(",\"tr\":").append(v(tr.getTranslation().x, tr.getTranslation().y, tr.getTranslation().z));
                o.append(",\"lr\":").append(v(tr.getLeftRotation().x, tr.getLeftRotation().y, tr.getLeftRotation().z, tr.getLeftRotation().w));
                o.append(",\"s\":").append(v(tr.getScale().x, tr.getScale().y, tr.getScale().z));
                o.append(",\"rr\":").append(v(tr.getRightRotation().x, tr.getRightRotation().y, tr.getRightRotation().z, tr.getRightRotation().w));
                o.append(",\"bb\":").append(q(d.getBillboard().name()));
                o.append(",\"idur\":").append(d.getInterpolationDuration()).append(",\"idel\":").append(d.getInterpolationDelay()).append(",\"tdur\":").append(d.getTeleportDuration());
                if (d.isGlowing()) o.append(",\"glow\":").append(d.getGlowColorOverride() == null ? -1 : d.getGlowColorOverride().asRGB());
            }
            if (e instanceof BlockDisplay bd) {
                BlockData bdata = bd.getBlock();
                o.append(",\"t\":\"b\",\"m\":").append(q(bdata.getMaterial().getKey().getKey()));
            } else if (e instanceof ItemDisplay id) {
                ItemStack it = id.getItemStack();
                String model = it != null && it.hasItemMeta() && it.getItemMeta().hasItemModel() ? it.getItemMeta().getItemModel().toString() : "";
                o.append(",\"t\":\"i\",\"m\":").append(q(it == null ? "air" : it.getType().getKey().getKey())).append(",\"model\":").append(q(model))
                        .append(",\"mode\":").append(q(id.getItemDisplayTransform().name()));
            } else if (e instanceof TextDisplay td) {
                Color bg = td.getBackgroundColor();
                String text = td.text() == null ? "" : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(td.text());
                o.append(",\"t\":\"t\",\"text\":").append(q(text)).append(",\"bg\":").append(bg == null ? 0 : bg.asARGB())
                        .append(",\"op\":").append(td.getTextOpacity() & 0xFF);
            } else if (e instanceof org.bukkit.entity.Mannequin le) {
                double sc = 1;
                try { sc = le.getAttribute(Attribute.SCALE).getValue(); } catch (Exception ignored) { }
                o.append(",\"t\":\"m\",\"pose\":").append(q(e.getPose().name())).append(",\"scale\":").append(sc)
                        .append(",\"body\":").append(le.getBodyYaw()).append(",\"inv\":").append(le.isInvisible());
            } else {
                double sc = 1;
                if (e instanceof LivingEntity le2) try { sc = le2.getAttribute(Attribute.SCALE).getValue(); } catch (Exception ignored) { }
                o.append(",\"t\":").append(q(e.getType().name())).append(",\"scale\":").append(sc);
            }
            o.append('}');
            if (!first) sb.append(',');
            sb.append(o);
            first = false;
        }
        sb.append("],\"pt\":[");
        sb.append(String.join(",", pending));
        pending.clear();
        sb.append("]}");
        try (FileWriter w = new FileWriter(file, tick > 0)) { w.write(sb.append('\n').toString()); } catch (IOException ignored) { }
    }

    private static final List<String> pending = new ArrayList<>();

    static void particle(Fx fx, Particle p, Location l, int n, double dx, double dy, double dz, double speed, Object data) {
        if (current != fx) return;
        Location b = fx.base;
        String extra = "";
        if (data instanceof Particle.DustOptions o) extra = ",\"c\":" + o.getColor().asRGB() + ",\"sz\":" + o.getSize();
        else if (data instanceof Particle.DustTransition o) extra = ",\"c\":" + o.getColor().asRGB() + ",\"c2\":" + o.getToColor().asRGB() + ",\"sz\":" + o.getSize();
        else if (data instanceof Color c) extra = ",\"c\":" + c.asRGB();
        else if (data instanceof BlockData bd) extra = ",\"m\":" + q(bd.getMaterial().getKey().getKey());
        else if (data instanceof ItemStack it) extra = ",\"m\":" + q(it.getType().getKey().getKey());
        else if (data instanceof Particle.Trail tr) extra = ",\"c\":" + tr.getColor().asRGB() + ",\"to\":" + v(tr.getTarget().getX() - b.getX(), tr.getTarget().getY() - b.getY(), tr.getTarget().getZ() - b.getZ()) + ",\"dur\":" + tr.getDuration();
        pending.add("{\"k\":" + q(p.name()) + ",\"p\":" + v(l.getX() - b.getX(), l.getY() - b.getY(), l.getZ() - b.getZ()) + ",\"n\":" + n
                + ",\"d\":" + v(dx, dy, dz) + ",\"sp\":" + String.format(java.util.Locale.US, "%.4f", speed) + extra + "}");
    }

    static void marker(Fx fx, String kind, Location l, int rgb) {
        if (current != fx) return;
        Location b = fx.base;
        pending.add("{\"k\":" + q(kind) + ",\"p\":" + v(l.getX() - b.getX(), l.getY() - b.getY(), l.getZ() - b.getZ()) + ",\"n\":1,\"d\":[0,0,0],\"sp\":0,\"c\":" + rgb + "}");
    }
}
