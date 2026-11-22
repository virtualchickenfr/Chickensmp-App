package dev.killeffects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Pose;
import io.papermc.paper.entity.TeleportFlag;

/**
 * Kill effects built from block displays: little animated block models (helicopter, balloon, piano ...).
 * Coordinates are blocks relative to the victim's feet; models face the killer (or the viewer in a preview).
 */
final class Effects {
    static final String[] SPRITES = new String[0];

    /** Effects of version 1.x -> the new effect of the same rarity that replaces it. */
    static final Map<String, String> MIGRATE = Map.ofEntries(
            Map.entry("blood_splash", "tombstone"), Map.entry("poof", "gift"), Map.entry("last_love", "cake"),
            Map.entry("smoke_ring", "anvil"), Map.entry("funeral_march", "coffin"), Map.entry("confetti", "cake"),
            Map.entry("glimmer", "gift"),
            Map.entry("flame_spiral", "tnt"), Map.entry("soul_release", "pumpkin"), Map.entry("frostbite", "ice_cube"),
            Map.entry("thunderstrike", "tnt"), Map.entry("slime_splat", "slime_bounce"), Map.entry("sakura", "slime_bounce"),
            Map.entry("rest_in_peace", "pumpkin"),
            Map.entry("shockwave", "rocket"), Map.entry("void_vortex", "balloon"), Map.entry("arcane_circle", "coconut"),
            Map.entry("jackpot", "slot_machine"), Map.entry("death_mark", "balloon"), Map.entry("bubble_burst", "balloon"),
            Map.entry("dragons_wrath", "ez"), Map.entry("black_hole", "ufo"), Map.entry("ascension", "ufo"),
            Map.entry("meteor_strike", "explode"), Map.entry("chicken_storm", "piano"),
            Map.entry("royal_execution", "royal_crown"), Map.entry("phoenix", "meteor"),
            Map.entry("galaxy_collapse", "tornado"), Map.entry("wrath_of_zeus", "tornado"),
            Map.entry("the_reaper", "dismantle"), Map.entry("supernova", "helicopter"));

    private Effects() {
    }

    static Map<String, KillEffect> all() {
        List<KillEffect> l = new ArrayList<>();
        // ---------------------------------------------------------------- common
        l.add(new KillEffect("tombstone", "Tombstone", Rarity.COMMON, "A gravestone pushes out of the ground.", Material.MOSSY_STONE_BRICKS, Effects::tombstone));
        l.add(new KillEffect("coffin", "Coffin", Rarity.COMMON, "Into the box. Lid shut.", Material.DARK_OAK_PLANKS, Effects::coffin));
        l.add(new KillEffect("cake", "Party Cake", Rarity.COMMON, "Happy death day!", Material.CAKE, Effects::cake));
        l.add(new KillEffect("anvil", "Anvil Drop", Rarity.COMMON, "Look up. Too late.", Material.ANVIL, Effects::anvil));
        l.add(new KillEffect("bone_pile", "Bone Pile", Rarity.COMMON, "Nothing left but bones.", Material.BONE_BLOCK, Effects::bonePile));
        l.add(new KillEffect("gift", "Surprise Gift", Rarity.COMMON, "A present wobbles... and pops.", Material.RED_WOOL, Effects::gift));
        // ---------------------------------------------------------------- uncommon
        l.add(new KillEffect("ice_cube", "Ice Cube", Rarity.UNCOMMON, "Frozen solid, then shattered.", Material.PACKED_ICE, Effects::iceCube));
        l.add(new KillEffect("tnt", "TNT", Rarity.UNCOMMON, "Tsssss... BOOM.", Material.TNT, Effects::tnt));
        l.add(new KillEffect("slime_bounce", "Slime Bounce", Rarity.UNCOMMON, "Boing, boing, splat.", Material.SLIME_BLOCK, Effects::slimeBounce));
        l.add(new KillEffect("pumpkin", "Pumpkin Head", Rarity.UNCOMMON, "A grinning pumpkin rolls away.", Material.JACK_O_LANTERN, Effects::pumpkin));
        // ---------------------------------------------------------------- rare
        l.add(new KillEffect("balloon", "Balloon", Rarity.RARE, "Prove they're big headed.", Material.RED_WOOL, Effects::balloon));
        l.add(new KillEffect("coconut", "Coconut", Rarity.RARE, "Warning! Falling coconuts from above!", Material.JUNGLE_LOG, Effects::coconut));
        l.add(new KillEffect("rocket", "Rocket", Rarity.RARE, "Launched into space.", Material.FIREWORK_ROCKET, Effects::rocket));
        l.add(new KillEffect("slot_machine", "Slot Machine", Rarity.RARE, "Spin... spin... JACKPOT!", Material.GOLD_BLOCK, Effects::slotMachine));
        // ---------------------------------------------------------------- epic
        l.add(new KillEffect("ez", "EZ", Rarity.EPIC, "Way too easy buddy, try harder.", Material.NAME_TAG, Effects::ez));
        l.add(new KillEffect("explode", "Explode 'em", Rarity.EPIC, "Simply explodes your opponent.", Material.TNT_MINECART, Effects::explode));
        l.add(new KillEffect("awoooga", "AWOOOGA", Rarity.EPIC, "HUMINA HUMINA HUMINA!", Material.GOAT_HORN, Effects::awoooga));
        l.add(new KillEffect("i_love_you", "I Love You", Rarity.EPIC, "Awww, you mean so much to them!", Material.POPPY, Effects::iLoveYou));
        l.add(new KillEffect("tick_tock", "Tick, Tock", Rarity.EPIC, "Count their days, watch them crumble.", Material.CLOCK, Effects::tickTock));
        l.add(new KillEffect("heart_attack", "Heart Attack", Rarity.EPIC, "Their heart just stopped.", Material.RED_DYE, Effects::heartAttack));
        l.add(new KillEffect("piano", "Piano Drop", Rarity.EPIC, "A grand finale. Literally.", Material.NOTE_BLOCK, Effects::piano));
        l.add(new KillEffect("ufo", "UFO", Rarity.EPIC, "Beamed up for science.", Material.BEACON, Effects::ufo));
        // ---------------------------------------------------------------- legendary
        l.add(new KillEffect("royal_crown", "Royal Crown", Rarity.LEGENDARY, "A giant crown for the fallen king.", Material.GOLDEN_HELMET, Effects::royalCrown));
        l.add(new KillEffect("volcano", "Volcano", Rarity.LEGENDARY, "The ground rises and erupts.", Material.MAGMA_BLOCK, Effects::volcano));
        l.add(new KillEffect("tornado", "Tornado", Rarity.LEGENDARY, "Swept away by a block tornado.", Material.COBWEB, Effects::tornado));
        l.add(new KillEffect("meteor", "Meteor", Rarity.LEGENDARY, "Didn't see that one coming...", Material.MAGMA_BLOCK, Effects::meteor));
        l.add(new KillEffect("bike", "Bike", Rarity.LEGENDARY, "Turn your opponent into a bike. Look at them go!", Material.MINECART, Effects::bike));
        // ---------------------------------------------------------------- mythic
        l.add(new KillEffect("chicken_god", "Chicken God", Rarity.MYTHIC, "The almighty Golden Chicken judges them.", Material.GOLDEN_APPLE, Effects::chickenGod));
        l.add(new KillEffect("helicopter", "Helicopter", Rarity.MYTHIC, "Turns your opponent into a chopper.", Material.IRON_BLOCK, Effects::helicopter));
        l.add(new KillEffect("dismantle", "Dismantle", Rarity.MYTHIC, "Pull them to pieces. Good luck putting that back together.", Material.SHEARS, Effects::dismantle));
        l.add(new KillEffect("black_hole_x", "Black Hole", Rarity.MYTHIC, "The ground gets sucked in - then boom.", Material.OBSIDIAN, Effects::blackHole));
        Map<String, KillEffect> map = new LinkedHashMap<>();
        for (KillEffect e : l) map.put(e.id(), e);
        return map;
    }

    // ==================================================================== helpers

    private static Model root(Fx fx, double y) {
        return fx.model(0, y, 0).rot(fx.face(), 0, 0);
    }

    private static void dirt(Fx fx, Location at, int n) {
        Material under = fx.groundBlock();
        fx.block(at, under, n, 0.35, 0.1);
    }

    private static final Material[] DEBRIS = {Material.DIRT, Material.COARSE_DIRT, Material.STONE, Material.GRASS_BLOCK, Material.COBBLESTONE};

    // ==================================================================== common

    private static void tombstone(Fx fx) {
        Model m = root(fx, -1.7);
        m.box(Material.STONE_BRICKS, -0.6, 0, -0.3, 1.2, 0.25, 0.6);
        m.box(Material.STONE, -0.45, 0.25, -0.15, 0.9, 1.1, 0.3);
        m.box(Material.STONE, -0.32, 1.35, -0.15, 0.64, 0.14, 0.3);
        m.box(Material.MOSSY_COBBLESTONE, -0.5, 0.2, -0.17, 0.25, 0.35, 0.34);
        m.box(Material.MOSSY_COBBLESTONE, 0.2, 1.05, -0.17, 0.27, 0.22, 0.34);
        m.box(Material.BLACK_CONCRETE, -0.05, 0.55, 0.15, 0.1, 0.6, 0.02);
        m.box(Material.BLACK_CONCRETE, -0.22, 0.9, 0.15, 0.44, 0.1, 0.02);
        m.show(1);
        fx.sound("minecraft:block.gravel.break", 1f, 0.6f);
        fx.anim(3, 12, 2, t -> {
            m.at(fx.rand(0.04), -1.7 + 1.7 * Fx.ease(t), fx.rand(0.04)).apply(2);
            dirt(fx, fx.loc(0, 0.1, 0), 8);
            if ((int) (t * 12) % 3 == 0) fx.sound("minecraft:block.rooted_dirt.break", 0.8f, 0.7f);
        });
        fx.later(27, () -> {
            fx.sound("minecraft:block.bell.use", 0.9f, 0.5f);
            fx.p(Particle.SOUL, fx.loc(0, 1.6, 0), 6, 0.25, 0.02);
        });
        fx.anim(80, 10, 2, t -> m.at(0, -1.7 * t, 0).apply(2));
        fx.later(102, m::remove);
    }

    private static void coffin(Fx fx) {
        Model m = root(fx, -0.7);
        m.box(Material.DARK_OAK_PLANKS, -0.45, 0, -1.0, 0.9, 0.5, 2.0);
        m.box(Material.BLACK_WOOL, -0.35, 0.3, -0.9, 0.7, 0.21, 1.8);
        m.box(Material.GOLD_BLOCK, -0.5, 0.2, -0.6, 0.06, 0.08, 0.3);
        m.box(Material.GOLD_BLOCK, -0.5, 0.2, 0.3, 0.06, 0.08, 0.3);
        m.box(Material.GOLD_BLOCK, 0.44, 0.2, -0.6, 0.06, 0.08, 0.3);
        m.box(Material.GOLD_BLOCK, 0.44, 0.2, 0.3, 0.06, 0.08, 0.3);
        m.head(0, 0.62, -0.55, 0.45);
        Model lid = m.child(-0.45, 0.5, 0).rot(0, 0, 110);
        lid.box(Material.SPRUCE_PLANKS, 0, 0, -1.0, 0.9, 0.12, 2.0);
        lid.box(Material.GOLD_BLOCK, 0.4, 0.12, -0.6, 0.1, 0.02, 0.6);
        lid.box(Material.GOLD_BLOCK, 0.2, 0.12, -0.4, 0.5, 0.02, 0.1);
        m.show(1);
        fx.anim(3, 6, 2, t -> m.at(0, -0.7 * (1 - t), 0).apply(2));
        fx.later(18, () -> fx.sound("minecraft:block.bell.use", 1f, 0.5f));
        fx.later(30, () -> {
            lid.rot(0, 0, 0).apply(4);
            fx.sound(4, "minecraft:block.wooden_door.close", 1f, 0.5f);
            fx.later(4, () -> dirt(fx, fx.loc(0, 0.5, 0), 15));
        });
        fx.later(42, () -> fx.sound("minecraft:block.note_block.bass", 1f, 0.6f));
        fx.later(50, () -> fx.sound("minecraft:block.note_block.bass", 1f, 0.53f));
        fx.anim(62, 12, 2, t -> {
            m.at(0, -0.9 * t, 0).apply(2);
            dirt(fx, fx.loc(0, 0.1, 0), 5);
        });
        fx.later(88, m::remove);
    }

    private static void cake(Fx fx) {
        Model m = root(fx, 0);
        m.boxC("minecraft:cake", 0, 0.5, 0, 1, 1, 1);
        m.boxC("minecraft:red_candle[candles=1,lit=true]", 0, 1.0, 0, 1, 1, 1);
        m.size(0.01).show(1);
        fx.later(2, () -> m.size(1.9).apply(5));
        fx.later(7, () -> m.size(1.5).apply(3));
        float[] tune = {1.06f, 1.06f, 1.19f, 1.06f, 1.41f, 1.33f};
        for (int i = 0; i < tune.length; i++) {
            float p = tune[i];
            fx.later(6 + i * 5, () -> fx.sound("minecraft:block.note_block.bell", 1f, p));
        }
        fx.repeat(14, 3, i -> fx.p(Particle.SMALL_FLAME, m.where(0, 1.45, 0), 1, 0.01, 0.0));
        fx.later(40, () -> {
            int[] colors = {0xFF4D6D, 0xFFD84A, 0x4FC3FF, 0x7CFF6B, 0xC37BFF};
            for (int c : colors) fx.dust(fx.loc(0, 2.2, 0), c, 1.2f, 14, 0.8);
            fx.p(Particle.FIREWORK, fx.loc(0, 2, 0), 20, 0.3, 0.1);
            fx.sound("minecraft:entity.firework_rocket.twinkle", 1f, 1.2f);
        });
        for (int b = 0; b < 3; b++) {
            int bite = b;
            fx.later(50 + b * 8, () -> {
                m.size(1.5 - (bite + 1) * 0.45).apply(4);
                fx.sound("minecraft:entity.generic.eat", 1f, 1f);
                fx.block(fx.loc(0, 0.8, 0), Material.CAKE, 10, 0.3, 0.1);
            });
        }
        fx.later(76, () -> fx.sound("minecraft:entity.player.burp", 1f, 1f));
        fx.later(78, m::remove);
    }

    private static void anvil(Fx fx) {
        Model m = root(fx, 14);
        m.boxC("minecraft:anvil[facing=east]", 0, 0.5, 0, 1, 1, 1);
        m.size(1.3).show(0);
        fx.sound("minecraft:block.anvil.place", 0.4f, 2f);
        fx.anim(4, 7, 1, t -> m.at(0, 14 - 14 * t * t, 0).apply(1));
        fx.later(11, () -> {
            fx.sound("minecraft:block.anvil.land", 1f, 0.8f);
            fx.sound("minecraft:entity.player.attack.crit", 1f, 0.6f);
            fx.block(fx.loc(0, 0.2, 0), fx.groundBlock(), 40, 0.6, 0.2);
            fx.p(Particle.CRIT, fx.loc(0, 0.5, 0), 25, 0.6, 0.3);
            m.size(1.3).apply(0);
        });
        fx.later(12, () -> m.at(0, 0.25, 0).apply(3));
        fx.later(15, () -> m.at(0, 0, 0).apply(3));
        fx.repeat(12, 3, i -> {
            double a = i * 0.9;
            fx.p(Particle.WAX_ON, fx.loc(Math.cos(a) * 0.7, 1.7, Math.sin(a) * 0.7), 1, 0, 0);
            if (i % 4 == 0) fx.sound("minecraft:block.note_block.chime", 0.5f, 1.8f);
        });
        m.shatter(60, 10, 1.5, 0.3);
    }

    private static void bonePile(Fx fx) {
        List<Model> bones = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            double x = fx.rand(0.55), z = fx.rand(0.55);
            double land = 0.15 + (i % 3) * 0.2;
            Model b = fx.model(x, 4 + fx.rnd.nextDouble() * 3, z).rot(fx.rand(180), fx.rand(180), fx.rand(180));
            b.boxC(Material.BONE_BLOCK, 0, 0, 0, 0.7, 0.18, 0.18);
            b.boxC(Material.BONE_BLOCK, -0.33, 0, 0, 0.12, 0.3, 0.3);
            b.boxC(Material.BONE_BLOCK, 0.33, 0, 0, 0.12, 0.3, 0.3);
            b.show(0);
            int start = i * 3;
            double y0 = b.lpos.y;
            double yaw = fx.rand(180);
            fx.anim(start, 6, 1, t -> b.at(x, y0 - (y0 - land) * t * t, z).rot(yaw, 0, 90 * (1 - t)).apply(1));
            fx.sound(start + 6, "minecraft:entity.skeleton.step", 1f, 0.8f + i * 0.08f);
            bones.add(b);
        }
        Model skull = fx.model(0, 6, 0).rot(fx.face(), 0, 0);
        skull.item(Material.SKELETON_SKULL, 0, 0.3, 0, 1.0);
        skull.size(1.4).show(0);
        fx.anim(30, 6, 1, t -> skull.at(0, 6 - 5.2 * t * t, 0).apply(1));
        fx.later(37, () -> {
            fx.sound("minecraft:entity.skeleton.death", 1f, 0.9f);
            fx.item(fx.loc(0, 0.8, 0), Material.BONE, 20, 0.3, 0.1);
        });
        fx.later(75, () -> {
            for (Model b : bones) b.shatter(0, 8, 0.8, 0.2);
            skull.shatter(0, 8, 0.8, 0.4);
            fx.sound("minecraft:entity.skeleton.hurt", 1f, 0.7f);
        });
    }

    private static void gift(Fx fx) {
        Model m = root(fx, 0);
        Model box = m.child(0, 0, 0);
        box.boxC(Material.RED_WOOL, 0, 0.4, 0, 0.8, 0.8, 0.8);
        box.boxC(Material.YELLOW_WOOL, 0, 0.4, 0, 0.84, 0.82, 0.16);
        box.boxC(Material.YELLOW_WOOL, 0, 0.4, 0, 0.16, 0.82, 0.84);
        box.boxC(Material.RED_WOOL, 0, 0.86, 0, 0.88, 0.14, 0.88);
        box.boxC(Material.YELLOW_WOOL, 0, 0.86, 0, 0.9, 0.15, 0.16);
        box.boxC(Material.YELLOW_WOOL, 0, 0.86, 0, 0.16, 0.15, 0.9);
        Model bow = box.child(0, 0.95, 0);
        bow.boxC(Material.YELLOW_WOOL, -0.14, 0.1, 0, 0.26, 0.2, 0.1);
        bow.boxC(Material.YELLOW_WOOL, 0.14, 0.1, 0, 0.26, 0.2, 0.1);
        m.size(0.01).show(1);
        fx.later(2, () -> m.size(1.2).apply(5));
        fx.sound("minecraft:entity.item.pickup", 1f, 0.6f);
        for (int i = 0; i < 6; i++) {
            int k = i;
            fx.later(12 + i * 4, () -> {
                box.rot(0, 0, k % 2 == 0 ? 12 : -12).apply(3);
                fx.sound("minecraft:block.wool.hit", 1f, 0.8f + k * 0.1f);
            });
        }
        fx.later(36, () -> box.rot(0, 0, 0).at(0, 0.2, 0).apply(3));
        fx.later(40, () -> {
            m.shatter(0, 10, 2.2, 0.8);
            int[] colors = {0xFF4D6D, 0xFFD84A, 0x4FC3FF, 0x7CFF6B, 0xC37BFF, 0xFF9A2A};
            for (int c : colors) fx.dust(fx.loc(0, 1.2, 0), c, 1.3f, 16, 1.0);
            fx.p(Particle.FIREWORK, fx.loc(0, 1, 0), 30, 0.2, 0.15);
            fx.sound("minecraft:entity.firework_rocket.blast", 1f, 1.2f);
            fx.sound(3, "minecraft:entity.firework_rocket.twinkle", 1f, 1f);
        });
    }

    // ==================================================================== uncommon

    private static void iceCube(Fx fx) {
        Model m = root(fx, 0);
        m.head(0, 1.55, 0, 0.55);
        Model ice = m.child(0, 0, 0);
        for (int i = 0; i < 4; i++) {
            ice.box(Material.LIGHT_BLUE_STAINED_GLASS, -0.55, i * 0.55, -0.55, 1.1, 0.55, 1.1);
        }
        ice.box(Material.PACKED_ICE, -0.6, -0.05, -0.6, 1.2, 0.15, 1.2);
        ice.box(Material.ICE, -0.6, 2.18, -0.6, 1.2, 0.12, 1.2);
        m.size(0.01).show(1);
        fx.later(2, () -> m.size(1).apply(8));
        fx.sound("minecraft:entity.player.hurt_freeze", 1f, 0.8f);
        fx.sound(4, "minecraft:block.glass.place", 1f, 0.6f);
        fx.repeat(12, 2, i -> fx.p(Particle.SNOWFLAKE, fx.loc(0, 1.1, 0), 6, 0.6, 0.02));
        for (int i = 0; i < 3; i++) {
            int k = i;
            fx.later(30 + i * 7, () -> {
                fx.sound("minecraft:block.glass.hit", 1f, 0.8f + k * 0.2f);
                m.at(fx.rand(0.05), 0, fx.rand(0.05)).apply(1);
                fx.block(fx.loc(0, 1.1, 0), Material.ICE, 15, 0.5, 0.05);
            });
        }
        fx.later(52, () -> {
            m.shatter(0, 12, 2.0, 0.6);
            fx.sound("minecraft:block.glass.break", 1f, 0.7f);
            fx.sound("minecraft:block.amethyst_block.break", 1f, 1.4f);
            fx.p(Particle.SNOWFLAKE, fx.loc(0, 1.1, 0), 50, 0.6, 0.15);
            fx.block(fx.loc(0, 1.1, 0), Material.PACKED_ICE, 60, 0.5, 0.25);
        });
    }

    private static void tnt(Fx fx) {
        Model m = root(fx, 6);
        m.cube(Material.TNT, 0, 0.5, 0, 1);
        Model flash = m.child(0, 0, 0);
        flash.cube(Material.WHITE_CONCRETE, 0, 0.5, 0, 1.02);
        flash.size(0.01);
        m.show(0);
        fx.anim(2, 8, 1, t -> m.at(0, 6 - 6 * t * t, 0).apply(1));
        fx.later(10, () -> {
            fx.sound("minecraft:block.grass.place", 1f, 0.6f);
            fx.sound("minecraft:entity.tnt.primed", 1f, 1f);
        });
        for (int i = 0; i < 8; i++) {
            int k = i;
            fx.later(14 + i * 4, () -> {
                flash.size(k % 2 == 0 ? 1 : 0.01).apply(0);
                m.size(1 + k * 0.04).apply(4);
            });
        }
        fx.repeat(16, 2, i -> fx.p(Particle.SMOKE, fx.loc(0, 1.2, 0), 2, 0.05, 0.01));
        fx.later(46, () -> {
            m.remove();
            Model debris = fx.model(0, 0, 0);
            for (int i = 0; i < 14; i++) {
                debris.cube(DEBRIS[i % DEBRIS.length], fx.rand(0.6), 0.3 + fx.rnd.nextDouble() * 0.6, fx.rand(0.6), 0.25 + fx.rnd.nextDouble() * 0.2);
            }
            debris.apply(0);
            debris.shatter(1, 14, 4, 0.9);
            fx.p(Particle.EXPLOSION_EMITTER, fx.loc(0, 0.6, 0), 1, 0, 0);
            fx.p(Particle.FLAME, fx.loc(0, 0.6, 0), 30, 0.4, 0.15);
            fx.sound("minecraft:entity.generic.explode", 1f, 1f);
        });
    }

    private static void slimeBounce(Fx fx) {
        Model m = root(fx, 0);
        m.head(0, 0.5, 0, 0.6);
        m.cube(Material.SLIME_BLOCK, 0, 0.5, 0, 1.1);
        m.size(0.01).show(1);
        fx.later(2, () -> m.size(1).apply(4));
        double[] heights = {1.2, 2.2, 3.4};
        int t0 = 8;
        for (double h : heights) {
            int start = t0;
            fx.later(start, () -> fx.sound("minecraft:block.slime_block.fall", 1f, 0.8f));
            fx.anim(start, 6, 1, t -> m.at(0, h * Math.sin(t * Math.PI / 2), 0).apply(1));
            fx.anim(start + 6, 6, 1, t -> m.at(0, h * Math.cos(t * Math.PI / 2), 0).apply(1));
            fx.later(start + 12, () -> {
                fx.p(Particle.ITEM_SLIME, fx.loc(0, 0.2, 0), 20, 0.4, 0.1);
                fx.sound("minecraft:entity.slime.squish", 1f, 0.7f);
            });
            t0 += 14;
        }
        fx.later(t0 + 2, () -> {
            m.remove();
            Model splat = fx.model(0, 0, 0);
            splat.boxC(Material.SLIME_BLOCK, 0, 0.05, 0, 2.4, 0.1, 2.4);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                splat.cube(Material.SLIME_BLOCK, Math.cos(a) * 1.5, 0.08, Math.sin(a) * 1.5, 0.3);
            }
            splat.size(0.2).show(0);
            fx.later(1, () -> splat.size(1).apply(3));
            fx.sound("minecraft:entity.slime.death", 1f, 0.5f);
            fx.p(Particle.ITEM_SLIME, fx.loc(0, 0.3, 0), 60, 0.8, 0.2);
            splat.vanish(30, 10);
        });
    }

    private static void pumpkin(Fx fx) {
        Model m = root(fx, 0);
        Model head = m.child(0, 0.75, 0);
        head.boxC("minecraft:carved_pumpkin[facing=south]", 0, 0, 0, 1.5, 1.5, 1.5);
        Model lit = head.child(0, 0, 0);
        lit.boxC("minecraft:jack_o_lantern[facing=south]", 0, 0, 0, 1.52, 1.52, 1.52);
        lit.size(0.01);
        m.size(0.01).show(1);
        fx.later(2, () -> m.size(1).apply(10));
        fx.sound("minecraft:block.pumpkin.carve", 1f, 0.8f);
        fx.anim(2, 10, 1, t -> head.rot(360 * t, 0, 0).apply(1));
        fx.later(20, () -> {
            lit.size(1).apply(0);
            fx.sound("minecraft:entity.witch.celebrate", 1f, 0.7f);
            fx.p(Particle.FLAME, fx.loc(0, 0.8, 0), 20, 0.5, 0.05);
        });
        fx.anim(40, 20, 1, t -> {
            head.at(3.5 * t, 0.75, 0).rot(0, 0, -360 * 2 * t).apply(1);
            if ((int) (t * 20) % 5 == 0) fx.sound("minecraft:block.wood.step", 1f, 0.7f);
        });
        fx.later(62, () -> {
            m.shatter(0, 10, 1.6, 0.4);
            fx.block(m.where(3.5, 0.75, 0), Material.PUMPKIN, 40, 0.5, 0.2);
            fx.sound("minecraft:block.wood.break", 1f, 0.8f);
        });
    }

    // ==================================================================== rare

    private static void rocket(Fx fx) {
        Model m = root(fx, 0);
        m.boxC(Material.WHITE_CONCRETE, 0, 1.0, 0, 0.6, 1.4, 0.6);
        m.boxC(Material.RED_CONCRETE, 0, 1.8, 0, 0.5, 0.25, 0.5);
        m.boxC(Material.RED_CONCRETE, 0, 2.0, 0, 0.3, 0.2, 0.3);
        m.boxC(Material.RED_CONCRETE, 0, 2.15, 0, 0.12, 0.15, 0.12);
        m.boxC(Material.LIGHT_BLUE_STAINED_GLASS, 0, 1.35, 0.3, 0.3, 0.3, 0.04);
        m.head(0, 1.35, 0.15, 0.25);
        for (int i = 0; i < 4; i++) {
            Model fin = m.child(0, 0.35, 0).rot(i * 90, 0, 0);
            fin.box(Material.RED_CONCRETE, 0.28, 0, -0.04, 0.3, 0.5, 0.08);
        }
        m.boxC(Material.GRAY_CONCRETE, 0, 0.22, 0, 0.36, 0.15, 0.36);
        m.size(0.01).show(1);
        fx.later(2, () -> m.size(1).apply(6));
        fx.sound("minecraft:block.iron_trapdoor.close", 1f, 0.6f);
        fx.anim(10, 14, 1, t -> {
            m.at(fx.rand(0.04), 0, fx.rand(0.04)).apply(1);
            fx.p(Particle.CAMPFIRE_COSY_SMOKE, fx.loc(0, 0.1, 0), 2, 0.3, 0.02);
        });
        fx.later(12, () -> fx.sound("minecraft:entity.firework_rocket.launch", 1f, 0.5f));
        fx.anim(25, 30, 1, t -> {
            m.at(0, 28 * t * t, 0).rot(fx.face(), 0, 0).apply(1);
            Location tail = m.where(0, 0.1, 0);
            fx.p(Particle.FLAME, tail, 4, 0.08, 0.02);
            fx.p(Particle.LARGE_SMOKE, tail, 2, 0.1, 0.02);
        });
        fx.repeat(4, 6, i -> fx.sound("minecraft:entity.firework_rocket.launch", 1f, 0.6f + i * 0.15f));
        fx.later(56, () -> {
            Location at = fx.loc(0, 29, 0);
            m.remove();
            int[] colors = {0xFF4D6D, 0xFFD84A, 0x4FC3FF, 0xFFFFFF};
            for (int c : colors) fx.dust(at, c, 2f, 40, 2.5);
            fx.explode(at, Particle.FIREWORK, 80, 0.5);
            fx.sound("minecraft:entity.firework_rocket.large_blast", 2f, 1f);
            fx.sound(4, "minecraft:entity.firework_rocket.twinkle_far", 2f, 1f);
        });
    }

    private static final Material[] REELS = {Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.REDSTONE_BLOCK, Material.LAPIS_BLOCK, Material.GOLD_BLOCK, Material.AMETHYST_BLOCK};

    private static void slotMachine(Fx fx) {
        Model m = root(fx, 0);
        m.boxC(Material.RED_CONCRETE, 0, 0.45, 0, 1.6, 0.9, 0.8);
        m.boxC(Material.RED_CONCRETE, 0, 1.3, -0.1, 1.6, 0.8, 0.6);
        m.boxC(Material.GOLD_BLOCK, 0, 1.75, -0.1, 1.7, 0.12, 0.7);
        m.boxC(Material.BLACK_CONCRETE, 0, 1.3, 0.2, 1.4, 0.52, 0.02);
        m.boxC(Material.GOLD_BLOCK, 0, 0.92, 0.1, 1.7, 0.08, 0.95);
        m.boxC(Material.YELLOW_CONCRETE, 0, 0.5, 0.41, 0.6, 0.12, 0.02);
        Model lever = m.child(0.85, 1.0, 0);
        lever.boxC(Material.IRON_BLOCK, 0.06, 0.35, 0, 0.06, 0.7, 0.06);
        lever.cube(Material.REDSTONE_BLOCK, 0.06, 0.75, 0, 0.18);
        List<Model> reels = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Model r = m.child(-0.45 + i * 0.45, 1.3, 0.12);
            r.cube(REELS[i], 0, 0, 0, 0.36);
            reels.add(r);
        }
        m.size(0.01).show(1);
        fx.later(2, () -> m.size(1.2).apply(6));
        fx.later(10, () -> {
            lever.rot(0, 70, 0).apply(3);
            fx.sound("minecraft:block.lever.click", 1f, 0.6f);
        });
        fx.later(16, () -> lever.rot(0, 0, 0).apply(4));
        for (int i = 0; i < 3; i++) {
            Model r = reels.get(i);
            int stop = 22 + i * 10;
            for (int t = 14; t < 14 + stop; t += 2) {
                int k = t;
                fx.later(t, () -> {
                    setBlock(r, REELS[Math.floorMod(k / 2 + reels.indexOf(r) * 2, REELS.length)]);
                    r.rot(0, (k % 4) * 45, 0).apply(1);
                });
            }
            fx.later(14 + stop, () -> {
                setBlock(r, Material.GOLD_BLOCK);
                r.rot(0, 0, 0).apply(2);
                fx.sound("minecraft:block.note_block.pling", 1f, 1f + 0.25f * reels.indexOf(r));
            });
        }
        fx.repeat(16, 3, i -> fx.sound("minecraft:block.note_block.hat", 0.6f, 1.4f));
        fx.later(60, () -> {
            fx.sound("minecraft:entity.player.levelup", 1f, 1f);
            fx.sound("minecraft:block.amethyst_block.chime", 1f, 1.5f);
            fx.item(fx.loc(0, 1.8, 0), Material.GOLD_NUGGET, 40, 0.3, 0.25);
            fx.item(fx.loc(0, 1.8, 0), Material.GOLD_INGOT, 15, 0.3, 0.25);
            fx.p(Particle.WAX_ON, fx.loc(0, 1.4, 0), 30, 0.9, 0.1);
        });
        fx.repeat(6, 4, i -> {
            if (fx.world != null) fx.item(fx.loc(0, 0.6, 0.6), Material.GOLD_NUGGET, 8, 0.15, 0.15);
            fx.sound("minecraft:entity.experience_orb.pickup", 0.8f, 0.8f + i * 0.1f);
        });
        m.shatter(95, 10, 1.6, 0.4);
    }

    private static void setBlock(Model m, Material mat) {
        for (Model.Part p : m.parts) {
            if (p.d instanceof BlockDisplay bd && bd.isValid()) bd.setBlock(mat.createBlockData());
        }
    }

    // ==================================================================== epic

    private static void piano(Fx fx) {
        Model m = root(fx, 16);
        m.boxC(Material.BLACK_CONCRETE, 0, 0.95, 0, 1.8, 0.5, 1.2);
        m.boxC(Material.BLACK_CONCRETE, 0.2, 0.95, -0.7, 1.4, 0.5, 0.4);
        m.boxC(Material.QUARTZ_BLOCK, 0, 0.88, 0.68, 1.6, 0.1, 0.2);
        for (int i = 0; i < 7; i++) {
            if (i == 2 || i == 6) continue;
            m.boxC(Material.BLACK_CONCRETE, -0.6 + i * 0.2, 0.95, 0.65, 0.09, 0.06, 0.12);
        }
        m.boxC(Material.BLACK_CONCRETE, -0.75, 0.35, 0.4, 0.12, 0.7, 0.12);
        m.boxC(Material.BLACK_CONCRETE, 0.75, 0.35, 0.4, 0.12, 0.7, 0.12);
        m.boxC(Material.BLACK_CONCRETE, 0.4, 0.35, -0.75, 0.12, 0.7, 0.12);
        Model lid = m.child(-0.9, 1.2, 0).rot(0, 0, -35);
        lid.box(Material.BLACK_CONCRETE, 0, 0, -0.6, 1.8, 0.06, 1.2);
        m.boxC(Material.GOLD_BLOCK, 0, 1.2, 0.2, 0.06, 0.4, 0.06);
        m.size(1.2).show(0);
        fx.sound("minecraft:block.note_block.harp", 1f, 2f);
        fx.anim(10, 10, 1, t -> m.at(0, 16 - 16 * t * t, 0).rot(fx.face(), 0, t * 8).apply(1));
        fx.later(21, () -> {
            m.shatter(0, 14, 3.0, 0.9);
            fx.sound("minecraft:block.note_block.bass", 2f, 0.5f);
            fx.sound("minecraft:block.note_block.harp", 2f, 0.53f);
            fx.sound("minecraft:block.note_block.harp", 2f, 0.75f);
            fx.sound("minecraft:block.note_block.pling", 2f, 0.6f);
            fx.sound("minecraft:block.wood.break", 2f, 0.6f);
            fx.sound("minecraft:entity.generic.explode", 0.6f, 1.5f);
            fx.block(fx.loc(0, 0.5, 0), Material.BLACK_CONCRETE, 60, 0.8, 0.2);
            fx.explode(fx.loc(0, 1, 0), Particle.NOTE, 25, 0.6);
        });
        fx.repeat(8, 4, i -> {
            fx.sound(24, "minecraft:block.note_block.harp", 0.6f, 0.5f + fx.rnd.nextFloat());
            fx.p(Particle.NOTE, fx.loc(fx.rand(1), 1 + i * 0.3, fx.rand(1)), 1, 0, 0);
        });
    }

    private static void ufo(Fx fx) {
        Model u = root(fx, 18);
        u.boxC(Material.LIGHT_GRAY_CONCRETE, 0, 0, 0, 3.2, 0.22, 3.2);
        u.boxC(Material.IRON_BLOCK, 0, 0, 0, 2.6, 0.4, 2.6);
        u.boxC(Material.GRAY_CONCRETE, 0, -0.25, 0, 1.6, 0.2, 1.6);
        u.boxC(Material.LIGHT_BLUE_STAINED_GLASS, 0, 0.45, 0, 1.3, 0.6, 1.3);
        u.boxC(Material.LIGHT_BLUE_STAINED_GLASS, 0, 0.8, 0, 0.8, 0.2, 0.8);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            u.cube(i % 2 == 0 ? Material.SEA_LANTERN : Material.LIME_CONCRETE, Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5, 0.22);
        }
        Model beam = fx.model(0, 0, 0);
        beam.box(Material.LIME_STAINED_GLASS, -0.55, 0, -0.55, 1.1, 1, 1.1);
        Model head = fx.model(0, 0.4, 0);
        head.head(0, 0.25, 0, 0.6);
        u.show(0);
        head.size(0.01).show(0);
        fx.later(2, () -> head.size(1).apply(4));
        fx.sound("minecraft:block.beacon.ambient", 2f, 1.6f);
        fx.anim(2, 16, 1, t -> u.at(0, 18 - 12.5 * Fx.ease(t), 0).rot(fx.face() + t * 180, 0, 0).apply(1));
        fx.later(20, () -> {
            fx.sound("minecraft:block.beacon.activate", 2f, 1.4f);
            beam.size(0.01).show(0);
            beam.at(0, 0, 0).apply(0);
        });
        fx.later(22, () -> {
            beam.scale = 1;
            if (!beam.parts.isEmpty()) beam.parts.get(0).size.set(1.1f, 5.3f, 1.1f);
            beam.apply(6);
        });
        final double[] yaw = {fx.face() + 180};
        fx.anim(20, 40, 1, t -> {
            yaw[0] += 9;
            u.at(0, 5.5 + Math.sin(t * 12) * 0.1, 0).rot(yaw[0], 0, 0).apply(1);
            if (t > 0.25) {
                double k = (t - 0.25) / 0.75;
                head.at(0, 0.4 + 4.6 * Fx.ease(k), 0).rot(k * 720, 0, k * 30).apply(1);
            }
            fx.p(Particle.END_ROD, fx.loc(fx.rand(0.45), fx.rnd.nextDouble() * 5, fx.rand(0.45)), 1, 0, 0.01);
        });
        fx.later(60, () -> {
            head.remove();
            beam.size(0.01).apply(4);
            fx.sound("minecraft:block.beacon.deactivate", 2f, 1.4f);
        });
        fx.later(66, beam::remove);
        double yawR = Math.toRadians(fx.face() + 60);
        fx.anim(70, 14, 1, t -> u.at(Math.sin(yawR) * 30 * t * t, 5.5 + 14 * t * t, Math.cos(yawR) * 30 * t * t).rot(yaw[0] + t * 400, 0, 15).apply(1));
        fx.later(70, () -> fx.sound("minecraft:entity.enderman.teleport", 2f, 0.6f));
        fx.later(86, u::remove);
    }

    // ==================================================================== legendary

    private static void royalCrown(Fx fx) {
        Model c = root(fx, 14);
        int n = 12;
        double r = 1.25;
        for (int i = 0; i < n; i++) {
            Model seg = c.child(0, 0, 0).rot(i * 360.0 / n, 0, 0);
            seg.box(Material.GOLD_BLOCK, -0.36, 0, r - 0.15, 0.72, 0.55, 0.3);
            if (i % 2 == 0) {
                seg.box(Material.GOLD_BLOCK, -0.18, 0.55, r - 0.12, 0.36, 0.45, 0.24);
                seg.box(Material.GOLD_BLOCK, -0.08, 1.0, r - 0.08, 0.16, 0.2, 0.16);
                Material gem = i % 6 == 0 ? Material.EMERALD_BLOCK : (i % 6 == 2 ? Material.DIAMOND_BLOCK : Material.REDSTONE_BLOCK);
                seg.box(gem, -0.12, 0.17, r + 0.12, 0.24, 0.24, 0.06);
            }
        }
        c.box(Material.RED_WOOL, -1.0, 0, -1.0, 2.0, 0.08, 2.0);
        c.show(0);
        fx.sound("minecraft:event.raid.horn", 2f, 1.4f);
        final double[] yaw = {0};
        fx.anim(2, 24, 1, t -> {
            yaw[0] += 6;
            c.at(0, 14 - 14 * Fx.ease(t), 0).rot(yaw[0], 0, 0).apply(1);
            fx.p(Particle.END_ROD, c.where(fx.rand(1.3), 0.6, fx.rand(1.3)), 1, 0, 0.01);
        });
        fx.later(27, () -> {
            fx.sound("minecraft:block.anvil.land", 1f, 1.6f);
            fx.sound("minecraft:ui.toast.challenge_complete", 1f, 1.1f);
            fx.p(Particle.WAX_ON, fx.loc(0, 1, 0), 40, 1.4, 0.2);
            fx.p(Particle.TOTEM_OF_UNDYING, fx.loc(0, 1, 0), 60, 0.8, 0.4);
        });
        fx.anim(28, 36, 1, t -> {
            yaw[0] += 4;
            c.at(0, Math.sin(t * Math.PI * 2) * 0.1 + 0.05, 0).rot(yaw[0], 0, 0).apply(1);
            if ((int) (t * 36) % 6 == 0) fx.p(Particle.WAX_OFF, c.where(fx.rand(1.3), 1.0, fx.rand(1.3)), 3, 0.1, 0.02);
        });
        fx.anim(66, 20, 1, t -> {
            yaw[0] += 10;
            c.at(0, 10 * t * t, 0).rot(yaw[0], 0, 0).size(1 - 0.9 * t).apply(1);
        });
        fx.later(66, () -> fx.sound("minecraft:block.beacon.power_select", 1.5f, 1.4f));
        fx.later(88, c::remove);
    }

    private static void volcano(Fx fx) {
        Model v = root(fx, -3.2);
        double[][] layers = {{3.0, 0.8, 0}, {2.4, 0.8, 1}, {1.8, 0.8, 2}, {1.2, 0.8, 3}};
        Material[] mats = {Material.STONE, Material.BASALT, Material.BLACKSTONE, Material.BLACKSTONE};
        for (double[] l : layers) {
            int i = (int) l[2];
            v.boxC(mats[i], 0, i * 0.8 + 0.4, 0, l[0], l[1], l[0]);
        }
        v.boxC(Material.MAGMA_BLOCK, 0, 3.22, 0, 0.9, 0.05, 0.9);
        v.boxC(Material.ORANGE_CONCRETE, 0.9, 1.6, 0.9, 0.3, 0.9, 0.12);
        v.boxC(Material.ORANGE_CONCRETE, -0.7, 2.2, 0.6, 0.25, 0.7, 0.12);
        v.show(1);
        fx.sound("minecraft:entity.warden.emerge", 1.5f, 1.2f);
        fx.anim(3, 20, 1, t -> {
            v.at(fx.rand(0.05), -3.2 + 3.2 * Fx.ease(t), fx.rand(0.05)).apply(1);
            dirt(fx, fx.loc(0, 0.2, 0), 10);
        });
        fx.repeat(6, 4, i -> fx.sound(24, "minecraft:block.lava.ambient", 1.5f, 0.7f + i * 0.1f));
        fx.later(36, () -> {
            fx.sound("minecraft:entity.generic.explode", 2f, 0.6f);
            fx.sound("minecraft:item.firecharge.use", 2f, 0.5f);
            Location top = fx.loc(0, 3.4, 0);
            fx.p(Particle.EXPLOSION, top, 4, 0.4, 0);
            for (int i = 0; i < 12; i++) {
                Model blob = fx.model(0, 3.3, 0);
                blob.cube(i % 2 == 0 ? Material.MAGMA_BLOCK : Material.ORANGE_CONCRETE, 0, 0, 0, 0.35 + fx.rnd.nextDouble() * 0.2);
                blob.show(0);
                double a = fx.rnd.nextDouble() * Math.PI * 2, d = 2 + fx.rnd.nextDouble() * 3, hgt = 3 + fx.rnd.nextDouble() * 4;
                fx.anim(1, 20, 1, t -> {
                    blob.at(Math.cos(a) * d * t, 3.3 + hgt * 4 * t * (1 - t) - 3.3 * t, Math.sin(a) * d * t).rot(t * 400, t * 300, 0).apply(1);
                    fx.p(Particle.FLAME, blob.where(0, 0, 0), 1, 0.05, 0.01);
                });
                blob.vanish(22, 4);
            }
        });
        fx.repeat(25, 2, i -> {
            Location top = fx.loc(0, 3.4, 0);
            fx.p(Particle.LAVA, top, 2, 0.3, 0);
            fx.p(Particle.CAMPFIRE_COSY_SMOKE, top, 2, 0.3, 0.05);
        });
        fx.anim(80, 16, 1, t -> v.at(0, -3.4 * t, 0).apply(1));
        fx.later(98, v::remove);
    }

    private static void tornado(Fx fx) {
        Model t0 = fx.model(0, 0, 0);
        List<Model> rings = new ArrayList<>();
        Material[] mats = {Material.LIGHT_GRAY_WOOL, Material.WHITE_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL};
        for (int lvl = 0; lvl < 9; lvl++) {
            Model ring = t0.child(0, lvl * 0.55, 0);
            double r = 0.35 + lvl * 0.2;
            int n = 4 + lvl / 2;
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                Material mat = (lvl + i) % 5 == 0 ? DEBRIS[(lvl + i) % DEBRIS.length] : mats[(lvl + i) % mats.length];
                ring.boxC(mat, Math.cos(a) * r, 0, Math.sin(a) * r, 0.35, 0.3, 0.35);
            }
            if (lvl % 3 == 1) ring.cube(Material.OAK_LOG, r + 0.3, 0.1, 0, 0.3);
            rings.add(ring);
        }
        Model head = t0.child(0, 0.5, 0);
        head.head(0, 0, 0, 0.55);
        t0.size(0.01).show(1);
        fx.later(2, () -> t0.size(1).apply(8));
        fx.sound("minecraft:item.elytra.flying", 1.5f, 0.6f);
        final double[] spin = new double[rings.size() + 1];
        fx.anim(4, 50, 2, t -> {
            for (int i = 0; i < rings.size(); i++) {
                spin[i] += 40 + i * 6;
                rings.get(i).rot(spin[i], 0, 0);
            }
            spin[rings.size()] += 50;
            head.at(Math.cos(t * 20) * 0.4, 0.5 + 4.2 * t, Math.sin(t * 20) * 0.4).rot(spin[rings.size()], 0, 20);
            t0.at(Math.sin(t * 6) * 0.8, 0, Math.cos(t * 5) * 0.6).apply(2);
            fx.p(Particle.CLOUD, fx.loc(fx.rand(1.5), 0.1, fx.rand(1.5)), 3, 0.2, 0.05);
            fx.block(fx.loc(0, 0.2, 0), fx.groundBlock(), 2, 0.8, 0.1);
        });
        fx.repeat(5, 20, i -> fx.sound("minecraft:item.elytra.flying", 1.2f, 0.5f + i * 0.05f));
        fx.later(104, () -> {
            t0.shatter(0, 12, 3, 0.7);
            fx.p(Particle.CLOUD, fx.loc(0, 2, 0), 40, 1.2, 0.1);
            fx.sound("minecraft:entity.breeze.wind_burst", 1.5f, 0.6f);
        });
    }

    // ==================================================================== mythic

    private static void chickenGod(Fx fx) {
        Model c = root(fx, 22);
        c.boxC(Material.GOLD_BLOCK, 0, 1.4, 0, 1.4, 1.1, 1.8);
        c.boxC(Material.GOLD_BLOCK, 0, 1.6, -1.0, 1.0, 0.8, 0.4);
        c.boxC(Material.ORANGE_CONCRETE, -0.35, 0.45, 0.1, 0.15, 0.9, 0.15);
        c.boxC(Material.ORANGE_CONCRETE, 0.35, 0.45, 0.1, 0.15, 0.9, 0.15);
        c.boxC(Material.ORANGE_CONCRETE, -0.35, 0.05, 0.25, 0.4, 0.1, 0.5);
        c.boxC(Material.ORANGE_CONCRETE, 0.35, 0.05, 0.25, 0.4, 0.1, 0.5);
        Model head = c.child(0, 2.1, 0.75);
        head.boxC(Material.GOLD_BLOCK, 0, 0.5, 0, 0.9, 1.0, 0.8);
        head.boxC(Material.ORANGE_CONCRETE, 0, 0.45, 0.55, 0.5, 0.25, 0.35);
        head.boxC(Material.RED_CONCRETE, 0, 0.18, 0.45, 0.25, 0.3, 0.12);
        head.boxC(Material.BLACK_CONCRETE, -0.46, 0.7, 0.2, 0.04, 0.16, 0.16);
        head.boxC(Material.BLACK_CONCRETE, 0.42, 0.7, 0.2, 0.04, 0.16, 0.16);
        head.boxC(Material.RED_CONCRETE, 0, 1.08, 0, 0.18, 0.18, 0.5);
        Model halo = head.child(0, 1.45, 0);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            halo.cube(Material.YELLOW_CONCRETE, Math.cos(a) * 0.55, 0, Math.sin(a) * 0.55, 0.14);
        }
        Model wl = c.child(-0.7, 1.8, 0);
        wl.box(Material.YELLOW_CONCRETE, -0.15, -0.8, -0.7, 0.15, 0.9, 1.4);
        Model wr = c.child(0.7, 1.8, 0);
        wr.box(Material.YELLOW_CONCRETE, 0, -0.8, -0.7, 0.15, 0.9, 1.4);
        c.size(1.4).show(0);
        fx.sound("minecraft:entity.lightning_bolt.thunder", 1.5f, 1.2f);
        fx.sound(2, "minecraft:block.beacon.activate", 2f, 0.8f);
        fx.repeat(10, 2, i -> fx.p(Particle.END_ROD, fx.loc(fx.rand(0.5), 1 + i * 1.5, fx.rand(0.5)), 4, 0.1, 0.02));
        double yawR = Math.toRadians(fx.face());
        double ox = -Math.sin(yawR) * 2.2, oz = -Math.cos(yawR) * 2.2;
        fx.anim(2, 26, 1, t -> {
            double flap = Math.sin(t * Math.PI * 10) * 45;
            wl.rot(0, 0, -flap);
            wr.rot(0, 0, flap);
            c.at(ox, 22 * (1 - Fx.ease(t)), oz).apply(1);
            fx.p(Particle.WAX_ON, c.where(fx.rand(1), 1.5, fx.rand(1)), 2, 0.1, 0.02);
        });
        fx.later(30, () -> {
            wl.rot(0, 0, 0);
            wr.rot(0, 0, 0);
            c.apply(4);
            fx.sound("minecraft:entity.chicken.ambient", 3f, 0.5f);
            fx.p(Particle.TOTEM_OF_UNDYING, fx.loc(0, 1.5, 0), 80, 1, 0.5);
        });
        fx.later(42, () -> {
            head.rot(0, 35, 0).apply(3);
            fx.sound("minecraft:entity.chicken.hurt", 3f, 0.5f);
        });
        fx.later(48, () -> head.rot(0, 0, 0).apply(3));
        Model egg = fx.model(ox, 1.2, oz);
        egg.boxC(Material.GOLD_BLOCK, 0, 0.3, 0, 0.45, 0.6, 0.45);
        egg.boxC(Material.GOLD_BLOCK, 0, 0.3, 0, 0.55, 0.4, 0.55);
        fx.later(52, () -> {
            egg.size(0.01).show(0);
            fx.later(1, () -> egg.size(1).apply(4));
            fx.sound("minecraft:entity.chicken.egg", 3f, 0.6f);
            fx.anim(4, 8, 1, t -> egg.at(ox * (1 - t), 1.2 * (1 - t), oz * (1 - t)).rot(0, 0, t * 180).apply(1));
        });
        fx.later(66, () -> {
            fx.lightning(fx.base);
            egg.shatter(0, 12, 2.5, 0.8);
            fx.item(fx.loc(0, 0.8, 0), Material.GOLD_NUGGET, 40, 0.3, 0.25);
            fx.flash(0xFFFFFF);
        });
        fx.anim(76, 24, 1, t -> {
            double flap = Math.sin(t * Math.PI * 14) * 50;
            wl.rot(0, 0, -flap);
            wr.rot(0, 0, flap);
            c.at(ox, 30 * t * t, oz).rot(fx.face(), -20 * t, 0).apply(1);
        });
        fx.later(76, () -> fx.sound("minecraft:entity.chicken.ambient", 3f, 0.6f));
        fx.later(102, c::remove);
    }

    private static void blackHole(Fx fx) {
        Model core = fx.model(0, 1.6, 0);
        core.cube(Material.BLACK_CONCRETE, 0, 0, 0, 0.7);
        core.cube(Material.CRYING_OBSIDIAN, 0, 0, 0, 0.5);
        List<Model> rings = new ArrayList<>();
        Material[] rm = {Material.PURPLE_CONCRETE, Material.MAGENTA_CONCRETE, Material.BLACK_CONCRETE};
        for (int k = 0; k < 3; k++) {
            Model ring = core.child(0, 0, 0).rot(k * 60, 25 + k * 30, 0);
            int n = 10;
            double r = 1.4 + k * 0.5;
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                ring.cube(rm[(i + k) % 3], Math.cos(a) * r, 0, Math.sin(a) * r, 0.22);
            }
            rings.add(ring);
        }
        core.size(0.01).show(1);
        fx.later(2, () -> core.size(1).apply(10));
        fx.sound("minecraft:block.portal.trigger", 1f, 0.5f);
        fx.sound(4, "minecraft:entity.warden.sonic_charge", 1.5f, 0.6f);
        Material ground = fx.groundBlock();
        List<Model> debris = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            double a = fx.rnd.nextDouble() * Math.PI * 2, d = 2.5 + fx.rnd.nextDouble() * 2.5;
            Model b = fx.model(Math.cos(a) * d, 0, Math.sin(a) * d);
            b.cube(i % 4 == 0 ? Material.DIRT : ground, 0, 0.25, 0, 0.5);
            b.show(0);
            debris.add(b);
            double a0 = a, d0 = d;
            int start = 10 + i * 2;
            fx.anim(start, 30, 1, t -> {
                double ang = a0 + t * 6;
                double dist = d0 * (1 - t);
                b.at(Math.cos(ang) * dist, 1.6 * t, Math.sin(ang) * dist).rot(t * 500, t * 300, 0).size(1 - 0.9 * t).apply(1);
            });
            b.vanish(start + 31, 1);
        }
        final double[] spin = {0, 0, 0};
        fx.anim(4, 70, 1, t -> {
            for (int k = 0; k < 3; k++) {
                spin[k] += 18 + k * 7;
                Model r = rings.get(k);
                r.rot(k * 60 + spin[k], 25 + k * 30, 0).size(1 - 0.75 * t);
            }
            core.rot(spin[0], spin[1], 0).size(1 + 0.4 * t).apply(1);
            fx.implode(fx.loc(0, 1.6, 0), Particle.PORTAL, 4, 3, 0.4);
            fx.p(Particle.REVERSE_PORTAL, fx.loc(0, 1.6, 0), 3, 0.3, 0.02);
        });
        fx.later(76, () -> {
            core.size(0.05).apply(4);
            fx.sound("minecraft:block.beacon.deactivate", 2f, 0.5f);
        });
        fx.later(82, () -> {
            core.remove();
            fx.flash(0xC37BFF);
            fx.p(Particle.EXPLOSION_EMITTER, fx.loc(0, 1.6, 0), 1, 0, 0);
            fx.explode(fx.loc(0, 1.6, 0), Particle.END_ROD, 60, 0.6);
            fx.explode(fx.loc(0, 1.6, 0), Particle.WITCH, 40, 0.4);
            fx.sound("minecraft:entity.generic.explode", 2f, 0.5f);
            fx.sound("minecraft:entity.wither.break_block", 1.5f, 0.6f);
            Model burst = fx.model(0, 1.6, 0);
            for (int i = 0; i < 16; i++) burst.cube(rm[i % 3], fx.rand(0.3), fx.rand(0.3), fx.rand(0.3), 0.3);
            burst.apply(0);
            burst.shatter(1, 16, 5, 0.3);
        });
    }
    // ==================================================================== player-model effects (Rig)

    private static final String[] HEART = {".##.##.", "#######", "#######", ".#####.", "..###..", "...#..."};

    private static Model heart(Fx fx, double x, double y, double z, double px) {
        Model h = fx.model(x, y, z).rot(fx.face(), 0, 0);
        h.pixels(Material.RED_CONCRETE, HEART, -3.5 * px, -3 * px, -px / 2, px, px);
        h.pixels(Material.PINK_CONCRETE, new String[]{".#.....", "#......"}, -3.5 * px + px * 0.1, 2 * px, px / 2, px * 0.9, px * 0.1);
        return h;
    }

    private static double yawOff(Fx fx, double deg) {
        return Math.toRadians(fx.face() + deg);
    }

    // ==================================================================== exact player copy (Doll = mannequin with the victim's skin)

    private static Location rel(Fx fx, Location l) {
        return new Location(l.getWorld(), l.getX() - fx.base.getX(), l.getY() - fx.base.getY(), l.getZ() - fx.base.getZ());
    }

    /** A big copy of the victim's head that sits on (and hides) the doll's head. */
    private static Model bigHead(Fx fx, Doll d, double size) {
        Location h = rel(fx, d.headLoc());
        Model m = fx.model(h.getX(), h.getY(), h.getZ()).rot(fx.face(), 0, 0);
        m.head(0, 0, 0, 1.0);
        m.size(size).show(0);
        return m;
    }

    /** Swap the doll for a block copy that can come apart (only for the moment it breaks). */
    private static Rig toRig(Fx fx, Doll d) {
        Rig r = fx.rig(d.x, d.y, d.z);
        r.root.rot(fx.face(), 0, 0).apply(0);
        d.remove();
        return r;
    }

    /** Flying copies of the victim's head (for explosions). */
    private static void headBurst(Fx fx, Location at, int n, double power) {
        Location r = rel(fx, at);
        Model heads = fx.model(r.getX(), r.getY(), r.getZ());
        for (int i = 0; i < n; i++) heads.head(fx.rand(0.3), fx.rand(0.3), fx.rand(0.3), 0.35);
        heads.apply(0);
        heads.shatter(1, 18, power, 0.8);
    }

    private static float mcYaw(Fx fx) {
        return (float) -fx.face();
    }

    private static void helicopter(Fx fx) {
        Doll d = new Doll(fx, 0, 0, 0);
        fx.sound("minecraft:entity.player.hurt", 1f, 0.8f);
        fx.later(8, () -> {
            d.pose(Pose.SWIMMING);
            fx.sound("minecraft:item.armor.equip_iron", 1f, 0.6f);
        });
        final float[] spin = {mcYaw(fx)};
        double dir = yawOff(fx, 90);
        fx.anim(14, 46, 2, t -> {
            spin[0] += 25 + 70 * Math.min(1, t * 3);
            double rise = Math.max(0, t - 0.25);
            double up = 0.6 * Math.min(1, t * 4) + 24 * rise * rise;
            double drift = 16 * Math.pow(Math.max(0, t - 0.5) / 0.5, 2);
            d.at(Math.sin(dir) * drift, up, Math.cos(dir) * drift).look(spin[0], 0);
            if (t < 0.45) fx.p(Particle.CLOUD, fx.loc(0, 0.1, 0), 4, 0.9, 0.05);
        });
        fx.repeat(23, 4, i -> fx.sound("minecraft:entity.phantom.flap", 1.3f, 0.7f + Math.min(0.6f, i * 0.03f)));
        fx.later(108, () -> {
            fx.p(Particle.POOF, d.loc().add(0, 0.5, 0), 25, 0.5, 0.05);
            d.remove();
        });
    }

    private static void bike(Fx fx) {
        double yaw = fx.face() + 90;
        Model b = fx.model(0, 0, 0).rot(yaw, 0, 0);
        Model fw = b.child(0, 0.36, 0.75);
        Model bw = b.child(0, 0.36, -0.75);
        for (Model w : new Model[]{fw, bw}) {
            w.boxC(Material.BLACK_CONCRETE, 0, 0, 0, 0.12, 0.7, 0.3);
            w.boxC(Material.BLACK_CONCRETE, 0, 0, 0, 0.12, 0.3, 0.7);
            w.boxC(Material.BLACK_CONCRETE, 0, 0, 0, 0.12, 0.55, 0.55);
            w.boxC(Material.IRON_BLOCK, 0, 0, 0, 0.14, 0.12, 0.12);
        }
        b.boxC(Material.RED_CONCRETE, 0, 0.6, 0, 0.1, 0.1, 1.3);
        b.boxC(Material.RED_CONCRETE, 0, 0.5, 0.65, 0.08, 0.35, 0.08);
        b.boxC(Material.RED_CONCRETE, 0, 0.5, -0.6, 0.08, 0.35, 0.08);
        b.boxC(Material.GRAY_CONCRETE, 0, 0.98, 0.7, 0.08, 0.5, 0.08);
        b.boxC(Material.BLACK_CONCRETE, 0, 1.22, 0.7, 0.6, 0.06, 0.06);
        b.boxC(Material.BLACK_CONCRETE, 0, 0.78, -0.3, 0.22, 0.06, 0.3);
        b.size(0.01).show(0);
        fx.later(2, () -> b.size(1).apply(5));
        double dir = Math.toRadians(yaw);
        // the doll sits on an invisible seat that drives with the bike
        double seatBack = -0.3, seatY = 0.82;
        Location sl = fx.base.clone().add(Math.sin(dir) * seatBack, seatY, Math.cos(dir) * seatBack);
        sl.setYaw((float) -yaw);
        ItemDisplay seat = fx.world.spawn(sl, ItemDisplay.class, e -> {
            fx.prepareDisplay(e);
            e.setTeleportDuration(2);
        });
        fx.trackEntity(seat);
        Doll d = new Doll(fx, sl.getX() - fx.base.getX(), seatY, sl.getZ() - fx.base.getZ());
        d.look((float) -yaw, 0);
        fx.later(1, () -> d.ride(seat));
        fx.sound("minecraft:entity.player.hurt", 1f, 0.8f);
        fx.repeat(6, 3, i -> fx.p(Particle.CLOUD, fx.loc(fx.rand(0.6), 0.6, fx.rand(0.6)), 6, 0.3, 0.03));
        fx.later(14, () -> fx.sound("minecraft:block.note_block.bell", 1f, 1.5f));
        fx.later(20, () -> fx.sound("minecraft:block.note_block.bell", 1f, 1.5f));
        final double[] wheel = {0};
        fx.anim(26, 30, 2, t -> {
            double dist = 22 * t * t;
            double x = Math.sin(dir) * dist, z = Math.cos(dir) * dist;
            wheel[0] += 30 + 60 * t;
            fw.rot(0, wheel[0], 0);
            bw.rot(0, wheel[0], 0);
            b.at(x, 0, z).apply(2);
            if (seat.isValid()) {
                Location l = fx.base.clone().add(x + Math.sin(dir) * seatBack, seatY, z + Math.cos(dir) * seatBack);
                l.setYaw((float) -yaw);
                seat.teleport(l, TeleportFlag.EntityState.RETAIN_PASSENGERS);
                d.e.setRotation((float) -yaw, 0);
                d.e.setBodyYaw((float) -yaw);
            }
            fx.p(Particle.CLOUD, b.where(0, 0.3, -1.0), 2, 0.1, 0.02);
        });
        fx.repeat(8, 6, i -> fx.sound(26, "minecraft:entity.minecart.riding", 0.6f, 1.2f + i * 0.05f));
        fx.later(88, () -> {
            fx.p(Particle.POOF, b.where(0, 0.8, 0), 25, 0.6, 0.05);
            b.remove();
            d.remove();
            if (seat.isValid()) seat.remove();
        });
    }

    private static void balloon(Fx fx) {
        Doll d = new Doll(fx, 0, 0, 0);
        fx.sound("minecraft:entity.player.hurt", 1f, 0.9f);
        final Model[] head = new Model[1];
        fx.later(8, () -> head[0] = bigHead(fx, d, 0.6));
        fx.anim(10, 20, 2, t -> {
            d.at(0, 0.6 * t, 0);
            if (head[0] != null) {
                Location h = rel(fx, d.headLoc());
                head[0].at(h.getX(), h.getY() + 0.75 * t, h.getZ()).size(0.6 + 1.9 * t).apply(2);
            }
            if ((int) (t * 20) % 4 == 0) fx.sound("minecraft:entity.breeze.inhale", 1f, 0.6f + (float) t);
        });
        fx.anim(50, 10, 2, t -> {
            d.at(fx.rand(0.05), 0.6 + Math.sin(t * Math.PI * 4) * 0.1, fx.rand(0.05));
            if (head[0] != null) head[0].by(0, Math.sin(t * Math.PI * 4) * 0.02, 0).apply(2);
        });
        fx.later(72, () -> {
            if (head[0] == null) return;
            Location at = head[0].where(0, 0, 0);
            head[0].shatter(0, 8, 2.5, 0.2);
            fx.p(Particle.POOF, at, 30, 1.0, 0.05);
            fx.p(Particle.EXPLOSION, at, 2, 0.4, 0);
            fx.sound("minecraft:entity.firework_rocket.blast", 1.5f, 0.8f);
            fx.sound("minecraft:entity.generic.explode", 0.6f, 1.6f);
        });
        fx.anim(74, 6, 1, t -> d.at(0, 0.6 * (1 - t), 0));
        fx.later(80, () -> {
            d.pose(Pose.SLEEPING);
            fx.sound("minecraft:entity.player.attack.knockback", 1f, 0.6f);
        });
        fx.later(100, () -> {
            fx.p(Particle.POOF, d.loc().add(0, 0.3, 0), 20, 0.6, 0.03);
            d.remove();
        });
    }

    private static void coconut(Fx fx) {
        Model tree = root(fx, 0);
        Model palm = tree.child(-1.5, 0, -0.6);
        double[][] trunk = {{0, 0}, {0.08, 0.7}, {0.2, 1.4}, {0.36, 2.1}, {0.55, 2.8}, {0.78, 3.5}, {1.0, 4.1}};
        for (double[] s : trunk) palm.boxC("minecraft:jungle_log[axis=y]", s[0], s[1] + 0.35, 0, 0.42, 0.72, 0.42);
        Model crown = palm.child(1.1, 4.6, 0);
        for (int i = 0; i < 6; i++) {
            Model leaf = crown.child(0, 0, 0).rot(i * 60, 0, -18);
            leaf.box(Material.JUNGLE_LEAVES, 0, -0.1, -0.25, 1.8, 0.2, 0.5);
            leaf.box(Material.JUNGLE_LEAVES, 1.5, -0.4, -0.2, 0.8, 0.2, 0.4);
        }
        crown.cube(Material.BROWN_TERRACOTTA, 0.2, -0.2, 0.2, 0.32);
        crown.cube(Material.BROWN_TERRACOTTA, -0.2, -0.25, 0.1, 0.32);
        tree.size(0.01).show(1);
        fx.later(2, () -> tree.size(1).apply(12));
        fx.sound("minecraft:block.bamboo.place", 1f, 0.6f);
        Doll d = new Doll(fx, 0, 0, 0);
        fx.later(10, () -> d.look(mcYaw(fx), -40));
        Model nut = fx.model(0, 6, 0);
        nut.cube(Material.BROWN_TERRACOTTA, 0, 0, 0, 0.4);
        nut.cube(Material.BROWN_CONCRETE, 0, 0.12, 0.14, 0.13);
        fx.later(22, () -> {
            nut.show(0);
            fx.sound("minecraft:block.azalea_leaves.break", 1f, 0.8f);
        });
        fx.anim(24, 9, 1, t -> nut.at(0, 6 - 3.9 * t * t, 0).rot(0, 0, 300 * t).apply(1));
        fx.later(33, () -> {
            fx.sound("minecraft:block.wood.hit", 1.5f, 1.4f);
            fx.sound("minecraft:block.note_block.bass", 1.5f, 1.9f);
            fx.p(Particle.CRIT, fx.loc(0, 2, 0), 20, 0.3, 0.2);
            d.look(mcYaw(fx), 40);
        });
        double away = yawOff(fx, 120);
        fx.anim(33, 10, 1, t -> nut.at(Math.sin(away) * 2 * t, 2.1 + Math.sin(t * Math.PI) * 1.2 - 1.9 * t, Math.cos(away) * 2 * t).rot(0, 0, 300 + 400 * t).apply(1));
        fx.later(38, () -> {
            d.pose(Pose.SLEEPING);
            fx.sound("minecraft:entity.player.attack.knockback", 1f, 0.6f);
            fx.block(fx.loc(0, 0.1, 0), fx.groundBlock(), 25, 0.6, 0.1);
        });
        fx.repeat(14, 3, i -> {
            double a = i * 0.9;
            fx.p(Particle.WAX_ON, d.loc().add(Math.cos(a) * 0.4, 0.6, Math.sin(a) * 0.4), 1, 0, 0);
        });
        nut.vanish(70, 4);
        fx.later(76, () -> tree.size(0.01).apply(10));
        fx.later(84, () -> {
            fx.p(Particle.POOF, d.loc().add(0, 0.3, 0), 20, 0.6, 0.03);
            d.remove();
        });
        fx.later(90, tree::remove);
    }

    private static final String[] EZ_PIX = {
            "#####.#####",
            "#.........#",
            "#........#.",
            "####....#..",
            "#......#...",
            "#.....#....",
            "#####.#####"};

    private static void ez(Fx fx) {
        Doll d = new Doll(fx, 0, 0, 0);
        fx.sound("minecraft:entity.player.hurt", 1f, 0.8f);
        Model letters = fx.model(0, 0, 0).rot(fx.face(), 0, 0);
        List<Model> px = new ArrayList<>();
        List<double[]> target = new ArrayList<>();
        double s = 0.3;
        double w = EZ_PIX[0].length() * s;
        for (int r = 0; r < EZ_PIX.length; r++) {
            for (int c = 0; c < EZ_PIX[r].length(); c++) {
                if (EZ_PIX[r].charAt(c) != '#') continue;
                Model m = letters.child(0, 1.0, 0);
                m.head(0, 0, 0, s);
                px.add(m);
                target.add(new double[]{-w / 2 + (c + 0.5) * s, 0.5 + (EZ_PIX.length - 1 - r) * s});
            }
        }
        fx.later(10, () -> {
            fx.p(Particle.POOF, d.loc().add(0, 1, 0), 20, 0.3, 0.05);
            fx.sound("minecraft:entity.item.pickup", 1f, 0.6f);
            d.remove();
            letters.size(1).show(0);
        });
        for (int i = 0; i < px.size(); i++) {
            Model m = px.get(i);
            double[] tg = target.get(i);
            fx.anim(12 + i / 3, 10, 1, t -> m.at(tg[0] * Fx.ease(t), 1.0 + (tg[1] - 1.0) * Fx.ease(t), 0).rot(360 * (1 - t), 0, 0).apply(1));
            if (i % 4 == 0) fx.sound(12 + i / 3, "minecraft:block.note_block.hat", 0.6f, 1.2f + i * 0.02f);
        }
        fx.later(36, () -> {
            fx.sound("minecraft:entity.player.levelup", 1f, 1.3f);
            fx.sound("minecraft:entity.villager.yes", 1f, 1f);
            fx.p(Particle.HAPPY_VILLAGER, fx.loc(0, 1.5, 0), 25, 1.4, 0.1);
        });
        fx.anim(40, 22, 2, t -> letters.at(0, Math.sin(t * Math.PI * 3) * 0.12, 0).apply(2));
        fx.later(88, () -> {
            letters.shatter(0, 10, 2.0, 0.6);
            fx.sound("minecraft:entity.item.pickup", 1f, 0.8f);
        });
    }

    private static void explode(Fx fx) {
        Doll d = new Doll(fx, 0, 0, 0);
        fx.sound("minecraft:entity.tnt.primed", 1f, 1f);
        fx.anim(6, 24, 1, t -> {
            d.scale(1 + 0.25 * t).at(fx.rand(0.05 * t), 0, fx.rand(0.05 * t));
            fx.p(Particle.SMOKE, fx.loc(0, 1.0, 0), 2, 0.25, 0.01);
        });
        fx.later(31, () -> {
            Rig r = toRig(fx, d);
            r.root.size(1.25).apply(0);
            r.root.shatter(1, 16, 3.5, 0.9);
            headBurst(fx, fx.loc(0, 1.7, 0), 3, 3.0);
            fx.p(Particle.EXPLOSION_EMITTER, fx.loc(0, 1, 0), 1, 0, 0);
            fx.p(Particle.FLAME, fx.loc(0, 1, 0), 50, 0.6, 0.15);
            fx.p(Particle.LARGE_SMOKE, fx.loc(0, 1, 0), 30, 0.6, 0.08);
            fx.sound("minecraft:entity.generic.explode", 1.5f, 1f);
            fx.sound("minecraft:entity.firework_rocket.large_blast", 1f, 0.8f);
        });
        fx.repeat(8, 2, i -> fx.p(Particle.FLAME, fx.loc(fx.rand(1.5), 1 + fx.rand(1), fx.rand(1.5)), 4, 0.2, 0.02));
    }

    private static void dismantle(Fx fx) {
        Doll d = new Doll(fx, 0, 0, 0);
        fx.sound("minecraft:entity.player.hurt", 1f, 0.7f);
        fx.anim(6, 10, 1, t -> d.at(0, 0.3 * Fx.ease(t), 0));
        fx.anim(16, 8, 1, t -> d.at(fx.rand(0.06), 0.3, fx.rand(0.06)));
        fx.later(10, () -> fx.sound("minecraft:block.chain.place", 1f, 0.6f));
        fx.later(24, () -> {
            Rig r = toRig(fx, d);
            r.root.at(0, 0.3, 0).apply(0);
            double p = Rig.PX, aw = r.skin.slim ? 3 : 4;
            fx.anim(1, 14, 1, t -> {
                double e = Fx.ease(t);
                r.head.at(0, 12 * p + 0.7 * e, 0).rot(25 * e, 0, 0);
                r.rarm.at(-(4 + aw / 2) * p - 0.75 * e, 12 * p + 0.15 * e, 0).rot(0, 0, -50 * e);
                r.larm.at((4 + aw / 2) * p + 0.75 * e, 12 * p + 0.15 * e, 0).rot(0, 0, 50 * e);
                r.rleg.at(-2 * p - 0.45 * e, 12 * p - 0.55 * e, 0).rot(0, 0, -25 * e);
                r.lleg.at(2 * p + 0.45 * e, 12 * p - 0.55 * e, 0).rot(0, 0, 25 * e);
                r.apply(1);
                if ((int) (t * 14) % 5 == 0) fx.sound("minecraft:block.chain.break", 0.8f, 0.6f + (float) t);
            });
            Model[] order = {r.lleg, r.rleg, r.larm, r.rarm, r.body, r.head};
            for (int i = 0; i < order.length; i++) {
                Model m = order[i];
                fx.later(17 + i * 5, () -> {
                    Location c = m.where(0, m == r.head ? 4 * p : -6 * p, 0);
                    fx.p(Particle.END_ROD, c, 12, 0.15, 0.06);
                    fx.p(Particle.FIREWORK, c, 6, 0.1, 0.05);
                    fx.sound("minecraft:block.amethyst_block.break", 1f, 1.2f);
                    m.shatter(0, 5, 0.6, 0.4);
                });
            }
        });
    }

    private static void awoooga(Fx fx) {
        Doll d = new Doll(fx, 0, 0, 0);
        for (int hop = 0; hop < 2; hop++) {
            int at = 6 + hop * 12;
            fx.anim(at, 8, 1, t -> d.at(0, Math.sin(t * Math.PI) * 0.8, 0));
            fx.later(at, () -> fx.sound("minecraft:item.goat_horn.sound.1", 1.5f, 1.4f));
        }
        final Model[] head = new Model[1];
        fx.later(30, () -> head[0] = bigHead(fx, d, 0.6));
        fx.anim(31, 14, 1, t -> {
            if (head[0] != null) head[0].size(0.6 + 0.5 * Math.abs(Math.sin(t * Math.PI * 3))).apply(1);
        });
        fx.later(31, () -> fx.sound("minecraft:item.goat_horn.sound.1", 2f, 1.0f));
        for (int i = 0; i < 4; i++) {
            int k = i;
            fx.later(34 + i * 4, () -> {
                Location h = rel(fx, d.headLoc());
                Model ht = heart(fx, h.getX(), h.getY(), h.getZ(), 0.09);
                ht.size(0.3).show(0);
                double ang = yawOff(fx, -60 + k * 40);
                double sx = ht.lpos.x, sy = ht.lpos.y, sz = ht.lpos.z;
                fx.anim(1, 14, 1, t -> ht.at(sx + Math.sin(ang) * 1.6 * t, sy + 1.8 * t, sz + Math.cos(ang) * 1.6 * t).size(0.3 + 0.9 * t).apply(1));
                ht.shatter(18, 6, 0.8, 0.3);
                fx.sound("minecraft:entity.experience_orb.pickup", 1f, 1.2f + k * 0.1f);
            });
        }
        fx.later(56, () -> {
            if (head[0] != null) head[0].vanish(0, 3);
            d.pose(Pose.SLEEPING);
            fx.p(Particle.HEART, d.loc().add(0, 0.6, 0), 10, 0.5, 0.05);
            fx.sound("minecraft:entity.player.attack.knockback", 1f, 0.6f);
        });
        fx.later(88, () -> {
            fx.p(Particle.POOF, d.loc().add(0, 0.3, 0), 20, 0.6, 0.03);
            d.remove();
        });
    }

    private static void iLoveYou(Fx fx) {
        Doll d = new Doll(fx, 0, 0, 0);
        fx.sound("minecraft:block.amethyst_block.chime", 1f, 0.8f);
        Model h = heart(fx, 0, 1.0, 0, 0.26);
        h.size(0.01).show(0);
        final float[] spin = {mcYaw(fx)};
        fx.anim(10, 14, 1, t -> {
            spin[0] += 20 + 40 * t;
            d.scale(1 - 0.93 * t).at(0, 0.8 * t, 0).look(spin[0], 0);
            h.size(1.0 * t).apply(1);
            fx.p(Particle.HEART, fx.loc(fx.rand(0.6), 1 + fx.rand(0.6), fx.rand(0.6)), 1, 0, 0);
        });
        fx.later(25, d::remove);
        for (int b = 0; b < 3; b++) {
            int at = 28 + b * 9;
            fx.later(at, () -> {
                h.size(1.25).apply(2);
                fx.sound("minecraft:block.note_block.bass", 1.5f, 0.7f);
            });
            fx.later(at + 3, () -> {
                h.size(1.0).apply(4);
                fx.sound("minecraft:block.note_block.bass", 1.2f, 0.6f);
            });
        }
        fx.anim(58, 18, 1, t -> h.at(0, 1.0 + 4 * t * t, 0).rot(fx.face() + Math.sin(t * 12) * 15, 0, 0).apply(1));
        fx.later(77, () -> {
            Location at = h.where(0, 0, 0);
            h.shatter(0, 10, 2.0, 0.4);
            fx.p(Particle.HEART, at, 20, 1.0, 0.1);
            fx.dust(at, 0xFF4D6D, 1.6f, 40, 1.2);
            fx.sound("minecraft:entity.firework_rocket.twinkle", 1f, 1.4f);
            fx.sound("minecraft:block.amethyst_block.break", 1f, 1.2f);
        });
    }

    private static void tickTock(Fx fx) {
        Doll d = new Doll(fx, 0, 0, 0);
        Model clock = fx.model(0, 3.0, 0).rot(fx.face(), 0, 0);
        clock.boxC(Material.QUARTZ_BLOCK, 0, 0, 0, 1.5, 1.5, 0.12);
        clock.boxC(Material.QUARTZ_BLOCK, 0, 0, 0, 1.8, 1.0, 0.12);
        clock.boxC(Material.QUARTZ_BLOCK, 0, 0, 0, 1.0, 1.8, 0.12);
        clock.boxC(Material.GOLD_BLOCK, 0, 0.95, -0.02, 1.1, 0.12, 0.14);
        clock.boxC(Material.GOLD_BLOCK, 0, -0.95, -0.02, 1.1, 0.12, 0.14);
        clock.boxC(Material.GOLD_BLOCK, 0.95, 0, -0.02, 0.12, 1.1, 0.14);
        clock.boxC(Material.GOLD_BLOCK, -0.95, 0, -0.02, 0.12, 1.1, 0.14);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            clock.boxC(Material.BLACK_CONCRETE, Math.sin(a) * 0.68, Math.cos(a) * 0.68, 0.07, 0.07, 0.07, 0.03);
        }
        Model hour = clock.child(0, 0, 0.09);
        hour.box(Material.BLACK_CONCRETE, -0.04, 0, -0.015, 0.08, 0.38, 0.03);
        Model minute = clock.child(0, 0, 0.11);
        minute.box(Material.BLACK_CONCRETE, -0.03, 0, -0.015, 0.06, 0.6, 0.03);
        clock.cube(Material.GOLD_BLOCK, 0, 0, 0.12, 0.08);
        clock.size(0.01).show(0);
        fx.later(2, () -> clock.size(1).apply(8));
        fx.sound("minecraft:block.bell.use", 1f, 1.2f);
        final double[] m = {0}, hr = {0};
        fx.anim(12, 30, 2, t -> {
            m[0] -= 40 + 60 * t;
            hr[0] -= (40 + 60 * t) / 12;
            minute.rot(0, 0, m[0]);
            hour.rot(0, 0, hr[0]);
            clock.apply(2);
            fx.sound("minecraft:block.note_block.hat", 0.8f, (int) (t * 30) % 2 == 0 ? 1.4f : 1.1f);
        });
        fx.later(26, () -> {
            d.pose(Pose.SNEAKING);
            d.look(mcYaw(fx), 35);
        });
        fx.anim(30, 12, 2, t -> d.scale(1 - 0.15 * t));
        fx.later(56, () -> {
            Rig r = toRig(fx, d);
            r.root.size(0.85);
            r.body.rot(0, 25, 0);
            r.head.rot(0, 30, 0);
            r.apply(0);
            r.recolor(Material.LIGHT_GRAY_CONCRETE);
            fx.sound("minecraft:block.sand.break", 1f, 0.6f);
            fx.later(8, () -> r.recolor(Material.GRAY_CONCRETE_POWDER));
            fx.later(16, () -> {
                r.root.shatter(0, 12, 0.7, -0.6);
                fx.block(fx.loc(0, 1.0, 0), Material.GRAVEL, 60, 0.4, 0.05);
                fx.p(Particle.ASH, fx.loc(0, 1.2, 0), 40, 0.5, 0.05);
                fx.sound("minecraft:block.gravel.break", 1.5f, 0.6f);
                fx.sound("minecraft:block.bell.resonate", 1f, 0.6f);
                clock.shatter(4, 10, 1.6, 0.4);
            });
        });
    }

    private static void meteor(Fx fx) {
        Doll d = new Doll(fx, 0, 0, 0);
        fx.later(8, () -> {
            d.look(mcYaw(fx), -50);
            fx.sound("minecraft:entity.villager.no", 1f, 0.7f);
        });
        double back = 14, high = 22;
        double yawR = yawOff(fx, 150);
        double sx = Math.sin(yawR) * back, sz = Math.cos(yawR) * back;
        Model rock = fx.model(sx, high, sz);
        Material[] mats = {Material.MAGMA_BLOCK, Material.NETHERRACK, Material.BLACKSTONE, Material.MAGMA_BLOCK};
        rock.cube(Material.MAGMA_BLOCK, 0, 0, 0, 1.3);
        for (int i = 0; i < 10; i++) rock.cube(mats[i % mats.length], fx.rand(0.65), fx.rand(0.65), fx.rand(0.65), 0.5 + fx.rnd.nextDouble() * 0.35);
        fx.later(8, () -> rock.show(0));
        fx.sound(8, "minecraft:entity.ghast.shoot", 2f, 0.5f);
        fx.anim(10, 20, 1, t -> {
            double k = t * t;
            rock.at(sx * (1 - k), high * (1 - k) + 1.0, sz * (1 - k)).rot(t * 300, t * 200, 0).apply(1);
            Location c = rock.where(0, 0, 0);
            fx.p(Particle.FLAME, c, 10, 0.4, 0.03);
            fx.p(Particle.LARGE_SMOKE, c, 4, 0.4, 0.02);
        });
        fx.later(30, () -> {
            Rig r = toRig(fx, d);
            r.root.shatter(1, 12, 2.5, 0.5);
            headBurst(fx, fx.loc(0, 1.6, 0), 1, 2.5);
            fx.flash(0xFF8A30);
            fx.p(Particle.EXPLOSION_EMITTER, fx.loc(0, 0.6, 0), 1, 0, 0);
            for (int i = 0; i < 4; i++) fx.p(Particle.CLOUD, fx.loc(0, 0.5 + i * 0.4, 0), 40, 1.2 + i * 0.3, 0.06);
            fx.p(Particle.POOF, fx.loc(0, 0.5, 0), 40, 1.5, 0.1);
            fx.sound("minecraft:entity.generic.explode", 2f, 0.7f);
            Model crater = fx.model(0, 0, 0);
            Material ground = fx.groundBlock();
            for (int i = 0; i < 14; i++) {
                double a = i * Math.PI / 7;
                crater.cube(i % 3 == 0 ? Material.NETHERRACK : ground, Math.cos(a) * 1.2, 0.2, Math.sin(a) * 1.2, 0.4 + fx.rnd.nextDouble() * 0.3);
            }
            crater.apply(0);
            crater.shatter(1, 16, 4.5, 1.3);
            rock.at(0, 0.0, 0).apply(2);
        });
        fx.repeat(20, 3, i -> fx.p(Particle.CAMPFIRE_COSY_SMOKE, fx.loc(fx.rand(0.5), 0.8, fx.rand(0.5)), 1, 0.1, 0.03));
        fx.later(80, () -> rock.size(0.01).at(0, -0.8, 0).apply(18));
        fx.later(100, rock::remove);
    }

    private static void heartAttack(Fx fx) {
        Doll d = new Doll(fx, 0, 0, 0);
        fx.later(6, () -> {
            d.pose(Pose.SNEAKING);
            d.look(mcYaw(fx), 30);
        });
        int[] beats = {8, 18, 26, 32, 37, 41};
        for (int i = 0; i < beats.length; i++) {
            float pitch = 0.6f + i * 0.08f;
            fx.later(beats[i], () -> {
                fx.sound("minecraft:block.note_block.basedrum", 1.5f, pitch);
                fx.p(Particle.HEART, d.loc().add(0, 1.1, 0), 2, 0.1, 0.02);
                d.scale(1.05);
            });
            fx.later(beats[i] + 2, () -> d.scale(1));
        }
        Model h = heart(fx, 0, 1.1, 0, 0.11);
        fx.later(44, () -> {
            double fz = 0.3;
            double yr = Math.toRadians(fx.face());
            h.at(Math.sin(yr) * fz, 1.1, Math.cos(yr) * fz);
            h.size(0.3).show(0);
            fx.sound("minecraft:entity.player.hurt", 1f, 0.6f);
        });
        fx.anim(46, 10, 1, t -> h.by(0, 0.08, 0).size(0.3 + 0.9 * t).apply(1));
        fx.later(58, () -> {
            h.shatter(0, 10, 1.5, 0.2);
            fx.sound("minecraft:block.glass.break", 1f, 0.8f);
            fx.dust(fx.loc(0, 1.6, 0), 0xC0262B, 1.5f, 30, 0.5);
        });
        fx.later(62, () -> {
            d.pose(Pose.SLEEPING);
            fx.sound("minecraft:block.note_block.basedrum", 1f, 0.5f);
            fx.sound("minecraft:entity.player.attack.knockback", 1f, 0.5f);
            fx.block(fx.loc(0, 0.1, 0), fx.groundBlock(), 25, 0.6, 0.1);
        });
        fx.later(92, () -> {
            fx.p(Particle.POOF, d.loc().add(0, 0.3, 0), 20, 0.6, 0.03);
            d.remove();
        });
    }
}
