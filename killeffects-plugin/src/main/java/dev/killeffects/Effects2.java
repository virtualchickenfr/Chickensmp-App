package dev.killeffects;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Pose;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 50 more kill effects (v3). Every one uses the victim's real skin (Doll = mannequin with their profile,
 * Rig = block copy in their skin colours for the moment the body breaks apart) and only the
 * EconomyCore palette for colours.
 */
final class Effects2 {
    private Effects2() {}

    // EconomyCore palette
    static final int G = 0x00FC00, B = 0x00A4FC, Y = 0xFCE300, R = 0xFC0000, P = 0xFC00E3, PU = 0xA303F9, C = 0x1EB8E9, M = 0x00FC99, O = 0xF97603, W = 0xFFFFFF;
    static final int[] ALL = {G, B, Y, R, P, PU, C, M, O};
    static final Material[] BLOCKS = {Material.LIME_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.YELLOW_CONCRETE, Material.RED_CONCRETE,
            Material.MAGENTA_CONCRETE, Material.PURPLE_CONCRETE, Material.CYAN_CONCRETE, Material.LIME_CONCRETE, Material.ORANGE_CONCRETE};
    private static final LegacyComponentSerializer LEG = LegacyComponentSerializer.builder().character('&').hexColors().build();

    static Map<String, KillEffect> all() {
        List<KillEffect> l = new ArrayList<>();
        // ---------------------------------------------------------------- common
        l.add(new KillEffect("spin_out", "Spin Out", Rarity.COMMON, "Spins faster and faster into the ground.", Material.LIME_CONCRETE, Effects2::spinOut));
        l.add(new KillEffect("shrink_ray", "Shrink Ray", Rarity.COMMON, "Zapped down to pocket size.", Material.LIGHT_BLUE_CONCRETE, Effects2::shrinkRay));
        l.add(new KillEffect("float_away", "Float Away", Rarity.COMMON, "Floats off into the sky.", Material.FEATHER, Effects2::floatAway));
        l.add(new KillEffect("quicksand", "Quicksand", Rarity.COMMON, "Slowly swallowed by the ground.", Material.SAND, Effects2::quicksand));
        l.add(new KillEffect("bouncy", "Bouncy", Rarity.COMMON, "Boing, boing, boing... flop.", Material.SLIME_BALL, Effects2::bouncy));
        l.add(new KillEffect("nap_time", "Nap Time", Rarity.COMMON, "Time for a little nap. Zzz.", Material.WHITE_BED, Effects2::napTime));
        l.add(new KillEffect("dizzy", "Dizzy", Rarity.COMMON, "Seeing stars.", Material.YELLOW_CONCRETE, Effects2::dizzy));
        l.add(new KillEffect("crouch_spam", "Crouch Spam", Rarity.COMMON, "The classic victory dance.", Material.LEATHER_BOOTS, Effects2::crouchSpam));
        l.add(new KillEffect("confetti_pop", "Confetti Pop", Rarity.COMMON, "Pops into colourful confetti.", Material.MAGENTA_CONCRETE, Effects2::confettiPop));
        l.add(new KillEffect("gg", "GG", Rarity.COMMON, "Good game. Well played.", Material.LIME_DYE, Effects2::gg));
        // ---------------------------------------------------------------- uncommon
        l.add(new KillEffect("freeze_frame", "Freeze Frame", Rarity.UNCOMMON, "Frozen in a block of glass.", Material.LIGHT_BLUE_STAINED_GLASS, Effects2::freezeFrame));
        l.add(new KillEffect("caged", "Caged", Rarity.UNCOMMON, "Locked up for good.", Material.IRON_BARS, Effects2::caged));
        l.add(new KillEffect("laser", "Laser Beam", Rarity.UNCOMMON, "A red beam from above.", Material.RED_STAINED_GLASS, Effects2::laser));
        l.add(new KillEffect("giant", "Giant", Rarity.UNCOMMON, "Grows huge... and pops.", Material.LIME_CONCRETE, Effects2::giant));
        l.add(new KillEffect("rewind", "Rewind", Rarity.UNCOMMON, "Played backwards out of existence.", Material.CLOCK, Effects2::rewind));
        l.add(new KillEffect("glitch", "Glitch", Rarity.UNCOMMON, "Lagging out of the game.", Material.MAGENTA_STAINED_GLASS, Effects2::glitch));
        l.add(new KillEffect("bubble", "Bubble", Rarity.UNCOMMON, "Trapped in a floating bubble.", Material.LIGHT_BLUE_STAINED_GLASS, Effects2::bubble));
        l.add(new KillEffect("spotlight", "Spotlight", Rarity.UNCOMMON, "One last spin in the spotlight.", Material.YELLOW_STAINED_GLASS, Effects2::spotlight));
        l.add(new KillEffect("ground_slam", "Ground Slam", Rarity.UNCOMMON, "Up high, then slammed down.", Material.ORANGE_CONCRETE, Effects2::groundSlam));
        l.add(new KillEffect("rain_cloud", "Rain Cloud", Rarity.UNCOMMON, "A sad little cloud.", Material.WHITE_WOOL, Effects2::rainCloud));
        // ---------------------------------------------------------------- rare
        l.add(new KillEffect("vortex", "Vortex", Rarity.RARE, "Sucked into a purple vortex.", Material.PURPLE_CONCRETE, Effects2::vortex));
        l.add(new KillEffect("teleported", "Teleported", Rarity.RARE, "Flickers and is gone.", Material.ENDER_PEARL, Effects2::teleported));
        l.add(new KillEffect("gift_wrapped", "Gift Wrapped", Rarity.RARE, "Wrapped up as a present.", Material.RED_CONCRETE, Effects2::giftWrapped));
        l.add(new KillEffect("rocket_ride", "Rocket Ride", Rarity.RARE, "A one-way ticket to space.", Material.FIREWORK_ROCKET, Effects2::rocketRide));
        l.add(new KillEffect("trophy", "Trophy", Rarity.RARE, "Put on the shelf as a trophy.", Material.YELLOW_CONCRETE, Effects2::trophy));
        l.add(new KillEffect("washing_machine", "Washing Machine", Rarity.RARE, "Spin cycle. 1400 rpm.", Material.CYAN_CONCRETE, Effects2::washingMachine));
        l.add(new KillEffect("popcorn", "Popcorn", Rarity.RARE, "Pop, pop, pop... buried in popcorn.", Material.WHITE_CONCRETE, Effects2::popcorn));
        l.add(new KillEffect("magnet", "Magnet", Rarity.RARE, "Pulled away by a giant magnet.", Material.RED_CONCRETE, Effects2::magnet));
        l.add(new KillEffect("portal", "Portal", Rarity.RARE, "Falls through a portal.", Material.PURPLE_STAINED_GLASS, Effects2::portal));
        l.add(new KillEffect("dance_floor", "Dance Floor", Rarity.RARE, "One last dance.", Material.JUKEBOX, Effects2::danceFloor));
        // ---------------------------------------------------------------- epic
        l.add(new KillEffect("hologram", "Hologram", Rarity.EPIC, "Was it ever real?", Material.LIGHT_BLUE_STAINED_GLASS_PANE, Effects2::hologram));
        l.add(new KillEffect("pinata", "Pinata", Rarity.EPIC, "Hit it until the candy comes out.", Material.MAGENTA_WOOL, Effects2::pinata));
        l.add(new KillEffect("thunder_strike", "Thunder Strike", Rarity.EPIC, "Struck three times.", Material.LIGHTNING_ROD, Effects2::thunderStrike));
        l.add(new KillEffect("statue", "Statue", Rarity.EPIC, "Turned to stone, then crumbles.", Material.STONE, Effects2::statue));
        l.add(new KillEffect("gold_statue", "Gold Statue", Rarity.EPIC, "A golden copy. Briefly.", Material.YELLOW_CONCRETE, Effects2::goldStatue));
        l.add(new KillEffect("star_shower", "Star Shower", Rarity.EPIC, "Stars rain down from the sky.", Material.NETHER_STAR, Effects2::starShower));
        l.add(new KillEffect("big_slap", "Big Slap", Rarity.EPIC, "One giant slap.", Material.PINK_CONCRETE, Effects2::bigSlap));
        l.add(new KillEffect("knockout", "K.O.", Rarity.EPIC, "Knocked out cold.", Material.RED_WOOL, Effects2::knockout));
        // ---------------------------------------------------------------- legendary
        l.add(new KillEffect("dragon_breath", "Dragon Breath", Rarity.LEGENDARY, "Burned away by purple fire.", Material.DRAGON_BREATH, Effects2::dragonBreath));
        l.add(new KillEffect("blender", "Blender", Rarity.LEGENDARY, "Blended into a smoothie.", Material.GLASS, Effects2::blender));
        l.add(new KillEffect("money_rain", "Money Rain", Rarity.LEGENDARY, "Buried in money.", Material.EMERALD, Effects2::moneyRain));
        l.add(new KillEffect("ascension", "Ascension", Rarity.LEGENDARY, "Rises up with a halo.", Material.GLOWSTONE, Effects2::ascension));
        l.add(new KillEffect("shattered", "Shattered", Rarity.LEGENDARY, "Turns to glass and shatters.", Material.LIGHT_BLUE_STAINED_GLASS, Effects2::shattered));
        l.add(new KillEffect("gladiator", "Gladiator", Rarity.LEGENDARY, "The crowd gives a thumbs down.", Material.IRON_SWORD, Effects2::gladiator));
        l.add(new KillEffect("zero_gravity", "Zero Gravity", Rarity.LEGENDARY, "Gravity just turned off.", Material.PHANTOM_MEMBRANE, Effects2::zeroGravity));
        // ---------------------------------------------------------------- mythic
        l.add(new KillEffect("prism", "Prism", Rarity.MYTHIC, "Split into every colour.", Material.AMETHYST_CLUSTER, Effects2::prism));
        l.add(new KillEffect("clone_army", "Clone Army", Rarity.MYTHIC, "Six clones say goodbye.", Material.ARMOR_STAND, Effects2::cloneArmy));
        l.add(new KillEffect("judgement", "Judgement", Rarity.MYTHIC, "The hammer has spoken.", Material.MACE, Effects2::judgement));
        l.add(new KillEffect("time_stop", "Time Stop", Rarity.MYTHIC, "Time stops. Then they break.", Material.CLOCK, Effects2::timeStop));
        l.add(new KillEffect("nova", "Nova", Rarity.MYTHIC, "Charged up, then a colour nova.", Material.BEACON, Effects2::nova));
        Map<String, KillEffect> map = new LinkedHashMap<>();
        for (KillEffect e : l) map.put(e.id(), e);
        return map;
    }

    // ==================================================================== helpers

    private static Doll doll(Fx fx) {
        Doll d = new Doll(fx, 0, 0, 0);
        fx.sound("minecraft:entity.player.hurt", 1f, 0.9f);
        return d;
    }

    private static float mcYaw(Fx fx) { return (float) -fx.face(); }

    private static Location rel(Fx fx, Location l) {
        return new Location(l.getWorld(), l.getX() - fx.base.getX(), l.getY() - fx.base.getY(), l.getZ() - fx.base.getZ());
    }

    private static int pick(Fx fx) { return ALL[fx.rnd.nextInt(ALL.length)]; }

    private static Material block(Fx fx) { return BLOCKS[fx.rnd.nextInt(BLOCKS.length)]; }

    /** Coloured dust cloud. */
    private static void cloud(Fx fx, Location at, int color, int n, double spread) {
        fx.dust(at, color, 1.4f, n, spread);
    }

    /** Multi-colour confetti. */
    private static void confetti(Fx fx, Location at, int n, double spread) {
        for (int c : ALL) fx.dust(at, c, 1.2f, Math.max(1, n / ALL.length), spread);
    }

    /** A ring of dust that grows outwards. */
    private static void shock(Fx fx, double y, double from, double to, int color) {
        fx.expandingRing(y, from, to, 8, 28, l -> fx.dust(l, color, 1.6f, 1, 0));
    }

    /** Puff + coloured dust, then the doll is gone. */
    private static void poof(Fx fx, Doll d, int color) {
        Location at = d.loc().add(0, 1 * d.scaleNow(), 0);
        fx.p(Particle.POOF, at, 25, 0.4 * d.scaleNow(), 0.04);
        cloud(fx, at, color, 30, 0.5 * d.scaleNow());
        fx.sound("minecraft:entity.chicken.egg", 1f, 0.7f);
        d.remove();
    }

    /** Block copy of the doll in the victim's skin colours (to break apart). */
    private static Rig toRig(Fx fx, Doll d) {
        Rig r = fx.rig(d.x, d.y, d.z);
        r.root.rot(fx.face(), 0, 0).size(d.scaleNow()).apply(0);
        d.remove();
        return r;
    }

    /** Floating text in EconomyCore colours. */
    private static TextDisplay text(Fx fx, String legacy, double x, double y, double z, float size) {
        Location l = fx.loc(x, y, z);
        TextDisplay t = fx.world.spawn(l, TextDisplay.class, d -> {
            fx.prepareDisplay(d);
            d.setBillboard(Display.Billboard.CENTER);
            d.text(LEG.deserialize(legacy));
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(true);
            d.setSeeThrough(false);
            d.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.01f, 0.01f, 0.01f), new Quaternionf()));
        });
        fx.trackEntity(t);
        fx.later(2, () -> textTo(t, 0, size, 6));
        return t;
    }

    private static void textTo(TextDisplay t, double dy, float size, int dur) {
        if (t == null || !t.isValid()) return;
        t.setInterpolationDelay(0);
        t.setInterpolationDuration(dur);
        t.setTransformation(new Transformation(new Vector3f(0, (float) dy, 0), new Quaternionf(), new Vector3f(size, size, size), new Quaternionf()));
    }

    private static void textGone(Fx fx, TextDisplay t, int delay) {
        fx.later(delay, () -> textTo(t, 0.6, 0.01f, 8));
        fx.later(delay + 9, () -> { if (t.isValid()) t.remove(); });
    }

    private static void flashAt(Fx fx, Location at) {
        fx.particle(Particle.FLASH, at, 1, 0, 0, 0, 0, Fx.color(W));
    }

    private static Model root(Fx fx, double y) {
        return fx.model(0, y, 0).rot(fx.face(), 0, 0);
    }

    // ==================================================================== common

    private static void spinOut(Fx fx) {
        Doll d = doll(fx);
        float[] yaw = {mcYaw(fx)};
        fx.anim(4, 40, 1, t -> {
            yaw[0] += 8 + 50 * t;
            d.at(0, -1.9 * Math.max(0, t - 0.4) / 0.6, 0).look(yaw[0], 0);
            if ((int) (t * 40) % 3 == 0) {
                fx.dust(fx.loc(0, 0.1, 0), G, 1.2f, 6, 0.5);
                fx.block(fx.loc(0, 0.1, 0), fx.groundBlock(), 4, 0.3, 0.1);
            }
            if ((int) (t * 40) % 6 == 0) fx.sound("minecraft:entity.player.attack.sweep", 0.6f, 0.6f + (float) t);
        });
        fx.later(46, () -> { cloud(fx, fx.loc(0, 0.2, 0), G, 30, 0.5); d.remove(); fx.sound("minecraft:block.rooted_dirt.break", 1f, 0.6f); });
    }

    private static void shrinkRay(Fx fx) {
        Doll d = doll(fx);
        fx.anim(6, 30, 1, t -> {
            d.scale(1 - 0.9 * t);
            fx.ring(fx.loc(0, 1.2 * (1 - t) + 0.1, 0), 0.7 * (1 - t) + 0.15, 14, t * 6, l -> fx.dust(l, B, 1f, 1, 0));
            if ((int) (t * 30) % 5 == 0) fx.sound("minecraft:block.beacon.ambient", 1f, 1.2f + (float) t);
        });
        fx.later(40, () -> poof(fx, d, B));
    }

    private static void floatAway(Fx fx) {
        Doll d = doll(fx);
        fx.anim(4, 50, 1, t -> {
            d.at(Math.sin(t * 8) * 0.25, 5 * t * t, Math.cos(t * 8) * 0.25).look(mcYaw(fx) + (float) (t * 180), -20);
            fx.dust(d.loc().add(0, 0.2, 0), Y, 1f, 2, 0.3);
            fx.p(Particle.END_ROD, d.loc().add(0, 1, 0), 1, 0.3, 0.01);
        });
        fx.sound("minecraft:block.amethyst_block.chime", 1f, 1.4f);
        fx.later(56, () -> poof(fx, d, Y));
    }

    private static void quicksand(Fx fx) {
        Doll d = doll(fx);
        Material g = fx.groundBlock();
        fx.anim(6, 45, 1, t -> {
            d.at(fx.rand(0.03), -2.0 * t, fx.rand(0.03));
            fx.block(fx.loc(0, 0.1, 0), g, 6, 0.4, 0.05);
            if ((int) (t * 45) % 4 == 0) fx.sound("minecraft:block.sand.break", 0.8f, 0.6f);
        });
        fx.later(10, () -> fx.dust(fx.loc(0, 0.1, 0), O, 1.4f, 20, 0.6));
        fx.later(52, () -> { d.remove(); fx.block(fx.loc(0, 0.2, 0), g, 30, 0.4, 0.2); });
    }

    private static void bouncy(Fx fx) {
        Doll d = doll(fx);
        double[] heights = {1.6, 1.1, 0.6};
        for (int i = 0; i < heights.length; i++) {
            double h = heights[i];
            int start = 4 + i * 14;
            fx.anim(start, 14, 1, t -> d.at(0, h * 4 * t * (1 - t), 0));
            fx.later(start, () -> {
                fx.sound("minecraft:block.slime_block.fall", 1f, 0.8f + (float) h * 0.3f);
                fx.dust(fx.loc(0, 0.1, 0), G, 1.6f, 12, 0.5);
                fx.p(Particle.ITEM_SLIME, fx.loc(0, 0.1, 0), 10, 0.4, 0.1);
            });
        }
        fx.later(48, () -> { d.pose(Pose.SLEEPING); fx.sound("minecraft:block.slime_block.place", 1f, 0.5f); });
        fx.later(75, () -> poof(fx, d, G));
    }

    private static void napTime(Fx fx) {
        Doll d = doll(fx);
        fx.later(6, () -> { d.pose(Pose.SLEEPING); fx.sound("minecraft:entity.fox.sleep", 1f, 0.8f); });
        TextDisplay[] z = new TextDisplay[3];
        for (int i = 0; i < 3; i++) {
            int k = i;
            fx.later(14 + i * 10, () -> {
                z[k] = text(fx, "&#00A4FC&lZ", 0.3 + k * 0.25, 0.8 + k * 0.35, 0, 0.8f + k * 0.4f);
                fx.sound("minecraft:entity.cat.purr", 0.6f, 1.4f);
            });
            fx.later(16 + i * 10, () -> textTo(z[k], 0.6, 0.8f + k * 0.4f, 30));
        }
        fx.later(70, () -> { for (TextDisplay t : z) if (t != null) textGone(fx, t, 0); });
        fx.later(80, () -> poof(fx, d, B));
    }

    private static void dizzy(Fx fx) {
        Doll d = doll(fx);
        Model stars = fx.model(0, 2.15, 0);
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5;
            stars.cube(Material.YELLOW_CONCRETE, Math.cos(a) * 0.45, 0, Math.sin(a) * 0.45, 0.14);
        }
        stars.show(4);
        fx.anim(2, 50, 1, t -> {
            stars.rot(t * 900, 0, 0).at(0, 2.15 - 0.4 * Math.max(0, t - 0.7) / 0.3, 0).apply(1);
            d.look(mcYaw(fx) + (float) (Math.sin(t * 20) * 25), (float) (Math.sin(t * 13) * 20));
            if ((int) (t * 50) % 8 == 0) fx.sound("minecraft:block.note_block.chime", 0.6f, 1.5f + (float) fx.rand(0.3));
        });
        fx.later(36, () -> { d.pose(Pose.SLEEPING); fx.sound("minecraft:entity.player.attack.knockback", 1f, 0.6f); });
        stars.vanish(58, 6);
        fx.later(70, () -> poof(fx, d, Y));
    }

    private static void crouchSpam(Fx fx) {
        Doll d = doll(fx);
        fx.repeat(12, 4, i -> {
            d.pose(i % 2 == 0 ? Pose.SNEAKING : Pose.STANDING);
            if (i % 2 == 0) {
                fx.dust(fx.loc(0, 0.3, 0), C, 1.3f, 8, 0.4);
                fx.sound("minecraft:block.note_block.hat", 0.8f, 1.2f);
            }
        });
        fx.later(52, () -> poof(fx, d, C));
    }

    private static void confettiPop(Fx fx) {
        Doll d = doll(fx);
        fx.anim(4, 12, 1, t -> d.at(0, 0.8 * Math.sin(t * Math.PI / 2), 0).scale(1 + 0.15 * t));
        fx.later(18, () -> {
            Location at = d.loc().add(0, 1, 0);
            d.remove();
            fx.sphere(at, 0.6, 40, l -> fx.dust(l, pick(fx), 1.5f, 1, 0.05));
            fx.p(Particle.FIREWORK, at, 20, 0.2, 0.15);
            fx.sound("minecraft:entity.firework_rocket.twinkle", 1f, 1.2f);
            fx.sound("minecraft:entity.firework_rocket.blast", 1f, 1f);
        });
        fx.repeat(14, 2, i -> confetti(fx, fx.loc(0, 2.2 - i * 0.13, 0), 18, 0.6 + i * 0.05));
    }

    private static void gg(Fx fx) {
        Doll d = doll(fx);
        TextDisplay t = text(fx, "&#00FC00&lGG", 0, 2.5, 0, 2.2f);
        fx.sound("minecraft:entity.player.levelup", 1f, 1.4f);
        fx.later(10, () -> d.pose(Pose.SNEAKING));
        fx.later(16, () -> d.pose(Pose.STANDING));
        fx.later(22, () -> d.pose(Pose.SNEAKING));
        fx.repeat(10, 3, i -> fx.dust(fx.loc(0, 2.6, 0), G, 1f, 4, 0.5));
        textGone(fx, t, 50);
        fx.later(56, () -> poof(fx, d, G));
    }

    // ==================================================================== uncommon

    private static void freezeFrame(Fx fx) {
        Doll d = doll(fx);
        Model ice = root(fx, 0);
        ice.box(Material.LIGHT_BLUE_STAINED_GLASS, -0.55, 0, -0.55, 1.1, 2.1, 1.1);
        ice.size(0.01).show(0);
        fx.later(4, () -> { ice.size(1).apply(6); fx.sound("minecraft:block.glass.place", 1f, 0.8f); fx.sound("minecraft:entity.player.hurt_freeze", 1f, 1f); });
        fx.repeat(10, 3, i -> fx.p(Particle.SNOWFLAKE, fx.loc(0, 1, 0), 6, 0.6, 0.01));
        fx.later(40, () -> fx.sound("minecraft:block.glass.hit", 1f, 0.6f));
        fx.later(50, () -> {
            ice.shatter(0, 12, 1.2, 0.3);
            Rig r = toRig(fx, d);
            r.root.shatter(0, 14, 1.5, 0.4);
            cloud(fx, fx.loc(0, 1, 0), B, 40, 0.6);
            fx.block(fx.loc(0, 1, 0), Material.LIGHT_BLUE_STAINED_GLASS, 40, 0.5, 0.2);
            fx.sound("minecraft:block.glass.break", 1.4f, 0.8f);
        });
    }

    private static void caged(Fx fx) {
        Doll d = doll(fx);
        Model cage = root(fx, 4);
        for (int i = 0; i < 4; i++) {
            double x = i < 2 ? -0.6 : 0.5, z = i % 2 == 0 ? -0.6 : 0.5;
            cage.box(Material.PURPLE_CONCRETE, x, 0, z, 0.1, 2.3, 0.1);
        }
        for (int i = 0; i < 3; i++) {
            cage.box(Material.PURPLE_CONCRETE, -0.6, 0, -0.6 + 0.37 * (i + 1) - 0.05, 0.1, 2.3, 0.06);
            cage.box(Material.PURPLE_CONCRETE, 0.5, 0, -0.6 + 0.37 * (i + 1) - 0.05, 0.1, 2.3, 0.06);
            cage.box(Material.PURPLE_CONCRETE, -0.6 + 0.37 * (i + 1) - 0.05, 0, -0.6, 0.06, 2.3, 0.1);
            cage.box(Material.PURPLE_CONCRETE, -0.6 + 0.37 * (i + 1) - 0.05, 0, 0.5, 0.06, 2.3, 0.1);
        }
        cage.box(Material.PURPLE_CONCRETE, -0.65, 2.3, -0.65, 1.3, 0.12, 1.3);
        cage.show(1);
        fx.anim(4, 8, 1, t -> cage.at(0, 4 * (1 - t * t), 0).apply(1));
        fx.later(12, () -> {
            fx.sound("minecraft:block.anvil.land", 1f, 0.7f);
            fx.sound("minecraft:block.iron_door.close", 1f, 0.6f);
            cloud(fx, fx.loc(0, 0.2, 0), PU, 30, 0.7);
            fx.block(fx.loc(0, 0.1, 0), fx.groundBlock(), 20, 0.6, 0.1);
        });
        fx.later(20, () -> d.pose(Pose.SNEAKING));
        fx.anim(40, 30, 1, t -> { cage.at(0, -2.4 * t, 0).apply(1); d.at(0, -2.4 * t, 0); });
        fx.later(72, () -> { d.remove(); cage.remove(); cloud(fx, fx.loc(0, 0.2, 0), PU, 20, 0.5); });
    }

    private static void laser(Fx fx) {
        Doll d = doll(fx);
        Model beam = root(fx, 0);
        beam.box(Material.RED_STAINED_GLASS, -0.25, 0, -0.25, 0.5, 30, 0.5);
        beam.box(Material.RED_CONCRETE, -0.08, 0, -0.08, 0.16, 30, 0.16);
        beam.size(0.01).show(0);
        fx.later(6, () -> { beam.size(1).apply(3); fx.sound("minecraft:block.beacon.activate", 1.4f, 1.6f); });
        fx.repeat(14, 2, i -> {
            fx.dust(fx.loc(0, 1 + fx.rand(1), 0), R, 1.5f, 10, 0.4);
            fx.p(Particle.SMOKE, fx.loc(0, 1, 0), 4, 0.3, 0.02);
            d.at(fx.rand(0.05), 0, fx.rand(0.05)).scale(1 - i * 0.04);
            if (i % 3 == 0) fx.sound("minecraft:block.fire.extinguish", 0.8f, 1.4f);
        });
        fx.later(40, () -> { beam.vanish(0, 4); poof(fx, d, R); fx.p(Particle.LAVA, fx.loc(0, 0.5, 0), 10, 0.3, 0.1); fx.sound("minecraft:entity.generic.burn", 1f, 1f); });
    }

    private static void giant(Fx fx) {
        Doll d = doll(fx);
        fx.anim(4, 30, 1, t -> {
            d.scale(1 + 2.6 * t * t);
            if ((int) (t * 30) % 5 == 0) {
                fx.sound("minecraft:entity.ravager.step", 1f, 1.2f - (float) t * 0.5f);
                fx.block(fx.loc(0, 0.1, 0), fx.groundBlock(), 15, 0.8, 0.15);
            }
        });
        fx.later(40, () -> {
            Location at = fx.loc(0, 4.5, 0);
            d.remove();
            fx.p(Particle.EXPLOSION, at, 6, 1.2, 0);
            fx.sphere(at, 1.8, 70, l -> fx.dust(l, G, 2f, 1, 0.1));
            fx.sound("minecraft:entity.generic.explode", 1.2f, 0.8f);
        });
    }

    private static void rewind(Fx fx) {
        Doll d = doll(fx);
        float[] yaw = {mcYaw(fx)};
        fx.anim(4, 40, 1, t -> {
            yaw[0] -= 22;
            d.at(-Math.sin(t * Math.PI * 2) * 0.6 * t, 1.6 * t, 0).look(yaw[0], 0);
            fx.dust(d.loc().add(fx.rand(0.4), 1 + fx.rand(0.8), fx.rand(0.4)), B, 1f, 3, 0.05);
            if ((int) (t * 40) % 4 == 0) fx.sound("minecraft:block.note_block.bit", 0.7f, 2f - (float) t);
        });
        fx.repeat(5, 8, i -> { d.e.setInvisible(true); fx.later(2, () -> d.e.setInvisible(false)); });
        fx.later(46, () -> poof(fx, d, B));
    }

    private static void glitch(Fx fx) {
        Doll d = doll(fx);
        fx.repeat(18, 3, i -> {
            d.at(fx.rand(0.7), Math.max(0, fx.rand(0.4)), fx.rand(0.7)).look(mcYaw(fx) + (float) fx.rand(60), (float) fx.rand(30));
            fx.dust(d.loc().add(0, 1, 0), i % 2 == 0 ? P : B, 1.3f, 10, 0.4);
            if (i % 3 == 0) { d.e.setInvisible(true); fx.later(1, () -> d.e.setInvisible(false)); }
            fx.sound("minecraft:block.note_block.bit", 0.6f, 0.5f + (float) fx.rnd.nextDouble() * 1.5f);
        });
        fx.later(58, () -> { cloud(fx, d.loc().add(0, 1, 0), P, 30, 0.5); cloud(fx, d.loc().add(0, 1, 0), B, 30, 0.5); d.remove(); fx.sound("minecraft:entity.enderman.teleport", 1f, 1.5f); });
    }

    private static void bubble(Fx fx) {
        Doll d = doll(fx);
        Model b = fx.model(0, 1, 0);
        b.boxC(Material.LIGHT_BLUE_STAINED_GLASS, 0, 0, 0, 1.4, 1.4, 1.4);
        b.size(0.01).show(0);
        fx.later(4, () -> { b.size(1).apply(6); fx.sound("minecraft:entity.player.splash", 1f, 1.4f); });
        fx.later(10, () -> d.scale(0.6));
        fx.anim(12, 50, 1, t -> {
            double y = 3.5 * t, x = Math.sin(t * 9) * 0.3;
            d.at(x, 0.55 + y, 0);
            b.at(x, 1 + y, 0).rot(t * 90, 0, 0).apply(1);
            fx.p(Particle.BUBBLE_POP, fx.loc(x, 1 + y, 0), 2, 0.5, 0.01);
        });
        fx.later(64, () -> {
            Location at = fx.loc(Math.sin(9) * 0.3, 4.5, 0);
            b.remove();
            fx.p(Particle.SPLASH, at, 40, 0.6, 0.1);
            cloud(fx, at, C, 30, 0.6);
            fx.sound("minecraft:entity.player.splash.high_speed", 1f, 1.6f);
            poof(fx, d, B);
        });
    }

    private static void spotlight(Fx fx) {
        Doll d = doll(fx);
        Model cone = root(fx, 0);
        for (int i = 0; i < 10; i++) {
            double r = 0.5 + i * 0.12;
            cone.boxC(Material.YELLOW_STAINED_GLASS, 0, i * 0.6 + 0.3, 0, r * 2, 0.05, r * 2);
        }
        cone.size(0.01).show(0);
        fx.later(4, () -> { cone.size(1).apply(4); fx.sound("minecraft:block.lever.click", 1f, 0.6f); fx.sound("minecraft:block.beacon.power_select", 1f, 1.2f); });
        float[] yaw = {mcYaw(fx)};
        fx.anim(12, 40, 1, t -> {
            yaw[0] += 12;
            d.look(yaw[0], -10);
            fx.dust(fx.loc(fx.rand(0.8), 0.1, fx.rand(0.8)), Y, 1.2f, 2, 0.05);
        });
        fx.later(30, () -> fx.sound("minecraft:entity.player.levelup", 0.8f, 0.8f));
        fx.later(56, () -> { cone.vanish(0, 4); fx.sound("minecraft:block.lever.click", 1f, 0.5f); poof(fx, d, Y); });
    }

    private static void groundSlam(Fx fx) {
        Doll d = doll(fx);
        fx.anim(4, 14, 1, t -> d.at(0, 4 * Math.sin(t * Math.PI / 2), 0).look(mcYaw(fx), -40));
        fx.later(4, () -> fx.sound("minecraft:entity.breeze.jump", 1f, 0.8f));
        fx.anim(22, 5, 1, t -> d.at(0, 4 * (1 - t), 0));
        fx.later(27, () -> {
            d.pose(Pose.SLEEPING);
            fx.sound("minecraft:item.mace.smash_ground_heavy", 1.4f, 0.8f);
            fx.p(Particle.EXPLOSION, fx.loc(0, 0.3, 0), 3, 0.6, 0);
            fx.block(fx.loc(0, 0.2, 0), fx.groundBlock(), 60, 1.2, 0.3);
            shock(fx, 0.15, 0.4, 3.2, O);
        });
        fx.later(60, () -> poof(fx, d, O));
    }

    private static void rainCloud(Fx fx) {
        Doll d = doll(fx);
        Model cl = root(fx, 2.9);
        double[][] puffs = {{0, 0, 0, 0.9}, {-0.55, -0.1, 0.1, 0.7}, {0.55, -0.05, -0.1, 0.75}, {0.2, 0.3, 0.1, 0.6}, {-0.25, 0.25, -0.15, 0.6}};
        for (double[] p : puffs) cl.cube(Material.WHITE_WOOL, p[0], p[1], p[2], p[3]);
        cl.size(0.01).show(0);
        fx.later(4, () -> { cl.size(1).apply(6); fx.sound("minecraft:weather.rain", 1f, 1f); });
        fx.repeat(25, 2, i -> {
            for (int k = 0; k < 4; k++) fx.dust(fx.loc(fx.rand(0.7), 2.4 - fx.rnd.nextDouble() * 2.2, fx.rand(0.5)), B, 0.9f, 1, 0);
            fx.p(Particle.FALLING_WATER, fx.loc(0, 2.5, 0), 4, 0.6, 0);
        });
        fx.later(14, () -> d.pose(Pose.SNEAKING));
        fx.later(26, () -> fx.sound("minecraft:entity.villager.no", 0.8f, 0.7f));
        fx.anim(30, 25, 1, t -> d.at(0, -1.9 * t, 0));
        fx.later(58, () -> { cl.vanish(0, 6); d.remove(); cloud(fx, fx.loc(0, 0.2, 0), B, 25, 0.5); });
    }

    // ==================================================================== rare

    private static void vortex(Fx fx) {
        Doll d = doll(fx);
        float[] yaw = {mcYaw(fx)};
        fx.anim(2, 60, 1, t -> {
            for (int arm = 0; arm < 3; arm++) {
                for (int k = 0; k < 6; k++) {
                    double h = k * 0.5 + t * 0.5;
                    double a = t * 14 + arm * Math.PI * 2 / 3 + k * 0.6;
                    double r = 0.4 + k * 0.25;
                    fx.dust(fx.loc(Math.cos(a) * r, h, Math.sin(a) * r), k % 2 == 0 ? PU : P, 1.1f, 1, 0);
                }
            }
            yaw[0] += 10 + 40 * t;
            d.at(0, 2.6 * t * t, 0).look(yaw[0], 0).scale(1 - 0.85 * t);
            if ((int) (t * 60) % 6 == 0) fx.sound("minecraft:entity.phantom.flap", 0.8f, 0.6f + (float) t);
        });
        fx.later(64, () -> { poof(fx, d, PU); fx.sound("minecraft:entity.enderman.teleport", 1f, 0.6f); });
    }

    private static void teleported(Fx fx) {
        Doll d = doll(fx);
        fx.repeat(8, 4, i -> {
            d.e.setInvisible(true);
            fx.later(2, () -> d.e.setInvisible(false));
            fx.p(Particle.PORTAL, fx.loc(0, 1, 0), 30, 0.4, 0.5);
            fx.sound("minecraft:block.portal.ambient", 0.4f, 1.6f);
        });
        fx.later(36, () -> {
            fx.ring(fx.loc(0, 0.05, 0), 1.0, 30, 0, l -> fx.dust(l, PU, 1.5f, 1, 0));
            fx.implode(fx.loc(0, 1, 0), Particle.REVERSE_PORTAL, 40, 1.5, 0.15);
            cloud(fx, fx.loc(0, 1, 0), PU, 40, 0.4);
            d.remove();
            fx.sound("minecraft:entity.enderman.teleport", 1.2f, 0.8f);
        });
        fx.later(46, () -> {
            Location far = fx.loc(Math.sin(yawRad(fx, 180)) * 6, 0, Math.cos(yawRad(fx, 180)) * 6);
            cloud(fx, far.clone().add(0, 1, 0), PU, 30, 0.4);
            fx.p(Particle.PORTAL, far.clone().add(0, 1, 0), 40, 0.4, 0.5);
        });
    }

    private static double yawRad(Fx fx, double off) { return Math.toRadians(fx.face() + off); }

    private static void giftWrapped(Fx fx) {
        Doll d = doll(fx);
        Model box = root(fx, 0);
        box.box(Material.RED_CONCRETE, -0.6, 0, -0.6, 1.2, 2.1, 1.2);
        box.box(Material.YELLOW_CONCRETE, -0.62, 0, -0.1, 1.24, 2.12, 0.2);
        box.box(Material.YELLOW_CONCRETE, -0.1, 0, -0.62, 0.2, 2.12, 1.24);
        box.box(Material.YELLOW_CONCRETE, -0.3, 2.1, -0.08, 0.25, 0.25, 0.16);
        box.box(Material.YELLOW_CONCRETE, 0.05, 2.1, -0.08, 0.25, 0.25, 0.16);
        box.size(0.01).show(0);
        fx.later(10, () -> { box.size(1).apply(8); fx.sound("minecraft:item.bundle.insert", 1f, 0.7f); });
        fx.anim(30, 24, 1, t -> box.rot(fx.face() + Math.sin(t * 30) * 8 * t, 0, Math.sin(t * 25) * 4 * t).apply(1));
        fx.later(30, () -> fx.sound("minecraft:block.chest.locked", 1f, 0.8f));
        fx.later(56, () -> {
            box.shatter(0, 14, 2.5, 0.8);
            d.remove();
            Location at = fx.loc(0, 1.1, 0);
            confetti(fx, at, 90, 0.9);
            fx.p(Particle.FIREWORK, at, 30, 0.4, 0.2);
            fx.sound("minecraft:entity.firework_rocket.large_blast", 1f, 1.2f);
            fx.sound("minecraft:entity.player.levelup", 0.8f, 1.6f);
        });
    }

    private static void rocketRide(Fx fx) {
        Doll d = doll(fx);
        Model rk = root(fx, 0);
        rk.boxC(Material.WHITE_CONCRETE, 0, 0.9, -0.55, 0.45, 1.8, 0.45);
        rk.boxC(Material.RED_CONCRETE, 0, 1.95, -0.55, 0.3, 0.35, 0.3);
        rk.boxC(Material.RED_CONCRETE, 0, 0.2, -0.85, 0.12, 0.5, 0.3);
        rk.boxC(Material.RED_CONCRETE, 0.28, 0.2, -0.55, 0.3, 0.5, 0.12);
        rk.boxC(Material.RED_CONCRETE, -0.28, 0.2, -0.55, 0.3, 0.5, 0.12);
        rk.boxC(Material.LIGHT_BLUE_STAINED_GLASS, 0, 1.3, -0.32, 0.22, 0.22, 0.05);
        rk.size(0.01).show(0);
        fx.later(4, () -> { rk.size(1).apply(6); fx.sound("minecraft:block.piston.extend", 1f, 0.7f); });
        fx.later(12, () -> d.pose(Pose.SNEAKING));
        fx.repeat(10, 2, i -> fx.p(Particle.SMOKE, rk.where(0, 0, -0.55), 4, 0.15, 0.02));
        fx.later(30, () -> fx.sound("minecraft:entity.firework_rocket.launch", 1.5f, 0.6f));
        fx.anim(32, 40, 1, t -> {
            double y = 18 * t * t;
            rk.at(0, y, 0).apply(1);
            d.at(0, y + 0.2, 0);
            Location f = rk.where(0, -0.1, -0.55);
            fx.p(Particle.FLAME, f, 6, 0.1, 0.04);
            fx.dust(f, O, 1.6f, 4, 0.15);
            fx.dust(f, Y, 1.2f, 3, 0.1);
        });
        fx.later(74, () -> { Location at = rk.where(0, 1, -0.55); rk.remove(); d.remove(); fx.p(Particle.FIREWORK, at, 60, 0.3, 0.25); confetti(fx, at, 60, 1.2); fx.sound("minecraft:entity.firework_rocket.blast_far", 2f, 1f); });
    }

    private static void trophy(Fx fx) {
        Doll d = doll(fx);
        Model tr = root(fx, 0);
        tr.boxC(Material.PURPLE_CONCRETE, 0, 0.15, 0, 1.0, 0.3, 1.0);
        tr.boxC(Material.YELLOW_CONCRETE, 0, 0.45, 0, 0.7, 0.3, 0.7);
        tr.boxC(Material.YELLOW_CONCRETE, 0, 0.85, 0, 0.25, 0.5, 0.25);
        tr.boxC(Material.YELLOW_CONCRETE, 0, 1.25, 0, 0.9, 0.3, 0.9);
        tr.boxC(Material.YELLOW_CONCRETE, 0.55, 1.35, 0, 0.12, 0.3, 0.12);
        tr.boxC(Material.YELLOW_CONCRETE, -0.55, 1.35, 0, 0.12, 0.3, 0.12);
        tr.size(0.01).show(0);
        fx.later(10, () -> { tr.size(1).apply(8); fx.sound("minecraft:block.anvil.use", 0.6f, 1.6f); });
        fx.anim(10, 10, 1, t -> d.at(0, 1.4 * t, 0).scale(1 - 0.45 * t));
        float[] yaw = {mcYaw(fx)};
        fx.anim(22, 50, 1, t -> {
            yaw[0] += 5;
            tr.rot(fx.face() + t * 250, 0, 0).apply(1);
            d.look(yaw[0], 0);
            fx.dust(fx.loc(fx.rand(0.6), 1.4 + fx.rand(0.8), fx.rand(0.6)), Y, 1f, 1, 0);
            if ((int) (t * 50) % 10 == 0) fx.p(Particle.END_ROD, fx.loc(0, 2.3, 0), 2, 0.3, 0.02);
        });
        fx.later(24, () -> fx.sound("minecraft:ui.toast.challenge_complete", 0.9f, 1.2f));
        fx.later(74, () -> { tr.vanish(0, 6); poof(fx, d, Y); });
    }

    private static void washingMachine(Fx fx) {
        Doll d = doll(fx);
        Model wm = root(fx, 0);
        wm.box(Material.WHITE_CONCRETE, -0.75, 0, -0.75, 1.5, 0.2, 1.5);
        wm.box(Material.WHITE_CONCRETE, -0.75, 0, -0.75, 0.15, 2.2, 1.5);
        wm.box(Material.WHITE_CONCRETE, 0.6, 0, -0.75, 0.15, 2.2, 1.5);
        wm.box(Material.WHITE_CONCRETE, -0.75, 0, -0.75, 1.5, 2.2, 0.15);
        wm.box(Material.WHITE_CONCRETE, -0.75, 2.05, -0.75, 1.5, 0.15, 1.5);
        wm.box(Material.CYAN_STAINED_GLASS, -0.6, 0.2, 0.6, 1.2, 1.85, 0.12);
        wm.box(Material.CYAN_CONCRETE, -0.75, 2.0, 0.6, 1.5, 0.2, 0.15);
        wm.size(0.01).show(0);
        fx.later(6, () -> { wm.size(1).apply(8); fx.sound("minecraft:block.iron_door.close", 1f, 0.8f); });
        float[] yaw = {mcYaw(fx)};
        fx.anim(18, 50, 1, t -> {
            yaw[0] += 20 + 60 * t;
            d.look(yaw[0], 0).at(fx.rand(0.05), 0.2, fx.rand(0.05));
            wm.at(fx.rand(0.04 * t), 0, fx.rand(0.04 * t)).apply(1);
            fx.p(Particle.BUBBLE_POP, fx.loc(0, 1, 0), 4, 0.4, 0.02);
            fx.dust(fx.loc(0, 1, 0), C, 1f, 3, 0.4);
            if ((int) (t * 50) % 5 == 0) fx.sound("minecraft:block.bubble_column.whirlpool_inside", 0.8f, 0.8f + (float) t);
        });
        fx.later(70, () -> { fx.sound("minecraft:block.note_block.bell", 1f, 1.4f); wm.vanish(4, 6); d.remove(); fx.p(Particle.SPLASH, fx.loc(0, 1, 0), 40, 0.6, 0.2); cloud(fx, fx.loc(0, 1, 0), C, 30, 0.6); });
    }

    private static void popcorn(Fx fx) {
        Doll d = doll(fx);
        List<Model> corn = new ArrayList<>();
        fx.repeat(14, 4, i -> {
            d.at(0, 0.25 * ((i % 2) == 0 ? 1 : 0), 0);
            Model k = fx.model(fx.rand(0.9), 0.2, fx.rand(0.9));
            k.cube(Material.WHITE_CONCRETE, 0, 0, 0, 0.25);
            k.cube(Material.YELLOW_CONCRETE, 0.08, 0.05, 0.05, 0.12);
            k.size(0.01).show(0);
            corn.add(k);
            fx.later(1, () -> k.size(1).by(0, 1.2 + fx.rnd.nextDouble(), 0).rot(fx.rand(180), fx.rand(90), 0).apply(4));
            fx.later(6, () -> k.at(k.lpos.x, 0.15 + i * 0.12, k.lpos.z).apply(5));
            fx.sound("minecraft:entity.chicken.egg", 0.9f, 1.4f + (float) fx.rnd.nextDouble() * 0.4f);
            fx.dust(fx.loc(0, 0.3, 0), Y, 1f, 4, 0.4);
        });
        fx.anim(40, 20, 1, t -> d.at(0, -1.6 * t, 0));
        fx.later(66, () -> { d.remove(); for (Model k : corn) k.vanish(0, 6); cloud(fx, fx.loc(0, 0.6, 0), Y, 30, 0.6); });
    }

    private static void magnet(Fx fx) {
        Doll d = doll(fx);
        Model mg = root(fx, 5);
        mg.box(Material.RED_CONCRETE, -0.7, 0, -0.2, 0.35, 1.2, 0.4);
        mg.box(Material.LIGHT_BLUE_CONCRETE, 0.35, 0, -0.2, 0.35, 1.2, 0.4);
        mg.box(Material.WHITE_CONCRETE, -0.7, -0.25, -0.2, 0.35, 0.25, 0.4);
        mg.box(Material.WHITE_CONCRETE, 0.35, -0.25, -0.2, 0.35, 0.25, 0.4);
        mg.box(Material.RED_CONCRETE, -0.7, 1.2, -0.2, 0.7, 0.35, 0.4);
        mg.box(Material.LIGHT_BLUE_CONCRETE, 0, 1.2, -0.2, 0.7, 0.35, 0.4);
        mg.show(1);
        fx.anim(2, 12, 1, t -> mg.at(0, 7 - 3.4 * Fx.ease(t), 0).apply(1));
        fx.later(10, () -> fx.sound("minecraft:block.beacon.power_select", 1f, 0.6f));
        fx.repeat(12, 2, i -> {
            fx.line(fx.loc(-0.5, 3.3, 0), fx.loc(0, 1.6, 0), 0.25, l -> fx.dust(l, R, 0.9f, 1, 0.02));
            fx.line(fx.loc(0.5, 3.3, 0), fx.loc(0, 1.6, 0), 0.25, l -> fx.dust(l, B, 0.9f, 1, 0.02));
        });
        fx.anim(26, 12, 1, t -> d.at(0, 1.3 * Fx.ease(t), 0).look(mcYaw(fx), -60));
        fx.later(38, () -> fx.sound("minecraft:block.anvil.land", 0.6f, 1.6f));
        fx.anim(44, 30, 1, t -> { double y = 3.6 + 12 * t * t; mg.at(0, y, 0).apply(1); d.at(0, y - 2.3, 0); });
        fx.later(76, () -> { mg.remove(); poof(fx, d, R); });
    }

    private static void portal(Fx fx) {
        Doll d = doll(fx);
        Model ring = fx.model(0, 0.03, 0);
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            ring.boxC(Material.PURPLE_CONCRETE, Math.cos(a) * 0.9, 0, Math.sin(a) * 0.9, 0.25, 0.06, 0.25);
        }
        ring.boxC(Material.BLACK_CONCRETE, 0, -0.01, 0, 1.6, 0.04, 1.6);
        ring.size(0.01).show(0);
        fx.later(4, () -> { ring.size(1).apply(8); fx.sound("minecraft:block.end_portal.spawn", 0.6f, 1.4f); });
        fx.anim(10, 60, 1, t -> {
            ring.rot(t * 360, 0, 0).apply(1);
            fx.p(Particle.PORTAL, fx.loc(0, 0.2, 0), 8, 0.6, 0.3);
            fx.dust(fx.loc(fx.rand(0.8), 0.1, fx.rand(0.8)), PU, 1.2f, 2, 0.05);
        });
        fx.later(24, () -> d.look(mcYaw(fx), 50));
        fx.anim(30, 20, 1, t -> d.at(0, -2.1 * t * t, 0));
        fx.later(30, () -> fx.sound("minecraft:entity.player.big_fall", 1f, 0.6f));
        fx.later(52, () -> { d.remove(); fx.p(Particle.REVERSE_PORTAL, fx.loc(0, 0.5, 0), 40, 0.3, 0.2); });
        fx.later(64, () -> { ring.vanish(0, 6); fx.sound("minecraft:block.portal.trigger", 0.4f, 2f); });
    }

    private static void danceFloor(Fx fx) {
        Doll d = doll(fx);
        Model fl = fx.model(0, 0.02, 0);
        Model[][] tiles = new Model[5][5];
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            Model t = fl.child(-1.25 + x * 0.5, 0, -1.25 + z * 0.5);
            t.box(BLOCKS[(x + z) % BLOCKS.length], 0.02, 0, 0.02, 0.46, 0.05, 0.46);
            tiles[x][z] = t;
        }
        fl.size(0.01).show(0);
        fx.later(4, () -> fl.size(1).apply(6));
        String[] notes = {"minecraft:block.note_block.bass", "minecraft:block.note_block.snare", "minecraft:block.note_block.hat", "minecraft:block.note_block.bell"};
        fx.repeat(16, 4, i -> {
            d.pose(i % 2 == 0 ? Pose.SNEAKING : Pose.STANDING).look(mcYaw(fx) + (i % 4 < 2 ? 30 : -30), 0);
            fx.sound(notes[i % notes.length], 0.9f, 0.8f + (i % 5) * 0.15f);
            fx.p(Particle.NOTE, fx.loc(fx.rand(1), 2.2, fx.rand(1)), 1, 0, 1);
            for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
                boolean on = (x + z + i) % 3 == 0;
                tiles[x][z].at(-1.25 + x * 0.5, on ? 0.06 : 0, -1.25 + z * 0.5).apply(2);
                if (on) fx.dust(fx.loc(-1 + x * 0.5, 0.15, -1 + z * 0.5), ALL[(x + z + i) % ALL.length], 1f, 1, 0.05);
            }
        });
        fx.later(68, () -> { fl.vanish(0, 6); d.pose(Pose.SLEEPING); fx.sound("minecraft:block.note_block.didgeridoo", 1f, 0.6f); });
        fx.later(84, () -> poof(fx, d, P));
    }

    // ==================================================================== epic

    private static void hologram(Fx fx) {
        Doll d = doll(fx);
        fx.anim(4, 50, 1, t -> {
            double y = (t * 4 % 1) * 2.1;
            fx.ring(fx.loc(0, y, 0), 0.55, 18, 0, l -> fx.dust(l, B, 0.8f, 1, 0));
            if ((int) (t * 50) % 4 == 0) { d.e.setInvisible(true); fx.later(1, () -> d.e.setInvisible(false)); }
            fx.dust(fx.loc(fx.rand(0.5), fx.rnd.nextDouble() * 2, fx.rand(0.5)), C, 0.8f, 2, 0);
        });
        fx.later(4, () -> fx.sound("minecraft:block.beacon.ambient", 1f, 1.8f));
        fx.later(30, () -> fx.sound("minecraft:block.respawn_anchor.deplete", 0.8f, 1.6f));
        fx.later(56, () -> {
            Rig r = toRig(fx, d);
            r.recolor(Material.LIGHT_BLUE_STAINED_GLASS);
            fx.later(6, () -> r.recolor(Material.CYAN_STAINED_GLASS));
            for (Model m : r.limbs()) m.shatter(10 + fx.rnd.nextInt(10), 10, 0.4, 1.2);
            fx.later(10, () -> fx.sound("minecraft:block.beacon.deactivate", 1f, 1.4f));
        });
    }

    private static void pinata(Fx fx) {
        Doll d = doll(fx);
        Model rope = root(fx, 2.15);
        rope.box(Material.WHITE_WOOL, -0.03, 0, -0.03, 0.06, 6, 0.06);
        rope.show(4);
        fx.anim(6, 10, 1, t -> d.at(0, 0.6 * t, 0).look(mcYaw(fx), 30));
        float[] yaw = {mcYaw(fx)};
        fx.anim(16, 40, 1, t -> {
            yaw[0] += 18;
            double sw = Math.sin(t * 18) * 0.35;
            d.at(sw, 0.6, 0).look(yaw[0], 20);
            rope.at(sw, 2.75, 0).apply(1);
            if ((int) (t * 40) % 7 == 0) {
                fx.sound("minecraft:entity.player.attack.strong", 1f, 1f);
                confetti(fx, fx.loc(sw, 1.4, 0), 9, 0.3);
            }
        });
        fx.later(58, () -> {
            Location at = fx.loc(0, 1.5, 0);
            Rig r = toRig(fx, d);
            r.root.shatter(0, 14, 2.5, 0.9);
            rope.vanish(0, 4);
            confetti(fx, at, 120, 1.0);
            Material[] candy = {Material.SWEET_BERRIES, Material.COOKIE, Material.HONEY_BOTTLE, Material.GLOW_BERRIES};
            for (Material c : candy) fx.item(at, c, 8, 0.4, 0.25);
            fx.p(Particle.FIREWORK, at, 30, 0.4, 0.2);
            fx.sound("minecraft:entity.firework_rocket.large_blast", 1.2f, 1f);
            fx.sound("minecraft:entity.villager.celebrate", 1f, 1.2f);
        });
    }

    private static void thunderStrike(Fx fx) {
        Doll d = doll(fx);
        for (int i = 0; i < 3; i++) {
            int k = i;
            fx.later(8 + i * 14, () -> {
                fx.lightning(fx.loc(fx.rand(0.3), 0, fx.rand(0.3)));
                fx.p(Particle.ELECTRIC_SPARK, fx.loc(0, 1, 0), 30, 0.5, 0.2);
                fx.dust(fx.loc(0, 1, 0), B, 1.6f, 25, 0.6);
                d.scale(1 - 0.1 * (k + 1));
                d.at(0, 0.3, 0);
                fx.later(3, () -> d.at(0, 0, 0));
            });
        }
        fx.repeat(20, 2, i -> fx.p(Particle.SMOKE, fx.loc(0, 1, 0), 3, 0.3, 0.02));
        fx.later(50, () -> {
            Rig r = toRig(fx, d);
            r.recolor(Material.BLACK_CONCRETE);
            r.root.shatter(8, 12, 0.6, -0.4);
            fx.sound("minecraft:block.fire.extinguish", 1f, 0.6f);
            fx.p(Particle.LARGE_SMOKE, fx.loc(0, 1, 0), 20, 0.4, 0.03);
            cloud(fx, fx.loc(0, 0.2, 0), Y, 20, 0.5);
        });
    }

    private static void statue(Fx fx) {
        Doll d = doll(fx);
        fx.later(8, () -> fx.sound("minecraft:block.stone.place", 1f, 0.6f));
        fx.later(14, () -> {
            Rig r = toRig(fx, d);
            r.recolor(Material.STONE);
            fx.block(fx.loc(0, 1, 0), Material.STONE, 30, 0.4, 0.1);
            fx.later(10, () -> r.recolor(Material.COBBLESTONE));
            fx.later(22, () -> r.recolor(Material.MOSSY_COBBLESTONE));
            fx.later(30, () -> fx.sound("minecraft:block.stone.hit", 1f, 0.5f));
            fx.later(40, () -> {
                r.root.shatter(0, 14, 0.7, -0.5);
                fx.block(fx.loc(0, 1, 0), Material.COBBLESTONE, 60, 0.5, 0.2);
                fx.sound("minecraft:block.stone.break", 1.4f, 0.5f);
                cloud(fx, fx.loc(0, 0.3, 0), M, 25, 0.6);
            });
        });
    }

    private static void goldStatue(Fx fx) {
        Doll d = doll(fx);
        fx.later(8, () -> fx.sound("minecraft:block.amethyst_block.chime", 1f, 0.8f));
        fx.later(14, () -> {
            Rig r = toRig(fx, d);
            r.recolor(Material.YELLOW_CONCRETE);
            fx.later(6, () -> r.recolor(Material.GOLD_BLOCK));
            fx.repeat(14, 3, i -> { fx.p(Particle.WAX_ON, fx.loc(0, 1, 0), 6, 0.4, 0.05); fx.dust(fx.loc(fx.rand(0.4), 0.2 + fx.rnd.nextDouble() * 1.8, fx.rand(0.4)), Y, 1f, 2, 0); });
            fx.later(10, () -> fx.sound("minecraft:entity.player.levelup", 1f, 1.8f));
            fx.later(46, () -> {
                r.root.shatter(0, 14, 1.8, 0.8);
                fx.item(fx.loc(0, 1, 0), Material.GOLD_NUGGET, 30, 0.4, 0.2);
                fx.sound("minecraft:block.chain.break", 1.2f, 0.8f);
                fx.sound("minecraft:entity.item.break", 1f, 0.8f);
                cloud(fx, fx.loc(0, 1, 0), Y, 40, 0.6);
            });
        });
    }

    private static void starShower(Fx fx) {
        Doll d = doll(fx);
        for (int i = 0; i < 9; i++) {
            int k = i;
            fx.later(4 + i * 5, () -> {
                double x = k == 8 ? 0 : fx.rand(2.2), z = k == 8 ? 0 : fx.rand(2.2);
                Model s = fx.model(x, 9, z);
                s.cube(Material.YELLOW_CONCRETE, 0, 0, 0, 0.35);
                s.cube(Material.YELLOW_CONCRETE, 0, 0, 0, 0.18);
                s.rot(45, 45, 0).show(0);
                fx.anim(1, 10, 1, t -> {
                    s.at(x, 9 - 8.8 * t, z).rot(45 + t * 400, 45, 0).apply(1);
                    fx.dust(fx.loc(x, 9 - 8.8 * t + 0.4, z), k % 2 == 0 ? Y : O, 1.2f, 3, 0.1);
                });
                fx.later(11, () -> {
                    fx.p(Particle.FIREWORK, fx.loc(x, 0.3, z), 12, 0.2, 0.1);
                    fx.sound("minecraft:entity.firework_rocket.twinkle", 1f, 1.2f + (float) fx.rnd.nextDouble() * 0.5f);
                    s.vanish(0, 4);
                    if (k == 8) { d.pose(Pose.SLEEPING); fx.sound("minecraft:entity.player.attack.crit", 1f, 0.8f); }
                });
            });
        }
        fx.later(70, () -> poof(fx, d, Y));
    }

    private static void bigSlap(Fx fx) {
        Doll d = doll(fx);
        double side = yawRad(fx, 90);
        Model hand = fx.model(Math.sin(side) * 3.5, 1.0, Math.cos(side) * 3.5).rot(fx.face() + 90, 0, 0);
        hand.box(Material.PINK_CONCRETE, -0.15, 0, -0.5, 0.3, 1.1, 1.0);
        for (int f = 0; f < 4; f++) hand.box(Material.PINK_CONCRETE, -0.13, 1.1, -0.48 + f * 0.25, 0.26, 0.55, 0.2);
        hand.box(Material.PINK_CONCRETE, -0.13, 0.4, 0.5, 0.26, 0.2, 0.45);
        hand.size(0.01).show(0);
        fx.later(4, () -> { hand.size(1.3).apply(6); fx.sound("minecraft:entity.breeze.whirl", 1f, 0.6f); });
        fx.anim(18, 4, 1, t -> hand.at(Math.sin(side) * (3.5 - 2.8 * t), 1.0, Math.cos(side) * (3.5 - 2.8 * t)).apply(1));
        fx.later(22, () -> {
            fx.sound("minecraft:entity.player.attack.knockback", 1.5f, 0.6f);
            fx.sound("minecraft:item.mace.smash_air", 1f, 1f);
            fx.p(Particle.SWEEP_ATTACK, fx.loc(0, 1.2, 0), 3, 0.3, 0);
            cloud(fx, fx.loc(0, 1.2, 0), P, 30, 0.4);
        });
        fx.anim(23, 20, 1, t -> d.at(-Math.sin(side) * 8 * t, 2.5 * Math.sin(t * Math.PI), -Math.cos(side) * 8 * t).look(mcYaw(fx) + (float) (t * 720), 0));
        hand.vanish(40, 6);
        fx.later(44, () -> poof(fx, d, P));
    }

    private static void knockout(Fx fx) {
        Doll d = doll(fx);
        double front = yawRad(fx, 0);
        Model glove = fx.model(Math.sin(front) * 3, 1.4, Math.cos(front) * 3).rot(fx.face(), 0, 0);
        glove.boxC(Material.RED_CONCRETE, 0, 0, 0, 0.55, 0.55, 0.7);
        glove.boxC(Material.RED_CONCRETE, 0.32, 0.05, 0.1, 0.2, 0.3, 0.35);
        glove.boxC(Material.WHITE_CONCRETE, 0, 0, 0.45, 0.45, 0.45, 0.2);
        glove.size(0.01).show(0);
        fx.later(4, () -> { glove.size(1).apply(6); fx.sound("minecraft:item.armor.equip_leather", 1f, 0.6f); });
        fx.anim(16, 4, 1, t -> glove.at(Math.sin(front) * (3 - 2.4 * t), 1.4, Math.cos(front) * (3 - 2.4 * t)).apply(1));
        fx.later(20, () -> {
            fx.sound("minecraft:entity.player.attack.crit", 1.5f, 0.6f);
            fx.sound("minecraft:block.bell.use", 1f, 1.6f);
            fx.p(Particle.CRIT, fx.loc(0, 1.6, 0), 30, 0.3, 0.3);
            d.pose(Pose.SLEEPING);
        });
        fx.later(22, () -> {
            TextDisplay t = text(fx, "&#FCE300&lK.O.", 0, 2.4, 0, 2.4f);
            textGone(fx, t, 40);
        });
        fx.repeat(12, 4, i -> fx.dust(fx.loc(0, 2.5, 0), i % 2 == 0 ? Y : R, 1.2f, 4, 0.6));
        glove.vanish(30, 6);
        fx.later(70, () -> poof(fx, d, R));
    }

    // ==================================================================== legendary

    private static void dragonBreath(Fx fx) {
        Doll d = doll(fx);
        fx.later(4, () -> fx.sound("minecraft:entity.ender_dragon.growl", 0.8f, 1.2f));
        fx.anim(10, 40, 1, t -> {
            for (int i = 0; i < 6; i++) {
                double y = 6 - (6 - 1) * t + fx.rand(0.4);
                fx.dust(fx.loc(fx.rand(0.5 + t), y * (1 - 0.6 * t), fx.rand(0.5 + t)), i % 2 == 0 ? PU : P, 1.6f, 1, 0);
            }
            fx.p(Particle.PORTAL, fx.loc(0, 1, 0), 6, 0.5, 0.02);
            if ((int) (t * 40) % 8 == 0) fx.sound("minecraft:entity.ender_dragon.shoot", 0.8f, 0.8f);
        });
        fx.anim(30, 20, 1, t -> d.scale(1 - 0.3 * t).at(fx.rand(0.04), 0, fx.rand(0.04)));
        fx.later(52, () -> {
            Rig r = toRig(fx, d);
            r.recolor(Material.PURPLE_CONCRETE);
            fx.later(4, () -> r.recolor(Material.MAGENTA_STAINED_GLASS));
            r.root.shatter(8, 16, 0.5, 1.0);
            fx.p(Particle.PORTAL, fx.loc(0, 1, 0), 60, 0.6, 0.05);
            fx.sound("minecraft:entity.ender_dragon.flap", 1f, 0.8f);
        });
    }

    private static void blender(Fx fx) {
        Doll d = doll(fx);
        Model bl = root(fx, 0);
        bl.box(Material.GRAY_CONCRETE, -0.75, 0, -0.75, 1.5, 0.45, 1.5);
        bl.box(Material.RED_CONCRETE, -0.2, 0.15, 0.75, 0.15, 0.15, 0.03);
        bl.box(Material.LIME_CONCRETE, 0.05, 0.15, 0.75, 0.15, 0.15, 0.03);
        bl.box(Material.GLASS, -0.6, 0.45, -0.6, 1.2, 2.3, 0.06);
        bl.box(Material.GLASS, -0.6, 0.45, 0.54, 1.2, 2.3, 0.06);
        bl.box(Material.GLASS, -0.6, 0.45, -0.6, 0.06, 2.3, 1.2);
        bl.box(Material.GLASS, 0.54, 0.45, -0.6, 0.06, 2.3, 1.2);
        bl.box(Material.GRAY_CONCRETE, -0.65, 2.75, -0.65, 1.3, 0.15, 1.3);
        Model liquid = bl.child(0, 0.45, 0);
        liquid.box(Material.MAGENTA_STAINED_GLASS, -0.52, 0, -0.52, 1.04, 0.01, 1.04);
        bl.size(0.01).show(0);
        fx.later(4, () -> { bl.size(1).apply(8); fx.sound("minecraft:block.glass.place", 1f, 0.8f); });
        fx.anim(10, 8, 1, t -> d.at(0, 0.45 * t, 0).scale(1 - 0.2 * t));
        float[] yaw = {mcYaw(fx)};
        fx.later(20, () -> fx.sound("minecraft:block.grindstone.use", 1.2f, 0.8f));
        fx.anim(22, 50, 1, t -> {
            yaw[0] += 30 + 60 * t;
            d.look(yaw[0], 0).at(fx.rand(0.04), 0.45 + Math.sin(t * 30) * 0.1, fx.rand(0.04));
            liquid.parts.get(0).size.set(1.04f, (float) (2.2 * t), 1.04f);
            liquid.apply(1);
            bl.at(fx.rand(0.03), 0, fx.rand(0.03)).apply(1);
            fx.dust(fx.loc(fx.rand(0.4), 0.6 + 2 * t * fx.rnd.nextDouble(), fx.rand(0.4)), t < 0.5 ? P : R, 1.2f, 3, 0.05);
            if ((int) (t * 50) % 5 == 0) fx.sound("minecraft:block.grindstone.use", 0.9f, 0.9f + (float) t);
        });
        fx.later(60, () -> d.remove());
        fx.later(76, () -> {
            bl.shatter(0, 14, 1.6, 0.6);
            fx.p(Particle.SPLASH, fx.loc(0, 1.2, 0), 60, 0.6, 0.3);
            cloud(fx, fx.loc(0, 1.2, 0), P, 60, 0.8);
            fx.sound("minecraft:block.glass.break", 1.4f, 0.8f);
            fx.sound("minecraft:entity.generic.drink", 1f, 0.8f);
        });
    }

    private static void moneyRain(Fx fx) {
        Doll d = doll(fx);
        TextDisplay t = text(fx, "&#00FC00&l$$$", 0, 2.6, 0, 2.2f);
        fx.sound("minecraft:entity.player.levelup", 1f, 1.2f);
        List<Model> bills = new ArrayList<>();
        fx.repeat(30, 2, i -> {
            double x = fx.rand(1.2), z = fx.rand(1.2);
            Model b = fx.model(x, 7, z);
            b.boxC(i % 3 == 0 ? Material.EMERALD_BLOCK : Material.LIME_CONCRETE, 0, 0, 0, 0.5, 0.06, 0.28);
            b.rot(fx.rand(180), 0, fx.rand(20)).show(0);
            bills.add(b);
            double land = 0.04 + Math.min(1.6, i * 0.055);
            fx.anim(1, 14, 1, s -> b.at(x + Math.sin(s * 6) * 0.2, 7 - (7 - land) * s, z).rot(s * 300, 0, Math.sin(s * 8) * 30).apply(1));
            if (i % 4 == 0) fx.sound(16, "minecraft:entity.experience_orb.pickup", 0.7f, 1.2f + (float) fx.rnd.nextDouble() * 0.5f);
        });
        fx.later(30, () -> d.pose(Pose.SNEAKING));
        fx.anim(40, 30, 1, s -> d.at(0, -1.4 * s, 0));
        textGone(fx, t, 60);
        fx.later(84, () -> { d.remove(); for (Model b : bills) b.vanish(0, 6); cloud(fx, fx.loc(0, 0.6, 0), G, 40, 0.8); fx.sound("minecraft:block.amethyst_block.break", 1f, 1.4f); });
    }

    private static void ascension(Fx fx) {
        Doll d = doll(fx);
        Model halo = fx.model(0, 2.25, 0);
        for (int i = 0; i < 14; i++) {
            double a = Math.PI * 2 * i / 14;
            halo.boxC(Material.YELLOW_CONCRETE, Math.cos(a) * 0.38, 0, Math.sin(a) * 0.38, 0.12, 0.05, 0.12);
        }
        halo.size(0.01).show(0);
        fx.later(6, () -> { halo.size(1).apply(6); fx.sound("minecraft:block.beacon.activate", 1f, 1.6f); });
        Model beam = root(fx, 0);
        beam.box(Material.YELLOW_STAINED_GLASS, -0.7, 0, -0.7, 1.4, 25, 1.4);
        beam.size(0.01).show(0);
        fx.later(14, () -> beam.size(1).apply(10));
        fx.anim(20, 70, 1, t -> {
            double y = 8 * t * t;
            d.at(0, y, 0).look(mcYaw(fx) + (float) (t * 90), -30);
            halo.at(0, 2.25 + y, 0).rot(t * 360, 0, 0).apply(1);
            fx.dust(fx.loc(fx.rand(0.7), y + fx.rnd.nextDouble() * 2, fx.rand(0.7)), Y, 1.2f, 3, 0.05);
            fx.p(Particle.END_ROD, fx.loc(0, y + 1, 0), 1, 0.4, 0.02);
        });
        fx.later(20, () -> fx.sound("minecraft:block.amethyst_block.resonate", 1f, 0.8f));
        fx.later(92, () -> {
            Location at = d.loc().add(0, 1, 0);
            d.remove();
            halo.vanish(0, 4);
            beam.vanish(4, 8);
            flashAt(fx, at);
            fx.sphere(at, 1.2, 60, l -> fx.dust(l, Y, 1.6f, 1, 0.05));
            fx.sound("minecraft:entity.player.levelup", 1.2f, 2f);
        });
    }

    private static void shattered(Fx fx) {
        Doll d = doll(fx);
        fx.later(6, () -> fx.sound("minecraft:block.glass.hit", 1f, 1.2f));
        fx.later(10, () -> {
            Rig r = toRig(fx, d);
            Material[] glass = {Material.LIGHT_BLUE_STAINED_GLASS, Material.CYAN_STAINED_GLASS, Material.MAGENTA_STAINED_GLASS};
            fx.repeat(6, 4, i -> { r.recolor(glass[i % glass.length]); fx.sound("minecraft:block.amethyst_cluster.hit", 1f, 0.8f + i * 0.15f); });
            fx.anim(1, 24, 1, t -> { r.root.at(fx.rand(0.03), 0.4 * t, fx.rand(0.03)).rot(fx.face() + t * 40, 0, 0).apply(1); fx.p(Particle.END_ROD, fx.loc(0, 1, 0), 1, 0.5, 0.02); });
            fx.later(30, () -> {
                for (Model m : r.limbs()) m.shatter(0, 16, 2.2, 0.5);
                fx.block(fx.loc(0, 1.2, 0), Material.LIGHT_BLUE_STAINED_GLASS, 60, 0.5, 0.3);
                cloud(fx, fx.loc(0, 1.2, 0), C, 50, 0.6);
                fx.sound("minecraft:block.glass.break", 1.5f, 0.7f);
                fx.sound("minecraft:block.glass.break", 1.5f, 1.1f);
            });
        });
    }

    private static void gladiator(Fx fx) {
        Doll d = doll(fx);
        Model arena = fx.model(0, 0, 0);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            Model p = arena.child(Math.cos(a) * 2.6, 0, Math.sin(a) * 2.6);
            p.boxC(Material.SMOOTH_QUARTZ, 0, 0.9, 0, 0.4, 1.8, 0.4);
            p.boxC(Material.ORANGE_CONCRETE, 0, 1.9, 0, 0.5, 0.2, 0.5);
        }
        arena.at(0, -2, 0).show(0);
        fx.anim(2, 14, 1, t -> arena.at(0, -2 + 2 * Fx.ease(t), 0).apply(1));
        fx.later(2, () -> fx.sound("minecraft:block.piston.extend", 1f, 0.5f));
        fx.repeat(20, 3, i -> {
            for (int k = 0; k < 8; k++) {
                double a = Math.PI * 2 * k / 8;
                fx.p(Particle.FLAME, fx.loc(Math.cos(a) * 2.6, 2.1, Math.sin(a) * 2.6), 1, 0.05, 0.01);
            }
        });
        fx.later(20, () -> { d.pose(Pose.SNEAKING); fx.sound("minecraft:entity.villager.celebrate", 1f, 0.6f); fx.sound("minecraft:entity.pillager.celebrate", 1f, 0.8f); });
        fx.later(30, () -> {
            TextDisplay t = text(fx, "&#FC0000&lDEFEATED", 0, 2.8, 0, 2.8f);
            textGone(fx, t, 30);
            fx.sound("minecraft:entity.villager.no", 1f, 0.6f);
        });
        fx.later(46, () -> {
            d.pose(Pose.SLEEPING);
            fx.sound("minecraft:item.trident.thunder", 0.8f, 1.4f);
            cloud(fx, fx.loc(0, 0.4, 0), O, 30, 0.6);
        });
        fx.later(76, () -> { arena.vanish(0, 8); poof(fx, d, O); });
    }

    private static void zeroGravity(Fx fx) {
        Doll d = doll(fx);
        List<Model> rocks = new ArrayList<>();
        Material g = fx.groundBlock();
        for (int i = 0; i < 10; i++) {
            Model r = fx.model(fx.rand(1.8), 0.1, fx.rand(1.8));
            r.cube(i % 3 == 0 ? Material.PURPLE_CONCRETE : g, 0, 0, 0, 0.25 + fx.rnd.nextDouble() * 0.2);
            r.show(4);
            rocks.add(r);
        }
        fx.sound("minecraft:block.beacon.power_select", 1f, 0.5f);
        fx.anim(6, 80, 1, t -> {
            d.at(Math.sin(t * 3) * 0.3, 3 * Fx.ease(t), Math.cos(t * 3) * 0.3).look(mcYaw(fx) + (float) (t * 200), (float) (Math.sin(t * 5) * 40));
            for (int i = 0; i < rocks.size(); i++) {
                Model r = rocks.get(i);
                r.at(r.lpos.x, 0.1 + 3.5 * Fx.ease(t) * (0.5 + 0.1 * i), r.lpos.z).rot(t * 200 * (i + 1), t * 90, 0).apply(1);
            }
            fx.dust(fx.loc(fx.rand(1.5), fx.rnd.nextDouble() * 3, fx.rand(1.5)), PU, 0.8f, 1, 0);
        });
        fx.later(10, () -> fx.sound("minecraft:block.amethyst_block.resonate", 1f, 0.5f));
        fx.later(90, () -> {
            for (Model r : rocks) r.shatter(0, 10, 1, 0.3);
            Rig rig = toRig(fx, d);
            rig.root.shatter(0, 14, 1.4, 0.2);
            cloud(fx, fx.loc(0, 3, 0), PU, 40, 0.8);
            fx.sound("minecraft:entity.generic.explode", 0.8f, 1.4f);
        });
    }

    // ==================================================================== mythic

    private static void prism(Fx fx) {
        Doll d = doll(fx);
        fx.anim(4, 30, 1, t -> {
            d.at(0, 1.5 * Fx.ease(t), 0).look(mcYaw(fx) + (float) (t * 360), 0);
            for (int i = 0; i < ALL.length; i++) {
                double a = Math.PI * 2 * i / ALL.length + t * 8;
                fx.dust(fx.loc(Math.cos(a) * 1.2, 0.2 + t * 2.5, Math.sin(a) * 1.2), ALL[i], 1.5f, 1, 0);
            }
        });
        fx.later(4, () -> fx.sound("minecraft:block.beacon.activate", 1f, 1.8f));
        fx.later(36, () -> {
            Rig r = toRig(fx, d);
            Material[] cols = {Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.PURPLE_CONCRETE};
            Model[] limbs = {r.lleg, r.rleg, r.larm, r.rarm, r.body};
            for (int i = 0; i < limbs.length; i++) {
                Material m = cols[i];
                for (Model.Part p : limbs[i].parts) if (p.d instanceof org.bukkit.entity.BlockDisplay bd) bd.setBlock(m.createBlockData());
            }
            fx.sound("minecraft:block.amethyst_block.chime", 1.2f, 1.2f);
            fx.anim(1, 26, 1, t -> { r.root.rot(fx.face() + t * 720, 0, 0).at(0, 1.5 + 0.5 * t, 0).apply(1); for (int i = 0; i < ALL.length; i++) fx.dust(fx.loc(0, 2.3, 0), ALL[i], 1.2f, 1, 0.6 * t); });
            fx.later(28, () -> {
                r.root.shatter(0, 16, 3, 0.4);
                for (int i = 0; i < ALL.length; i++) {
                    int c = ALL[i];
                    double rr = 0.5 + i * 0.35;
                    fx.later(i, () -> fx.ring(fx.loc(0, 2.2, 0), rr, 30, 0, l -> fx.dust(l, c, 1.8f, 1, 0)));
                }
                flashAt(fx, fx.loc(0, 2.2, 0));
                fx.sound("minecraft:entity.firework_rocket.large_blast_far", 2f, 1f);
                fx.sound("minecraft:block.glass.break", 1.2f, 1.4f);
            });
        });
    }

    private static void cloneArmy(Fx fx) {
        Doll main = doll(fx);
        List<Doll> clones = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            int k = i;
            fx.later(4 + i * 4, () -> {
                double a = Math.PI * 2 * k / 6;
                double x = Math.cos(a) * 2.4, z = Math.sin(a) * 2.4;
                Doll c = new Doll(fx, x, 0, z);
                c.look((float) Math.toDegrees(Math.atan2(x, -z)), 0);
                clones.add(c);
                fx.p(Particle.POOF, fx.loc(x, 1, z), 10, 0.3, 0.02);
                fx.dust(fx.loc(x, 1, z), ALL[k], 1.4f, 15, 0.4);
                fx.sound("minecraft:entity.illusioner.mirror_move", 1f, 1f + k * 0.1f);
            });
        }
        fx.repeat(10, 4, i -> {
            Pose p = i % 2 == 0 ? Pose.SNEAKING : Pose.STANDING;
            for (Doll c : clones) c.pose(p);
            main.pose(p);
            if (i % 2 == 0) fx.sound("minecraft:block.note_block.hat", 0.8f, 1.2f);
        });
        fx.anim(74, 16, 1, t -> {
            for (int i = 0; i < clones.size(); i++) {
                double a = Math.PI * 2 * i / 6 + t * 3;
                double r = 2.4 * (1 - t);
                Doll c = clones.get(i);
                c.at(Math.cos(a) * r, 0, Math.sin(a) * r);
            }
        });
        fx.later(91, () -> {
            for (Doll c : clones) c.remove();
            Rig r = toRig(fx, main);
            r.root.shatter(0, 16, 2.6, 0.6);
            fx.p(Particle.EXPLOSION, fx.loc(0, 1, 0), 3, 0.4, 0);
            confetti(fx, fx.loc(0, 1, 0), 120, 1.2);
            fx.sound("minecraft:entity.illusioner.cast_spell", 1.2f, 0.8f);
            fx.sound("minecraft:entity.firework_rocket.large_blast", 1f, 0.8f);
        });
    }

    private static void judgement(Fx fx) {
        Doll d = doll(fx);
        Model ham = root(fx, 9);
        ham.boxC(Material.PURPLE_CONCRETE, 0, 0, 0, 2.2, 1.3, 1.3);
        ham.boxC(Material.YELLOW_CONCRETE, 1.15, 0, 0, 0.1, 1.4, 1.4);
        ham.boxC(Material.YELLOW_CONCRETE, -1.15, 0, 0, 0.1, 1.4, 1.4);
        ham.boxC(Material.DARK_OAK_PLANKS, 0, 2.2, 0, 0.3, 3.2, 0.3);
        ham.boxC(Material.YELLOW_CONCRETE, 0, 3.85, 0, 0.45, 0.25, 0.45);
        ham.size(0.01).show(0);
        fx.later(4, () -> { ham.size(1).apply(10); fx.sound("minecraft:block.beacon.activate", 1.2f, 0.5f); fx.sound("minecraft:entity.wither.spawn", 0.4f, 1.6f); });
        fx.repeat(12, 3, i -> fx.dust(fx.loc(fx.rand(1.2), 9 + fx.rand(0.8), fx.rand(1.2)), i % 2 == 0 ? PU : Y, 1.4f, 6, 0.1));
        fx.later(16, () -> d.look(mcYaw(fx), -60));
        fx.anim(42, 6, 1, t -> ham.at(0, 9 - 8.3 * t * t, 0).apply(1));
        fx.later(48, () -> {
            d.remove();
            fx.p(Particle.EXPLOSION_EMITTER, fx.loc(0, 0.5, 0), 1, 0, 0);
            fx.block(fx.loc(0, 0.2, 0), fx.groundBlock(), 100, 1.5, 0.4);
            shock(fx, 0.15, 0.6, 5, PU);
            fx.later(3, () -> shock(fx, 0.25, 0.6, 4, Y));
            fx.sound("minecraft:item.mace.smash_ground_heavy", 2f, 0.6f);
            fx.sound("minecraft:entity.generic.explode", 1.5f, 0.6f);
            fx.sound("minecraft:entity.lightning_bolt.thunder", 0.8f, 1.2f);
        });
        fx.anim(52, 10, 1, t -> ham.at(0, 0.7 + fx.rand(0.05), 0).apply(1));
        fx.later(80, () -> { ham.vanish(0, 8); cloud(fx, fx.loc(0, 0.5, 0), PU, 40, 1.0); });
    }

    private static void timeStop(Fx fx) {
        Doll d = doll(fx);
        fx.anim(2, 10, 1, t -> d.at(0, 0.8 * Fx.ease(t), 0).look(mcYaw(fx), -20));
        fx.later(12, () -> {
            fx.sound("minecraft:block.bell.resonate", 1.2f, 0.6f);
            fx.sound("minecraft:block.beacon.deactivate", 1f, 0.5f);
            fx.flash(B);
            d.e.setGlowing(true);
        });
        TextDisplay[] clock = new TextDisplay[1];
        fx.later(14, () -> clock[0] = text(fx, "&#00A4FC&lTIME STOP", 0, 3, 0, 3f));
        List<Location> frozen = new ArrayList<>();
        for (int i = 0; i < 30; i++) frozen.add(fx.loc(fx.rand(2), 0.2 + fx.rnd.nextDouble() * 2.5, fx.rand(2)));
        fx.repeat(28, 2, i -> {
            for (Location l : frozen) fx.dust(l, i % 4 < 2 ? B : C, 1.1f, 1, 0);
            fx.ring(fx.loc(0, 1.3, 0), 1.6 + Math.sin(i * 0.3) * 0.1, 24, i * 0.05, l -> fx.dust(l, W, 0.7f, 1, 0));
            if (i % 4 == 0) fx.sound("minecraft:block.note_block.hat", 0.8f, 0.6f);
        });
        fx.later(70, () -> {
            if (clock[0] != null) textGone(fx, clock[0], 0);
            Rig r = toRig(fx, d);
            r.root.at(0, 0.8, 0).apply(0);
            r.recolor(Material.LIGHT_BLUE_STAINED_GLASS);
            fx.later(3, () -> {
                r.root.shatter(0, 18, 2.4, 0.3);
                for (Location l : frozen) fx.dust(l, B, 1.4f, 4, 0.2);
                fx.sound("minecraft:block.glass.break", 1.5f, 0.6f);
                fx.sound("minecraft:block.end_portal_frame.fill", 1f, 0.5f);
            });
        });
    }

    private static void nova(Fx fx) {
        Doll d = doll(fx);
        Model orb = fx.model(0, 3.4, 0);
        orb.boxC(Material.WHITE_CONCRETE, 0, 0, 0, 0.5, 0.5, 0.5);
        orb.boxC(Material.YELLOW_STAINED_GLASS, 0, 0, 0, 0.8, 0.8, 0.8);
        orb.size(0.01).show(0);
        fx.anim(4, 16, 1, t -> d.at(0, 1.2 * Fx.ease(t), 0).look(mcYaw(fx), -45));
        fx.anim(10, 50, 1, t -> {
            orb.size(0.2 + 1.8 * t).rot(t * 720, t * 360, 0).apply(1);
            fx.implode(fx.loc(0, 3.4, 0), Particle.END_ROD, 2, 2.5, 0.12);
            fx.dust(fx.loc(fx.rand(2.5), 3.4 + fx.rand(2.5), fx.rand(2.5)), pick(fx), 1.3f, 2, 0);
            d.at(fx.rand(0.04 * t), 1.2, fx.rand(0.04 * t));
            if ((int) (t * 50) % 5 == 0) fx.sound("minecraft:block.beacon.ambient", 1f, 0.6f + (float) t * 1.4f);
        });
        fx.later(10, () -> fx.sound("minecraft:block.portal.travel", 0.3f, 2f));
        fx.later(62, () -> {
            orb.shatter(0, 10, 2, 0);
            Rig r = toRig(fx, d);
            r.root.shatter(0, 18, 3.5, 0.6);
            flashAt(fx, fx.loc(0, 3.4, 0));
            fx.p(Particle.EXPLOSION_EMITTER, fx.loc(0, 2.5, 0), 1, 0, 0);
            for (int i = 0; i < ALL.length; i++) {
                int c = ALL[i];
                double rr = 0.8 + i * 0.6;
                fx.later(i * 2, () -> fx.sphere(fx.loc(0, 2.5, 0), rr, 50, l -> fx.dust(l, c, 2f, 1, 0)));
            }
            fx.sound("minecraft:entity.generic.explode", 2f, 0.6f);
            fx.sound("minecraft:entity.firework_rocket.large_blast_far", 2f, 0.8f);
            fx.sound("minecraft:entity.lightning_bolt.thunder", 1f, 1.4f);
        });
    }
}
