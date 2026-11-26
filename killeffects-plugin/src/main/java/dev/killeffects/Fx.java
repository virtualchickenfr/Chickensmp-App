/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Particle$DustTransition
 *  org.bukkit.World
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.BlockDisplay
 *  org.bukkit.entity.Display
 *  org.bukkit.entity.Display$Billboard
 *  org.bukkit.entity.Display$Brightness
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.entity.ItemDisplay$ItemDisplayTransform
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.util.Transformation
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 */
package dev.killeffects;

import dev.killeffects.KillEffects;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class Fx {
    static final int MAX_TICKS = 200;
    final KillEffects plugin;
    final World world;
    final Location base;
    final Player viewer;
    final boolean sprites;
    Player victim;
    Location faceTarget;

    /** Yaw (degrees) that turns a model's front (+z) towards the killer / viewer. */
    double face() {
        Location t = this.faceTarget != null ? this.faceTarget : (this.viewer != null ? this.viewer.getLocation() : null);
        if (t == null || t.getWorld() != this.world) return 0.0;
        double dx = t.getX() - this.base.getX();
        double dz = t.getZ() - this.base.getZ();
        if (dx * dx + dz * dz < 0.01) return 0.0;
        return Math.toDegrees(Math.atan2(dx, dz));
    }

    /** The block the victim stood on (for dirt particles and debris). */
    Material groundBlock() {
        if (this.world == null) return Material.DIRT;
        Material m = this.base.clone().add(0.0, -0.5, 0.0).getBlock().getType();
        return m.isSolid() && m.isBlock() && !m.isAir() ? m : Material.DIRT;
    }
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final List<Entity> spawned = new ArrayList<Entity>();

    Fx(KillEffects killEffects, Location location, Player player, boolean bl) {
        this.plugin = killEffects;
        this.world = location.getWorld();
        this.base = location.clone();
        this.viewer = player;
        this.sprites = bl;
        this.later(MAX_TICKS, this::cleanup);
        if (Trace.ON) Trace.start(this);
    }

    Location loc(double d, double d2, double d3) {
        return this.base.clone().add(d, d2, d3);
    }

    Location body() {
        return this.loc(0.0, 1.0, 0.0);
    }

    /** Tick (relative to the start) of the last scheduled step, i.e. how long the effect runs. */
    int endTick;
    private final int startTick = Bukkit.getCurrentTick();

    void later(int n, Runnable runnable) {
        if (n < MAX_TICKS) this.endTick = Math.max(this.endTick, Bukkit.getCurrentTick() - this.startTick + n);
        if (n <= 0) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, runnable, (long)n);
        }
    }

    void repeat(int n, int n2, IntConsumer intConsumer) {
        for (int i = 0; i < n; ++i) {
            int n3 = i;
            this.later(i * n2, () -> intConsumer.accept(n3));
        }
    }

    double rand(double d) {
        return (this.rnd.nextDouble() * 2.0 - 1.0) * d;
    }

    <T> void particle(Particle particle, Location location, int n, double d, double d2, double d3, double d4, T t) {
        if (Trace.ON) Trace.particle(this, particle, location, n, d, d2, d3, d4, t);
        if (this.viewer != null) {
            if (this.viewer.isOnline() && this.viewer.getWorld() == this.world) {
                this.viewer.spawnParticle(particle, location, n, d, d2, d3, d4, t);
            }
        } else {
            this.world.spawnParticle(particle, location, n, d, d2, d3, d4, t);
        }
    }

    void p(Particle particle, Location location, int n, double d, double d2) {
        this.particle(particle, location, n, d, d, d, d2, null);
    }

    void p(Particle particle, Location location, int n, double d, double d2, double d3, double d4) {
        this.particle(particle, location, n, d, d2, d3, d4, null);
    }

    void fly(Particle particle, Location location, double d, double d2, double d3, double d4) {
        this.particle(particle, location, 0, d, d2, d3, d4, null);
    }

    void dust(Location location, int n, float f, int n2, double d) {
        this.dust(location, n, f, n2, d, d, d);
    }

    void dust(Location location, int n, float f, int n2, double d, double d2, double d3) {
        this.particle(Particle.DUST, location, n2, d, d2, d3, 0.0, new Particle.DustOptions(Fx.color(n), f));
    }

    void fade(Location location, int n, int n2, float f, int n3, double d) {
        this.particle(Particle.DUST_COLOR_TRANSITION, location, n3, d, d, d, 0.0, new Particle.DustTransition(Fx.color(n), Fx.color(n2), f));
    }

    void block(Location location, Material material, int n, double d, double d2) {
        this.particle(Particle.BLOCK, location, n, d, d, d, d2, material.createBlockData());
    }

    void item(Location location, Material material, int n, double d, double d2) {
        this.particle(Particle.ITEM, location, n, d, d, d, d2, new ItemStack(material));
    }

    void flash(int n) {
        this.particle(Particle.FLASH, this.body(), 1, 0.0, 0.0, 0.0, 0.0, Fx.color(n));
    }

    static Color color(int n) {
        return Color.fromRGB((int)(n >> 16 & 0xFF), (int)(n >> 8 & 0xFF), (int)(n & 0xFF));
    }

    void ring(Location location, double d, int n, double d2, Consumer<Location> consumer) {
        for (int i = 0; i < n; ++i) {
            double d3 = d2 + Math.PI * 2 * (double)i / (double)n;
            consumer.accept(location.clone().add(Math.cos(d3) * d, 0.0, Math.sin(d3) * d));
        }
    }

    void dustRing(double d, double d2, int n, int n2, float f) {
        this.ring(this.loc(0.0, d, 0.0), d2, n, 0.0, location -> this.dust((Location)location, n2, f, 1, 0.0));
    }

    void expandingRing(double d, double d2, double d3, int n, int n2, Consumer<Location> consumer) {
        this.repeat(n, 1, n3 -> {
            double d4 = d2 + (d3 - d2) * (double)(n3 + 1) / (double)n;
            this.ring(this.loc(0.0, d, 0.0), d4, n2, (double)n3 * 0.13, consumer);
        });
    }

    void helix(double d, double d2, double d3, int n, int n2, Consumer<Location> consumer) {
        this.repeat(n, 1, n3 -> {
            double d4 = (double)(n3 + 1) / (double)n;
            for (int i = 0; i < n2; ++i) {
                for (int j = 0; j < 2; ++j) {
                    double d5 = d4 - (double)j * 0.5 / (double)n;
                    double d6 = d5 * d3 * Math.PI * 2.0 + Math.PI * 2 * (double)i / (double)n2;
                    consumer.accept(this.loc(Math.cos(d6) * d2, d5 * d, Math.sin(d6) * d2));
                }
            }
        });
    }

    void sphere(Location location, double d, int n, Consumer<Location> consumer) {
        double d2 = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < n; ++i) {
            double d3 = 1.0 - (double)i / (double)(n - 1) * 2.0;
            double d4 = Math.sqrt(1.0 - d3 * d3);
            double d5 = d2 * (double)i;
            consumer.accept(location.clone().add(Math.cos(d5) * d4 * d, d3 * d, Math.sin(d5) * d4 * d));
        }
    }

    void explode(Location location, Particle particle, int n, double d) {
        for (int i = 0; i < n; ++i) {
            double d2;
            double d3;
            double d4 = this.rand(1.0);
            double d5 = Math.sqrt(d4 * d4 + (d3 = this.rand(1.0)) * d3 + (d2 = this.rand(1.0)) * d2);
            if (d5 < 0.01) continue;
            this.fly(particle, location, d4 / d5, d3 / d5, d2 / d5, d * (0.5 + this.rnd.nextDouble() * 0.5));
        }
    }

    void implode(Location location, Particle particle, int n, double d, double d2) {
        for (int i = 0; i < n; ++i) {
            double d3;
            double d4;
            double d5 = this.rand(1.0);
            double d6 = Math.sqrt(d5 * d5 + (d4 = this.rand(1.0)) * d4 + (d3 = this.rand(1.0)) * d3);
            if (d6 < 0.01) continue;
            Location location2 = location.clone().add(d5 / d6 * d, d4 / d6 * d, d3 / d6 * d);
            this.fly(particle, location2, -d5 / d6, -d4 / d6, -d3 / d6, d2);
        }
    }

    void line(Location location, Location location2, double d, Consumer<Location> consumer) {
        double d2 = location2.getX() - location.getX();
        double d3 = location2.getY() - location.getY();
        double d4 = location2.getZ() - location.getZ();
        double d5 = Math.sqrt(d2 * d2 + d3 * d3 + d4 * d4);
        int n = Math.max(1, (int)(d5 / d));
        for (int i = 0; i <= n; ++i) {
            consumer.accept(location.clone().add(d2 * (double)i / (double)n, d3 * (double)i / (double)n, d4 * (double)i / (double)n));
        }
    }

    void sound(String string, float f, float f2) {
        if (this.viewer != null) {
            if (this.viewer.isOnline()) {
                this.viewer.playSound(this.base, string, f, f2);
            }
        } else {
            this.world.playSound(this.base, string, f, f2);
        }
    }

    void sound(int n, String string, float f, float f2) {
        this.later(n, () -> this.sound(string, f, f2));
    }

    void lightning(Location location) {
        if (this.viewer == null) {
            this.world.strikeLightningEffect(location);
        } else {
            this.particle(Particle.FLASH, location.clone().add(0.0, 1.0, 0.0), 1, 0.0, 0.0, 0.0, 0.0, Fx.color(0xFFFFFF));
            for (int i = 0; i < 12; ++i) {
                this.p(Particle.ELECTRIC_SPARK, location.clone().add(0.0, (double)i * 0.5, 0.0), 4, 0.15, 0.02);
            }
            this.sound("minecraft:entity.lightning_bolt.thunder", 0.5f, 1.2f);
        }
    }

    Sprite sprite(String string, double d, double d2, double d3, float f) {
        return this.sprite(string, d, d2, d3, f, Display.Billboard.CENTER, new Quaternionf());
    }

    Sprite flat(String string, double d, float f) {
        return this.sprite(string, 0.0, d, 0.0, f, Display.Billboard.FIXED, new Quaternionf().rotateX(-1.5707964f));
    }

    Sprite sprite(String string, double d, double d2, double d3, float f, Display.Billboard billboard, Quaternionf quaternionf) {
        if (!this.sprites || this.world == null) {
            return new Sprite(this, null, new Vector3f((float)d, (float)d2, (float)d3), quaternionf, f * 2.0f);
        }
        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setItemModel(new NamespacedKey("killfx", "sprite/" + string));
            itemStack.setItemMeta(itemMeta);
        }
        boolean bl = billboard != Display.Billboard.FIXED;
        Location location = bl ? this.base.clone().add(d, d2, d3) : this.base.clone();
        location.setYaw(0.0f);
        location.setPitch(0.0f);
        ItemDisplay itemDisplay2 = (ItemDisplay)this.world.spawn(location, ItemDisplay.class, itemDisplay -> {
            itemDisplay.setItemStack(itemStack);
            itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            this.prepare((Display)itemDisplay, billboard);
            itemDisplay.setTransformation(new Transformation(bl ? new Vector3f() : new Vector3f((float)d, (float)d2, (float)d3), quaternionf, new Vector3f(0.001f, 0.001f, 0.001f), new Quaternionf()));
        });
        this.track((Entity)itemDisplay2);
        Sprite sprite = new Sprite(this, (Display)itemDisplay2, new Vector3f((float)d, (float)d2, (float)d3), quaternionf, f * 2.0f);
        sprite.billboarded = bl;
        this.later(2, () -> sprite.apply(4));
        return sprite;
    }

    Sprite blockDisplay(Material material, double d, double d2, double d3, float f, float f2, float f3) {
        if (this.world == null) {
            return new Sprite(this, null, new Vector3f((float)d, (float)d2, (float)d3), new Quaternionf(), 1.0f);
        }
        BlockData blockData = material.createBlockData();
        Location location = this.base.clone();
        location.setYaw(0.0f);
        location.setPitch(0.0f);
        BlockDisplay blockDisplay2 = (BlockDisplay)this.world.spawn(location, BlockDisplay.class, blockDisplay -> {
            blockDisplay.setBlock(blockData);
            this.prepare((Display)blockDisplay, Display.Billboard.FIXED);
            blockDisplay.setTransformation(new Transformation(new Vector3f((float)d, (float)d2, (float)d3), new Quaternionf(), new Vector3f(0.001f, 0.001f, 0.001f), new Quaternionf()));
        });
        this.track((Entity)blockDisplay2);
        Sprite sprite = new Sprite(this, (Display)blockDisplay2, new Vector3f((float)d, (float)d2, (float)d3), new Quaternionf(), 1.0f);
        sprite.scaleVec = new Vector3f(f, f2, f3);
        this.later(2, () -> sprite.apply(5));
        return sprite;
    }

    Rig rig(double x, double y, double z) {
        return new Rig(this, x, y, z);
    }

    Model model(double x, double y, double z) {
        return new Model(this, x, y, z);
    }

    /** Calls step(t) every `every` ticks, t going from 1/steps to 1. Use it with Model.apply(every). */
    void anim(int delay, int steps, int every, java.util.function.DoubleConsumer step) {
        for (int i = 1; i <= steps; ++i) {
            double t = (double) i / steps;
            this.later(delay + (i - 1) * every, () -> step.accept(t));
        }
    }

    static double ease(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    void prepareDisplay(Display display) {
        this.prepare(display, Display.Billboard.FIXED);
    }

    void trackEntity(Entity entity) {
        this.track(entity);
    }

    private void prepare(Display display, Display.Billboard billboard) {
        display.setPersistent(false);
        display.setBillboard(billboard);
        display.setBrightness(new Display.Brightness(15, 15));
        display.setShadowRadius(0.0f);
        display.setViewRange(1.5f);
        display.setDisplayWidth(0f);
        display.setDisplayHeight(0f);
        if (this.viewer != null) {
            display.setVisibleByDefault(false);
        }
    }

    private void track(Entity entity) {
        this.spawned.add(entity);
        if (this.viewer != null && this.viewer.isOnline()) {
            this.viewer.showEntity((Plugin)this.plugin, entity);
        }
    }

    void cleanup() {
        for (Entity entity : this.spawned) {
            if (!entity.isValid()) continue;
            entity.remove();
        }
        this.spawned.clear();
    }

    static final class Sprite {
        private final Fx fx;
        final Display display;
        final Vector3f pos;
        Quaternionf rot;
        float scale;
        Vector3f scaleVec;
        boolean billboarded;

        Sprite(Fx fx, Display display, Vector3f vector3f, Quaternionf quaternionf, float f) {
            this.fx = fx;
            this.display = display;
            this.pos = vector3f;
            this.rot = quaternionf;
            this.scale = f;
        }

        void apply(int n) {
            if (this.display == null || !this.display.isValid()) {
                return;
            }
            Vector3f vector3f = this.scaleVec != null ? new Vector3f(this.scaleVec.x * this.scale, this.scaleVec.y * this.scale, this.scaleVec.z * this.scale) : new Vector3f(this.scale, this.scale, this.scale);
            this.display.setInterpolationDelay(0);
            this.display.setInterpolationDuration(Math.max(0, n));
            this.display.setTransformation(new Transformation(this.billboarded ? new Vector3f() : new Vector3f(this.pos.x, this.pos.y, this.pos.z), this.rot, vector3f, new Quaternionf()));
        }

        Sprite moveTo(double d, double d2, double d3, int n) {
            this.pos.set((float)d, (float)d2, (float)d3);
            if (this.billboarded) {
                if (this.display != null && this.display.isValid()) {
                    this.display.setTeleportDuration(Math.max(0, Math.min(59, n)));
                    Location location = this.where();
                    location.setYaw(0.0f);
                    location.setPitch(0.0f);
                    this.display.teleport(location);
                }
                return this;
            }
            this.apply(n);
            return this;
        }

        Sprite moveBy(double d, double d2, double d3, int n) {
            return this.moveTo((double)this.pos.x + d, (double)this.pos.y + d2, (double)this.pos.z + d3, n);
        }

        Sprite size(float f, int n) {
            this.scale = this.scaleVec != null ? f : f * 2.0f;
            this.apply(n);
            return this;
        }

        Sprite tilt(double d, int n) {
            this.rot = new Quaternionf().rotateZ((float)Math.toRadians(d));
            this.apply(n);
            return this;
        }

        Sprite spinFlat(double d, int n) {
            this.rot = new Quaternionf().rotateY((float)Math.toRadians(d)).rotateX(-1.5707964f);
            this.apply(n);
            return this;
        }

        void vanish(int n, int n2) {
            this.fx.later(n, () -> {
                this.scale = 0.001f;
                this.apply(n2);
                this.fx.later(n2 + 1, () -> {
                    if (this.display != null && this.display.isValid()) {
                        this.display.remove();
                    }
                });
            });
        }

        void poof(int n) {
            this.fx.later(n, () -> {
                Location location = this.fx.base.clone().add((double)this.pos.x, (double)this.pos.y, (double)this.pos.z);
                if (this.display != null) {
                    this.fx.p(Particle.POOF, location, 6, 0.15, 0.02);
                }
                if (this.display != null && this.display.isValid()) {
                    this.display.remove();
                }
            });
        }

        Location where() {
            return this.fx.base.clone().add((double)this.pos.x, (double)this.pos.y, (double)this.pos.z);
        }
    }
}

