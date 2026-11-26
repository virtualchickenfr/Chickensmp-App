package dev.killeffects;

import static dev.killeffects.Kit.B;
import static dev.killeffects.Kit.C;
import static dev.killeffects.Kit.G;
import static dev.killeffects.Kit.M;
import static dev.killeffects.Kit.O;
import static dev.killeffects.Kit.P;
import static dev.killeffects.Kit.PU;
import static dev.killeffects.Kit.R;
import static dev.killeffects.Kit.W;
import static dev.killeffects.Kit.Y;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Display;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Pose;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

/**
 * Kill effects v4: a small set of polished effects. Every one uses the victim's real skin (Doll),
 * real Minecraft textures, smooth interpolated motion, layered particles and sounds.
 */
final class Effects3 {
    private Effects3() {}

    static Map<String, KillEffect> all() {
        List<KillEffect> l = new ArrayList<>();
        // common
        l.add(new KillEffect("tombstone", "Tombstone", Rarity.COMMON, "They fall, the ground takes them, a grave rises.", Material.MOSSY_STONE_BRICKS, Effects3::tombstone));
        l.add(new KillEffect("anvil", "Anvil Drop", Rarity.COMMON, "Look up. Too late.", Material.ANVIL, Effects3::anvil));
        l.add(new KillEffect("bone_pile", "Bone Pile", Rarity.COMMON, "They rattle apart into a pile of bones.", Material.BONE, Effects3::bonePile));
        l.add(new KillEffect("confetti_pop", "Party Popper", Rarity.COMMON, "Pop! Fireworks and confetti everywhere.", Material.FIREWORK_STAR, Effects3::partyPopper));
        // uncommon
        l.add(new KillEffect("ice_cube", "Frozen", Rarity.UNCOMMON, "Frozen in a block of ice, then shattered.", Material.PACKED_ICE, Effects3::frozen));
        l.add(new KillEffect("tnt", "TNT", Rarity.UNCOMMON, "Tsssss... BOOM. And they fly.", Material.TNT, Effects3::tnt));
        l.add(new KillEffect("ground_slam", "Ground Slam", Rarity.UNCOMMON, "Lifted by the wind, slammed into the ground.", Material.MACE, Effects3::groundSlam));
        // rare
        l.add(new KillEffect("rocket", "Rocket", Rarity.RARE, "Strapped to a firework. Bye!", Material.FIREWORK_ROCKET, Effects3::rocket));
        l.add(new KillEffect("vortex", "Vortex", Rarity.RARE, "Spun into a portal in the ground.", Material.ENDER_EYE, Effects3::vortex));
        l.add(new KillEffect("balloon", "Balloon Head", Rarity.RARE, "Their head blows up like a balloon. Pop!", Material.PLAYER_HEAD, Effects3::balloon));
        // epic
        l.add(new KillEffect("explode", "Explode", Rarity.EPIC, "Charged up, then they blow apart.", Material.TNT_MINECART, Effects3::explode));
        l.add(new KillEffect("thunder_strike", "Smite", Rarity.EPIC, "Three bolts. Burnt to ash.", Material.LIGHTNING_ROD, Effects3::smite));
        l.add(new KillEffect("ez", "EZ", Rarity.EPIC, "Way too easy. A giant EZ slams down.", Material.NAME_TAG, Effects3::ez));
        l.add(new KillEffect("phantom", "Phantom Grab", Rarity.EPIC, "A giant phantom swoops down and takes them.", Material.PHANTOM_MEMBRANE, Effects3::phantom));
        // legendary
        l.add(new KillEffect("meteor", "Meteor", Rarity.LEGENDARY, "A burning rock from the sky.", Material.MAGMA_BLOCK, Effects3::meteor));
        l.add(new KillEffect("dragon_breath", "Dragon Breath", Rarity.LEGENDARY, "The dragon burns them away.", Material.DRAGON_HEAD, Effects3::dragonBreath));
        l.add(new KillEffect("ascension", "Ascension", Rarity.LEGENDARY, "A beam of light takes them up.", Material.TOTEM_OF_UNDYING, Effects3::ascension));
        l.add(new KillEffect("shattered", "Shattered", Rarity.LEGENDARY, "Turned to glass, then shattered.", Material.GLASS, Effects3::shattered));
        // mythic
        l.add(new KillEffect("black_hole_x", "Black Hole", Rarity.MYTHIC, "Everything gets pulled in. Then: boom.", Material.BLACK_CONCRETE, Effects3::blackHole));
        l.add(new KillEffect("judgement", "Judgement", Rarity.MYTHIC, "A giant mace from the sky.", Material.MACE, Effects3::judgement));
        // exclusive (not in crates, only visible to players who were given it)
        l.add(new KillEffect("banned", "Banned", Rarity.MYTHIC, "The Banhammer comes down. BANNED.", Material.BARRIER, Effects3::banned));
        l.add(new KillEffect("chicken_god", "Chicken God", Rarity.MYTHIC, "The Golden Chicken sits on them. Then lays an egg.", Material.GOLDEN_APPLE, Effects3::chickenGod));
        Map<String, KillEffect> map = new LinkedHashMap<>();
        for (KillEffect e : l) map.put(e.id(), e);
        return map;
    }

    /** Effects that never drop from crates and only show up for players who own them (given with /ke give). */
    static final java.util.Set<String> EXCLUSIVE = java.util.Set.of("banned");

    /** Every removed effect id -> the effect a player gets instead (same rarity). */
    static final Map<String, String> MIGRATE = Map.ofEntries(
            // common
            Map.entry("coffin", "tombstone"), Map.entry("nap_time", "tombstone"), Map.entry("quicksand", "tombstone"),
            Map.entry("cake", "confetti_pop"), Map.entry("gift", "confetti_pop"), Map.entry("gg", "confetti_pop"), Map.entry("bouncy", "confetti_pop"),
            Map.entry("spin_out", "anvil"), Map.entry("shrink_ray", "anvil"), Map.entry("float_away", "anvil"), Map.entry("dizzy", "anvil"), Map.entry("crouch_spam", "anvil"),
            // uncommon
            Map.entry("freeze_frame", "ice_cube"), Map.entry("bubble", "ice_cube"), Map.entry("rain_cloud", "ice_cube"),
            Map.entry("slime_bounce", "ground_slam"), Map.entry("giant", "ground_slam"), Map.entry("spotlight", "ground_slam"), Map.entry("rewind", "ground_slam"),
            Map.entry("pumpkin", "tnt"), Map.entry("caged", "tnt"), Map.entry("laser", "tnt"), Map.entry("glitch", "tnt"),
            // rare
            Map.entry("coconut", "balloon"), Map.entry("gift_wrapped", "balloon"), Map.entry("popcorn", "balloon"), Map.entry("trophy", "balloon"),
            Map.entry("slot_machine", "rocket"), Map.entry("rocket_ride", "rocket"), Map.entry("dance_floor", "rocket"),
            Map.entry("teleported", "vortex"), Map.entry("washing_machine", "vortex"), Map.entry("magnet", "vortex"), Map.entry("portal", "vortex"),
            // epic
            Map.entry("tick_tock", "explode"), Map.entry("pinata", "explode"), Map.entry("big_slap", "explode"),
            Map.entry("awoooga", "phantom"), Map.entry("i_love_you", "phantom"), Map.entry("piano", "phantom"), Map.entry("ufo", "phantom"), Map.entry("hologram", "phantom"),
            Map.entry("heart_attack", "thunder_strike"), Map.entry("statue", "thunder_strike"), Map.entry("gold_statue", "thunder_strike"), Map.entry("star_shower", "thunder_strike"), Map.entry("knockout", "thunder_strike"),
            // legendary
            Map.entry("royal_crown", "ascension"), Map.entry("money_rain", "ascension"), Map.entry("gladiator", "ascension"),
            Map.entry("volcano", "meteor"), Map.entry("bike", "meteor"), Map.entry("zero_gravity", "meteor"),
            Map.entry("tornado", "dragon_breath"), Map.entry("blender", "shattered"),
            // mythic
            Map.entry("helicopter", "judgement"), Map.entry("dismantle", "judgement"), Map.entry("time_stop", "black_hole_x"),
            Map.entry("prism", "black_hole_x"), Map.entry("clone_army", "chicken_god"), Map.entry("nova", "chicken_god"));

    /** Version 1 ids -> version 2 ids (followed further by MIGRATE). */
    static final Map<String, String> LEGACY = Map.ofEntries(
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

    // ==================================================================== helpers

    private static Location L(Fx fx, double x, double y, double z) { return Kit.at(fx, x, y, z); }

    private static void hurt(Fx fx) {
        fx.sound("minecraft:entity.player.hurt", 1f, 0.8f);
        fx.sound("minecraft:entity.player.attack.crit", 0.8f, 0.7f);
    }

    /** Swap the rig's skin head for a block (glass, coal...). */
    private static void headTo(Rig rig, Material m) {
        for (Model.Part p : rig.head.parts) {
            if (p.d instanceof org.bukkit.entity.ItemDisplay id) id.setItemStack(new org.bukkit.inventory.ItemStack(m));
        }
    }

    /** Puff of smoke + poof, then the doll is gone. */
    private static void poof(Fx fx, Doll d, double h) {
        Location at = d.loc().add(0, h, 0);
        fx.p(Particle.POOF, at, 30, 0.35, 0.06);
        fx.p(Particle.CLOUD, at, 12, 0.3, 0.02);
        d.remove();
    }

    // ==================================================================== common

    private static void tombstone(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Material g = fx.groundBlock();
        // falls over backwards
        fx.later(3, () -> {
            d.pose(Pose.SLEEPING);
            fx.sound("minecraft:entity.player.big_fall", 1f, 0.8f);
            fx.block(fx.base.clone().add(0, 0.1, 0), g, 25, 0.5, 0.1);
        });
        // the ground swallows the body
        fx.anim(12, 18, 1, t -> {
            Kit.dollAt(fx, d, 0, -1.2 * Kit.easeIn(t), 0);
            if ((int) (t * 18) % 3 == 0) fx.block(fx.base.clone().add(0, 0.15, 0), g, 6, 0.6, 0.05);
        });
        fx.later(31, d::remove);
        fx.sound(12, "minecraft:block.rooted_dirt.break", 1f, 0.6f);
        fx.sound(18, "minecraft:block.gravel.break", 1f, 0.5f);
        // the grave rises behind
        Model grave = Kit.root(fx);
        Model stone = grave.child(0, -1.6, -1.0);
        stone.boxC(Material.MOSSY_STONE_BRICKS, 0, 0.12, 0, 1.3, 0.24, 0.7)
                .boxC(Material.STONE_BRICKS, 0, 0.82, 0, 0.9, 1.2, 0.26)
                .boxC(Material.CHISELED_STONE_BRICKS, 0, 1.52, 0, 0.7, 0.22, 0.26)
                .boxC(Material.CRACKED_STONE_BRICKS, 0, 1.68, 0, 0.42, 0.12, 0.26)
                .boxC(Material.MOSS_CARPET, 0.33, 0.25, 0.2, 0.5, 0.06, 0.3)
                .item(Material.POPPY, 0.42, 0.42, 0.42, 0.45)
                .item(Material.WITHER_ROSE, -0.45, 0.42, 0.42, 0.4);
        grave.show(0);
        fx.later(14, () -> {
            fx.sound("minecraft:block.stone.place", 1f, 0.5f);
            fx.sound("minecraft:block.grindstone.use", 0.8f, 0.5f);
        });
        fx.anim(14, 14, 2, t -> {
            stone.at(0, -1.6 + 1.6 * Kit.back(t), -1.0).rot(0, 0, (1 - t) * 6 * Math.sin(t * 20));
            stone.apply(2);
            fx.block(L(fx, 0, 0.1, -1.0), g, 10, 0.5, 0.1);
            fx.particle(Particle.DUST_PILLAR, L(fx, 0, 0.1, -1.0), 3, 0.4, 0.05, 0.2, 0, g.createBlockData());
        });
        // a soul leaves
        fx.later(36, () -> {
            fx.sound("minecraft:block.bell.use", 1f, 0.5f);
            fx.sound("minecraft:particle.soul_escape", 1f, 1f);
        });
        fx.later(36, () -> fx.repeat(14, 3, i -> {
            fx.p(Particle.SOUL, L(fx, fx.rand(0.3), 0.3 + i * 0.12, -1.0 + fx.rand(0.2)), 1, 0.05, 0.02);
            fx.p(Particle.SCULK_SOUL, L(fx, 0, 1.3 + i * 0.05, -1.0), 1, 0.2, 0.01);
        }));
        fx.later(95, () -> {
            stone.at(0, -1.8, -1.0).apply(14);
            fx.block(L(fx, 0, 0.1, -1.0), g, 20, 0.5, 0.1);
            fx.sound("minecraft:block.gravel.break", 0.8f, 0.6f);
        });
        fx.later(112, grave::remove);
    }

    private static void anvil(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        fx.later(2, () -> d.look((float) -fx.face(), -70f));
        // growing shadow
        Kit.Glow shadow = Kit.glow(fx, fx.base.clone().add(0, 0.03, 0), 0.2, 0.2, 0x00000000, Display.Billboard.FIXED)
                .rot(new Quaternionf().rotateX((float) (-Math.PI / 2)));
        shadow.apply(0);
        fx.anim(1, 16, 1, t -> shadow.size(0.4 + 0.8 * t, 0.4 + 0.8 * t).color(((int) (0x99 * t) << 24)).apply(1));
        Model anvil = Kit.root(fx);
        Model a = anvil.child(0, 10, 0);
        a.boxC(Material.ANVIL, 0, 0.5, 0, 1.05, 1.05, 1.05);
        a.show(0);
        fx.sound(1, "minecraft:entity.arrow.shoot", 0.6f, 0.5f);
        fx.anim(1, 16, 1, t -> {
            double y = 10 - 10 * t * t;
            a.at(0, y, 0).rot(t * 25, 0, 0).apply(1);
            if (y > 1) fx.p(Particle.CLOUD, L(fx, 0, y + 1.2, 0), 1, 0.15, 0);
        });
        fx.later(17, () -> {
            a.at(0, 0, 0).rot(0, 0, 0).size(1.0).apply(1);
            d.scale(0.0625);
            d.at(0, -1, 0);
            fx.sound("minecraft:block.anvil.land", 1.2f, 0.75f);
            fx.sound("minecraft:entity.player.big_fall", 1f, 0.6f);
            fx.sound("minecraft:entity.generic.explode", 0.4f, 1.8f);
            fx.block(fx.base.clone().add(0, 0.3, 0), Material.ANVIL, 40, 0.6, 0.2);
            fx.block(fx.base.clone().add(0, 0.1, 0), fx.groundBlock(), 40, 0.9, 0.2);
            fx.p(Particle.CRIT, fx.base.clone().add(0, 0.6, 0), 40, 0.7, 0.5);
            fx.p(Particle.CLOUD, fx.base.clone().add(0, 0.2, 0), 20, 0.8, 0.05);
            shadow.color(0).apply(3);
        });
        fx.later(18, () -> Kit.wave(fx, 1.0, 2.6, 6, 3, 0.35));
        fx.later(19, () -> a.at(0, 0, 0).size(1.12).apply(2));
        fx.later(21, () -> a.at(0, 0, 0).size(1.0).apply(3));
        fx.later(20, d::remove);
        // stars over the anvil
        fx.later(22, () -> fx.repeat(8, 4, i -> {
            double an = i * 0.8;
            fx.p(Particle.WAX_ON, L(fx, Math.cos(an) * 0.5, 1.3, Math.sin(an) * 0.5), 1, 0, 0);
        }));
        fx.later(60, () -> {
            a.at(0, -1.1, 0).apply(12);
            fx.block(fx.base.clone().add(0, 0.1, 0), fx.groundBlock(), 20, 0.6, 0.1);
        });
        fx.later(74, () -> { anvil.remove(); shadow.remove(); });
    }

    private static void partyPopper(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        fx.sound(4, "minecraft:entity.firework_rocket.launch", 1f, 1.4f);
        // a little jump
        fx.anim(4, 8, 1, t -> Kit.dollAt(fx, d, 0, 0.8 * Math.sin(t * Math.PI / 2), 0));
        fx.later(12, () -> {
            Location c = d.loc().add(0, 1.2, 0);
            Kit.firework(fx, c, FireworkEffect.Type.BALL_LARGE, true, true, G, B, Y, P);
            Kit.firework(fx, c.clone().add(Kit.dir(fx, 0.8, 0.6, 0)), FireworkEffect.Type.BURST, false, true, O, M);
            Kit.firework(fx, c.clone().add(Kit.dir(fx, -0.8, 0.5, 0)), FireworkEffect.Type.STAR, false, false, PU, C);
            fx.sound("minecraft:entity.firework_rocket.large_blast", 1.2f, 1f);
            fx.sound("minecraft:entity.firework_rocket.twinkle", 1f, 1.2f);
            fx.sound("minecraft:block.note_block.bell", 1f, 1.6f);
            Kit.burst(fx, c, 2.2, 30, Y, 10);
            Kit.burst(fx, c, 2.0, 20, P, 12);
            Kit.burst(fx, c, 2.4, 20, B, 9);
            poof(fx, d, 1.0);
        });
        fx.repeat(16, 2, i -> Kit.confetti(fx, fx.base.clone().add(0, 2.6 + i * 0.03, 0), 10, 1.6));
        fx.sound(16, "minecraft:entity.firework_rocket.twinkle_far", 0.8f, 1.4f);
        fx.sound(22, "minecraft:block.note_block.chime", 0.9f, 1.8f);
    }

    private static void bonePile(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        fx.sound(2, "minecraft:entity.skeleton.ambient", 1.2f, 0.7f);
        // rattles, shaking harder
        fx.anim(2, 14, 1, t -> {
            double j = 0.03 + 0.09 * t;
            Kit.dollAt(fx, d, fx.rand(j), 0, fx.rand(j));
            d.look((float) (-fx.face() + fx.rand(14 * t)), (float) fx.rand(10 * t));
            if ((int) (t * 14) % 3 == 0) fx.sound("minecraft:entity.skeleton.step", 1f, (float) (1.2 + t));
        });
        fx.later(16, () -> {
            Location c = fx.base.clone().add(0, 1.0, 0);
            d.remove();
            fx.sound("minecraft:entity.skeleton.death", 1.2f, 0.8f);
            fx.sound("minecraft:block.bone_block.break", 1.2f, 0.7f);
            fx.sound("minecraft:entity.skeleton.hurt", 1f, 0.6f);
            fx.p(Particle.POOF, c, 20, 0.35, 0.05);
            fx.block(c, Material.BONE_BLOCK, 50, 0.4, 0.15);
            fx.item(c, Material.BONE_MEAL, 20, 0.4, 0.1);
            // bones fly up and land in a pile
            for (int i = 0; i < 12; i++) {
                Model b = fx.model(fx.rand(0.25), 0.5 + fx.rnd.nextDouble() * 1.2, fx.rand(0.25));
                b.item(Material.BONE, 0, 0, 0, 0.55 + fx.rnd.nextDouble() * 0.25);
                double a = fx.rnd.nextDouble() * Math.PI * 2, sp = 0.05 + fx.rnd.nextDouble() * 0.09;
                Kit.fly(fx, b, new Vector(Math.cos(a) * sp, 0.25 + fx.rnd.nextDouble() * 0.2, Math.sin(a) * sp), 62, 0.12);
            }
            for (int i = 0; i < 3; i++) {
                Model b = fx.model(fx.rand(0.2), 0.6, fx.rand(0.2));
                b.cube(Material.BONE_BLOCK, 0, 0, 0, 0.32);
                double a = fx.rnd.nextDouble() * Math.PI * 2;
                Kit.fly(fx, b, new Vector(Math.cos(a) * 0.06, 0.22, Math.sin(a) * 0.06), 62, 0.32);
            }
            // the skull pops up spinning and lands on top, facing the killer
            Model root = Kit.root(fx);
            Model skull = root.child(0, 1.4, 0);
            skull.item(Material.SKELETON_SKULL, 0, 0, 0, 1.3);
            skull.show(0);
            fx.anim(1, 16, 1, tt -> {
                double y = 1.4 + 1.8 * Math.sin(tt * Math.PI) - 1.15 * tt;
                skull.at(0, Math.max(0.33, y), 0.1).rot(tt * 720, 0, 0).apply(1);
            });
            fx.later(18, () -> {
                skull.at(0, 0.33, 0.1).rot(0, -8, 4).apply(2);
                fx.sound("minecraft:block.bone_block.place", 1.2f, 0.8f);
                fx.block(fx.base.clone().add(0, 0.2, 0), fx.groundBlock(), 12, 0.3, 0.05);
            });
            fx.later(20, () -> skull.at(0, 0.36, 0.1).rot(0, -10, 6).size(1.08).apply(2));
            fx.later(22, () -> skull.at(0, 0.33, 0.1).rot(0, -8, 4).size(1).apply(3));
            // a soul leaves, then the pile crumbles to dust
            fx.later(26, () -> fx.sound("minecraft:particle.soul_escape", 1.2f, 1f));
            fx.later(26, () -> fx.repeat(10, 3, i -> fx.p(Particle.SOUL, fx.base.clone().add(fx.rand(0.3), 0.5 + i * 0.15, fx.rand(0.3)), 1, 0.05, 0.02)));
            fx.later(58, () -> {
                skull.at(0, -0.3, 0.1).size(0.001).apply(6);
                fx.block(fx.base.clone().add(0, 0.3, 0), Material.BONE_BLOCK, 30, 0.5, 0.05);
                fx.particle(Particle.FALLING_DUST, fx.base.clone().add(0, 0.5, 0), 20, 0.4, 0.2, 0.4, 0, Material.BONE_BLOCK.createBlockData());
                fx.sound("minecraft:block.sand.break", 1f, 0.8f);
            });
            fx.later(66, root::remove);
        });
    }

    // ==================================================================== uncommon

    private static void frozen(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        fx.sound("minecraft:entity.player.hurt_freeze", 1f, 0.8f);
        fx.sound(2, "minecraft:block.powder_snow.place", 1f, 0.6f);
        // frost spreads on the ground
        fx.anim(0, 8, 1, t -> fx.dustRing(0.05, 0.4 + t * 1.2, 18, 0xDFF4FF, 1.2f));
        fx.repeat(10, 1, i -> fx.p(Particle.SNOWFLAKE, fx.base.clone().add(0, 0.2 + i * 0.2, 0), 6, 0.5, 0.01));
        // the ice grows upwards
        Model ice = Kit.root(fx);
        Model block = ice.child(0, 0, 0);
        block.boxC(Material.ICE, 0, 1.05, 0, 1.15, 2.1, 1.15)
                .boxC(Material.PACKED_ICE, 0, 0.06, 0, 1.3, 0.12, 1.3)
                .boxC(Material.PACKED_ICE, 0, 2.12, 0, 1.25, 0.1, 1.25);
        block.size(0.001).show(0);
        fx.later(3, () -> {
            block.size(1.0).apply(7);
            fx.sound("minecraft:block.glass.place", 1f, 0.6f);
            fx.sound("minecraft:block.amethyst_block.place", 1f, 0.5f);
        });
        // cracks
        for (int k = 0; k < 3; k++) {
            int at = 24 + k * 7;
            float pitch = 0.9f + k * 0.2f;
            fx.later(at, () -> {
                fx.sound("minecraft:block.glass.hit", 1f, pitch);
                block.at(fx.rand(0.03), 0, fx.rand(0.03)).rot(0, fx.rand(1.5), fx.rand(1.5)).apply(1);
                fx.block(fx.base.clone().add(0, 1 + fx.rand(0.6), 0), Material.ICE, 6, 0.45, 0.05);
            });
            fx.later(at + 2, () -> block.at(0, 0, 0).rot(0, 0, 0).apply(2));
        }
        // shatter
        fx.later(46, () -> {
            Location c = fx.base.clone().add(0, 1.0, 0);
            block.remove();
            d.remove();
            fx.sound("minecraft:block.glass.break", 1.2f, 0.7f);
            fx.sound("minecraft:block.amethyst_cluster.break", 1f, 0.6f);
            fx.sound("minecraft:entity.player.hurt_freeze", 1f, 1.2f);
            Kit.debris(fx, c, Material.ICE, 10, 0.22, 0.32, 0.38, 26);
            Kit.debris(fx, c.clone().add(0, 0.6, 0), Material.PACKED_ICE, 6, 0.25, 0.4, 0.3, 26);
            Kit.debris(fx, c, Material.BLUE_ICE, 4, 0.2, 0.3, 0.25, 24);
            fx.block(c, Material.ICE, 60, 0.5, 0.25);
            fx.p(Particle.SNOWFLAKE, c, 60, 0.6, 0.12);
            fx.p(Particle.END_ROD, c, 12, 0.5, 0.06);
            Kit.burst(fx, c, 2.0, 24, 0xBFE8FF, 8);
        });
    }

    private static void tnt(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Model root = Kit.root(fx);
        Model tnt = root.child(0.75, 2.8, 0.35);
        tnt.boxC(Material.TNT, 0, 0.42, 0, 0.84, 0.84, 0.84);
        tnt.show(0);
        fx.sound(1, "minecraft:entity.tnt.primed", 1.2f, 1f);
        // the TNT drops in front of them
        fx.anim(1, 6, 1, t -> tnt.at(0.75, 2.8 * (1 - t * t), 0.35).apply(1));
        fx.later(8, () -> {
            fx.sound("minecraft:block.wool.place", 1f, 0.6f);
            fx.block(L(fx, 0.75, 0.1, 0.35), fx.groundBlock(), 12, 0.4, 0.05);
        });
        // fuse: blinks faster and faster, swells
        int[] blinks = {14, 20, 25, 29, 32, 35, 37, 39, 41, 43};
        for (int i = 0; i < blinks.length; i++) {
            int at = blinks[i];
            double s = 1.0 + 0.012 * i;
            fx.later(at, () -> {
                tnt.parts.forEach(p -> { if (p.d instanceof org.bukkit.entity.BlockDisplay bd) bd.setBlock(Material.WHITE_CONCRETE.createBlockData()); });
                tnt.size(s + 0.05).apply(1);
            });
            fx.later(at + 1, () -> {
                tnt.parts.forEach(p -> { if (p.d instanceof org.bukkit.entity.BlockDisplay bd) bd.setBlock(Material.TNT.createBlockData()); });
                tnt.size(s).apply(1);
            });
        }
        fx.repeat(18, 2, i -> fx.p(Particle.SMOKE, L(fx, 0.75, 1.15, 0.35), 2, 0.05, 0.01));
        fx.repeat(18, 2, i -> fx.p(Particle.FLAME, L(fx, 0.75, 1.1, 0.35), 1, 0.02, 0.005));
        fx.later(46, () -> {
            root.remove();
            Location c = L(fx, 0.75, 0.6, 0.35);
            fx.p(Particle.EXPLOSION_EMITTER, c, 1, 0, 0);
            fx.p(Particle.EXPLOSION, c, 6, 1.0, 0);
            fx.p(Particle.LARGE_SMOKE, c, 10, 0.9, 0.06);
            fx.p(Particle.FLAME, c, 30, 0.5, 0.15);
            fx.particle(Particle.FLASH, c.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0, Kit.col(0xFFE7A0));
            fx.sound("minecraft:entity.generic.explode", 1.4f, 0.9f);
            fx.sound("minecraft:entity.dragon_fireball.explode", 0.8f, 1.2f);
            Kit.debris(fx, c, fx.groundBlock(), 12, 0.18, 0.5, 0.4, 30);
            Kit.debris(fx, c, Material.TNT, 3, 0.2, 0.5, 0.3, 26);
        });
        // the body is launched backwards and up, spinning
        fx.anim(46, 18, 1, t -> {
            double back = -5.5 * t, up = 6 * t - 6 * t * t * 0.9;
            Kit.dollAt(fx, d, 0.6 * t, up, back);
            d.look((float) (-fx.face() + t * 720), (float) (t * 180));
            fx.p(Particle.LARGE_SMOKE, d.loc().add(0, 0.8, 0), 1, 0.1, 0.01);
            fx.p(Particle.FLAME, d.loc().add(0, 0.8, 0), 2, 0.15, 0.01);
        });
        fx.later(65, () -> poof(fx, d, 0.8));
    }

    private static void groundSlam(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        fx.sound(2, "minecraft:entity.breeze.wind_burst", 1f, 0.8f);
        fx.sound(2, "minecraft:entity.breeze.idle_ground", 1f, 1.2f);
        fx.later(2, () -> fx.p(Particle.GUST_EMITTER_SMALL, fx.base.clone().add(0, 0.3, 0), 1, 0, 0));
        // lifted by the wind, spinning slowly
        fx.anim(3, 16, 1, t -> {
            double y = 2.9 * Kit.easeOut(t);
            Kit.dollAt(fx, d, 0, y, 0);
            d.look((float) (-fx.face() + t * 200), 0);
            fx.p(Particle.GUST, d.loc().add(0, -0.2, 0), 1, 0, 0);
            fx.p(Particle.WHITE_SMOKE, d.loc().add(0, 0.1, 0), 3, 0.25, 0.01);
            fx.p(Particle.END_ROD, d.loc().add(fx.rand(0.5), fx.rand(0.5) + 0.8, fx.rand(0.5)), 1, 0, 0.01);
        });
        // hang for a moment
        fx.later(19, () -> fx.sound("minecraft:item.mace.smash_air", 1f, 0.7f));
        fx.repeat(6, 1, i -> fx.p(Particle.ELECTRIC_SPARK, d.loc().add(0, 1, 0), 3, 0.4, 0.05));
        // slam
        fx.anim(24, 3, 1, t -> Kit.dollAt(fx, d, 0, 3.0 * (1 - t * t), 0));
        fx.later(27, () -> {
            d.remove();
            Location c = fx.base.clone().add(0, 0.2, 0);
            fx.sound("minecraft:item.mace.smash_ground_heavy", 1.4f, 0.8f);
            fx.sound("minecraft:entity.generic.explode", 0.7f, 0.6f);
            fx.sound("minecraft:entity.player.big_fall", 1f, 0.5f);
            fx.particle(Particle.DUST_PILLAR, c, 60, 1.2, 0.1, 1.2, 0, fx.groundBlock().createBlockData());
            fx.block(c, fx.groundBlock(), 60, 1.0, 0.3);
            fx.p(Particle.EXPLOSION, c.clone().add(0, 0.3, 0), 2, 0.3, 0);
            fx.p(Particle.CLOUD, c, 30, 1.2, 0.05);
            Kit.wave(fx, 0.9, 3.6, 10, 4, 0.55);
            Kit.debris(fx, c, fx.groundBlock(), 8, 0.12, 0.45, 0.35, 28);
        });
    }

    // ==================================================================== rare

    private static void rocket(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Model root = Kit.root(fx);
        Model rk = root.child(0.6, 0, -0.3);
        rk.item(Material.FIREWORK_ROCKET, 0, 1.3, 0, 3.0);
        rk.rot(0, 0, 0);
        rk.show(4);
        fx.sound(3, "minecraft:block.chain.place", 1f, 0.8f);
        fx.sound(8, "minecraft:entity.creeper.primed", 0.8f, 1.6f);
        fx.repeat(10, 2, i -> fx.p(Particle.FIREWORK, L(fx, 0.6, 0.1, -0.3), 3, 0.1, 0.05));
        fx.repeat(10, 2, i -> fx.p(Particle.SMOKE, L(fx, 0.6, 0.1, -0.3), 2, 0.1, 0.01));
        fx.later(28, () -> {
            fx.sound("minecraft:entity.firework_rocket.launch", 1.4f, 0.6f);
            fx.sound("minecraft:entity.firework_rocket.launch", 1.2f, 0.9f);
            fx.p(Particle.CAMPFIRE_COSY_SMOKE, fx.base.clone().add(0, 0.2, 0), 12, 0.4, 0.02);
            fx.p(Particle.EXPLOSION, fx.base.clone().add(0, 0.3, 0), 1, 0, 0);
        });
        fx.anim(28, 30, 1, t -> {
            double y = 22 * Kit.easeIn(t);
            Kit.dollAt(fx, d, 0, y, 0);
            d.look((float) (-fx.face() + t * 540), 0);
            rk.at(0.6, y, -0.3).apply(1);
            Location tail = d.loc().add(0, -0.2, 0);
            fx.p(Particle.FIREWORK, tail, 4, 0.08, 0.04);
            fx.p(Particle.FLAME, tail, 3, 0.06, 0.02);
            fx.p(Particle.WHITE_SMOKE, tail, 2, 0.08, 0.01);
        });
        fx.later(58, () -> {
            Location top = L(fx, 0, 22.5, 0);
            Kit.firework(fx, top, FireworkEffect.Type.BALL_LARGE, true, true, Y, O, R);
            Kit.firework(fx, top, FireworkEffect.Type.STAR, false, true, B, C);
            d.remove();
            root.remove();
        });
    }

    private static void vortex(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Location c = fx.base.clone().add(0, 0.04, 0);
        Kit.Ring outer = new Kit.Ring(fx, c, 0.2, 0.16, 20, 0xCCA303F9);
        Kit.Ring mid = new Kit.Ring(fx, c.clone().add(0, 0.01, 0), 0.15, 0.12, 16, 0xCCFC00E3);
        Kit.Ring inner = new Kit.Ring(fx, c.clone().add(0, 0.02, 0), 0.1, 0.1, 12, 0xEE1EB8E9);
        Kit.Disc hole = new Kit.Disc(fx, c.clone().add(0, -0.01, 0), 0x00100018, true);
        outer.apply(0); mid.apply(0); inner.apply(0);
        fx.sound(2, "minecraft:block.end_portal.spawn", 0.6f, 1.6f);
        fx.sound(2, "minecraft:block.portal.trigger", 0.6f, 1.4f);
        fx.anim(2, 50, 1, t -> {
            double open = Math.min(1, t * 3), close = t > 0.85 ? (1 - t) / 0.15 : 1;
            double k = open * close;
            outer.r = 1.9 * k; outer.spin += 0.18; outer.apply(1);
            mid.r = 1.35 * k; mid.spin -= 0.26; mid.apply(1);
            inner.r = 0.8 * k; inner.spin += 0.38; inner.apply(1);
            hole.set(2.9 * k, 0xC0100018, 1);
            for (int i = 0; i < 3; i++) {
                double a = fx.rnd.nextDouble() * Math.PI * 2, r = 2.4;
                Location from = c.clone().add(Math.cos(a) * r, 0.1 + fx.rnd.nextDouble() * 0.6, Math.sin(a) * r);
                Kit.streak(fx, from, c, i % 2 == 0 ? PU : P, 10);
            }
            fx.p(Particle.REVERSE_PORTAL, c.clone().add(0, 0.3, 0), 3, 0.8, 0.02);
        });
        // spins faster and is pulled down into the portal
        fx.anim(10, 34, 1, t -> {
            double spin = 1800 * t * t;
            Kit.dollAt(fx, d, 0, -2.0 * Kit.easeIn(t), 0);
            d.look((float) (-fx.face() + spin), 0);
            d.scale(1 - 0.7 * t);
            fx.p(Particle.PORTAL, d.loc().add(0, 0.8, 0), 6, 0.3, 0.4);
        });
        fx.sound(20, "minecraft:entity.enderman.teleport", 1f, 0.6f);
        fx.sound(38, "minecraft:entity.enderman.teleport", 1f, 0.9f);
        fx.later(45, () -> {
            d.remove();
            fx.particle(Particle.FLASH, c.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0, Kit.col(PU));
            fx.p(Particle.REVERSE_PORTAL, c.clone().add(0, 0.4, 0), 80, 0.4, 0.6);
            fx.p(Particle.END_ROD, c.clone().add(0, 0.4, 0), 20, 0.2, 0.15);
            fx.sound("minecraft:block.respawn_anchor.deplete", 1f, 0.8f);
            fx.sound("minecraft:entity.shulker.teleport", 1f, 0.6f);
        });
        fx.later(54, () -> { outer.remove(); mid.remove(); inner.remove(); hole.remove(); });
    }

    private static void balloon(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Model root = Kit.root(fx);
        Model head = root.child(0, 1.62, 0);
        head.head(0, 0, 0, 0.6);
        head.show(1);
        Model string = root.child(0, 0, 0);
        fx.later(4, () -> fx.sound("minecraft:entity.puffer_fish.blow_up", 1f, 0.8f));
        fx.later(16, () -> fx.sound("minecraft:entity.puffer_fish.blow_up", 1f, 0.6f));
        fx.later(28, () -> fx.sound("minecraft:entity.puffer_fish.blow_up", 1f, 0.45f));
        // the head inflates in pumps and lifts the body
        fx.anim(4, 40, 1, t -> {
            double pump = t + 0.06 * Math.sin(t * 40);
            double s = 0.6 + 1.6 * Kit.easeInOut(Math.min(1, pump));
            double lift = 1.3 * Kit.easeIn(t);
            Kit.dollAt(fx, d, 0, lift, 0);
            head.at(0, lift + 1.45 + s * 0.42, 0).size(s / 0.6).rot(Math.sin(t * 9) * 8, 0, Math.sin(t * 7) * 6).apply(1);
            if ((int) (t * 40) % 4 == 0) fx.p(Particle.CLOUD, d.loc().add(0, 1.6, 0), 1, 0.1, 0.01);
        });
        fx.repeat(10, 4, i -> fx.p(Particle.WHITE_SMOKE, d.loc().add(0, 1.3, 0), 2, 0.3, 0.01));
        fx.later(45, () -> fx.sound("minecraft:entity.puffer_fish.blow_out", 1f, 1.8f));
        fx.later(48, () -> {
            Location pop = d.loc().add(0, 2.5, 0);
            head.remove();
            string.remove();
            Kit.firework(fx, pop, FireworkEffect.Type.BURST, false, true, R, Y, B);
            fx.sound("minecraft:entity.firework_rocket.blast", 1.3f, 1.4f);
            fx.sound("minecraft:entity.generic.explode", 0.5f, 2f);
            fx.p(Particle.POOF, pop, 40, 0.6, 0.12);
            Kit.burst(fx, pop, 2.4, 30, W, 8);
            Kit.confetti(fx, pop, 30, 1.0);
        });
        // the body drops
        fx.anim(49, 8, 1, t -> Kit.dollAt(fx, d, 0, 1.3 * (1 - t * t), 0));
        fx.later(59, () -> {
            fx.sound("minecraft:entity.player.big_fall", 1f, 0.8f);
            fx.block(fx.base.clone().add(0, 0.1, 0), fx.groundBlock(), 20, 0.5, 0.1);
            poof(fx, d, 0.6);
        });
        fx.later(62, root::remove);
    }

    // ==================================================================== epic

    private static void explode(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        fx.sound(2, "minecraft:entity.creeper.primed", 1.2f, 0.7f);
        fx.sound(10, "minecraft:block.beacon.power_select", 1f, 1.6f);
        Kit.Disc core = Kit.disc(fx, L(fx, 0, 1.1, -0.35), 0x00FFFFFF);
        // charge: trembles, energy streaks rush into the body, it glows
        fx.anim(2, 30, 1, t -> {
            double j = 0.04 + 0.1 * t;
            Kit.dollAt(fx, d, fx.rand(j), fx.rand(j * 0.3), fx.rand(j));
            Location c = fx.base.clone().add(0, 1.1, 0);
            Kit.converge(fx, c, 2.6 - t, 2 + (int) (t * 5), t < 0.5 ? O : R, 6);
            fx.dust(c, R, 1.2f, 3, 0.35);
            int a = (int) (0xC0 * t * t);
            core.set(0.6 + 1.4 * t, (a / 3) << 24 | 0xFFC030, 1);
            if ((int) (t * 30) % 6 == 0) fx.sound("minecraft:block.note_block.basedrum", 1f, (float) (0.5 + t));
        });
        fx.later(32, () -> {
            Location c = fx.base.clone().add(0, 1.0, 0);
            Rig rig = fx.rig(0, 0, 0);
            rig.show(0);
            d.remove();
            core.set(8, 0xC0FFFFFF, 2);
            fx.particle(Particle.FLASH, c, 1, 0, 0, 0, 0, Kit.col(0xFFFFFF));
            fx.p(Particle.EXPLOSION_EMITTER, c, 1, 0, 0);
            fx.p(Particle.EXPLOSION, c, 8, 1.4, 0);
            fx.p(Particle.FLAME, c, 50, 0.6, 0.25);
            fx.p(Particle.LAVA, c, 20, 0.5, 0);
            fx.p(Particle.LARGE_SMOKE, c, 12, 1.0, 0.05);
            Kit.burst(fx, c, 4.5, 40, O, 12);
            Kit.burst(fx, c, 3.5, 30, Y, 10);
            fx.sound("minecraft:entity.generic.explode", 1.6f, 0.7f);
            fx.sound("minecraft:entity.dragon_fireball.explode", 1f, 0.8f);
            fx.sound("minecraft:entity.firework_rocket.large_blast", 1f, 0.6f);
            rig.root.shatter(1, 18, 3.0, 0.9);
            Kit.debris(fx, fx.base.clone().add(0, 0.3, 0), fx.groundBlock(), 14, 0.22, 0.55, 0.42, 32);
            Kit.wave(fx, 1.0, 3.6, 8, 4, 0.5);
        });
        fx.later(34, () -> core.set(10, 0x00FFFFFF, 6));
        fx.later(42, core::remove);
        fx.later(38, () -> fx.repeat(8, 4, i -> fx.p(Particle.CAMPFIRE_COSY_SMOKE, fx.base.clone().add(fx.rand(1), 0.2, fx.rand(1)), 1, 0.1, 0.02)));
    }

    /** One layer of the big 3D "EZ": bold text, transparent background, facing the killer. */
    private static org.bukkit.entity.TextDisplay ezLayer(Fx fx, Location at, String hex, float scale, float back) {
        org.bukkit.entity.TextDisplay t = fx.world.spawn(at, org.bukkit.entity.TextDisplay.class, d -> {
            d.text(net.kyori.adventure.text.Component.text("EZ").color(net.kyori.adventure.text.format.TextColor.fromHexString(hex))
                    .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
            d.setDefaultBackground(false);
            d.setBackgroundColor(org.bukkit.Color.fromARGB(0));
            d.setShadowed(false);
            d.setSeeThrough(false);
            fx.prepareDisplay(d);
            d.setBillboard(Display.Billboard.FIXED);
            d.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(0, 0, -back),
                    new Quaternionf().rotateY((float) Math.toRadians(fx.face())), new org.joml.Vector3f(0.001f, 0.001f, 0.001f), new Quaternionf()));
        });
        fx.trackEntity(t);
        return t;
    }

    private static void ez(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        // 3D letters: a bright front layer and darker layers behind it
        String[] cols = {"#FFF27A", "#FCE300", "#FCC800", "#F9A600", "#F97603", "#D85A00", "#A84300", "#7A3000"};
        Location at = L(fx, 0, 9, -2.6);
        at.setYaw(0);
        at.setPitch(0);
        List<org.bukkit.entity.TextDisplay> layers = new ArrayList<>();
        for (int i = 0; i < cols.length; i++) layers.add(ezLayer(fx, at, cols[i], 0.001f, i * 0.035f));
        java.util.function.BiConsumer<Double, Integer> pose = (sc, dur) -> {
            for (int i = 0; i < layers.size(); i++) {
                org.bukkit.entity.TextDisplay t = layers.get(i);
                if (!t.isValid()) continue;
                Quaternionf q = new Quaternionf().rotateY((float) Math.toRadians(fx.face()));
                org.joml.Vector3f off = q.transform(new org.joml.Vector3f(0, 0, (float) (-i * 0.035 * sc / 6)));
                t.setInterpolationDelay(0);
                t.setInterpolationDuration(dur);
                float s = (float) Math.max(0.001, sc);
                t.setTransformation(new org.bukkit.util.Transformation(off, q, new org.joml.Vector3f(s, s, s), new Quaternionf()));
            }
        };
        fx.later(1, () -> pose.accept(10.0, 0));
        // the doll looks up at it
        fx.later(3, () -> d.look((float) -fx.face(), -45f));
        fx.sound(2, "minecraft:entity.firework_rocket.launch", 1f, 0.5f);
        fx.sound(4, "minecraft:block.note_block.pling", 1f, 0.6f);
        fx.sound(8, "minecraft:block.note_block.pling", 1f, 0.9f);
        // falls from the sky and slams into the ground behind them
        fx.anim(2, 12, 1, t -> {
            Location l = L(fx, 0, 9 - 9 * t * t, -2.6);
            l.setYaw(0);
            l.setPitch(0);
            for (org.bukkit.entity.TextDisplay layer : layers) {
                if (!layer.isValid()) continue;
                layer.setTeleportDuration(1);
                layer.teleport(l);
            }
            fx.p(Particle.END_ROD, l.clone().add(0, 1.0, 0), 2, 0.8, 0.02);
        });
        fx.later(14, () -> {
            pose.accept(12.0, 2);
            Location g = L(fx, 0, 0.1, -2.6);
            fx.sound("minecraft:item.mace.smash_ground_heavy", 1.4f, 0.9f);
            fx.sound("minecraft:entity.generic.explode", 0.8f, 1.2f);
            fx.sound("minecraft:block.anvil.land", 0.8f, 1.4f);
            fx.particle(Particle.DUST_PILLAR, g, 40, 1.4, 0.1, 0.4, 0, fx.groundBlock().createBlockData());
            fx.p(Particle.CLOUD, g, 20, 1.2, 0.04);
            Kit.burst(fx, g.clone().add(0, 1, 0), 4, 30, Kit.Y, 10);
            Kit.wave(fx, 1.0, 3.2, 8, 3, 0.45);
        });
        fx.later(16, () -> pose.accept(9.2, 2));
        fx.later(18, () -> pose.accept(10.0, 3));
        // wobble while the doll gets laughed out
        fx.sound(22, "minecraft:entity.villager.no", 1.2f, 1.4f);
        fx.sound(28, "minecraft:entity.goat.screaming.ambient", 0.8f, 1.6f);
        fx.repeat(5, 4, i -> fx.later(22, () -> {
            for (org.bukkit.entity.TextDisplay layer : layers) {
                if (!layer.isValid()) continue;
                Quaternionf q = new Quaternionf().rotateY((float) Math.toRadians(fx.face())).rotateZ((float) Math.toRadians(i % 2 == 0 ? 5 : -5));
                org.bukkit.util.Transformation tr = layer.getTransformation();
                layer.setInterpolationDelay(0);
                layer.setInterpolationDuration(4);
                layer.setTransformation(new org.bukkit.util.Transformation(tr.getTranslation(), q, tr.getScale(), new Quaternionf()));
            }
        }));
        // the doll shrinks with embarrassment and is flicked away
        fx.anim(22, 14, 1, t -> {
            d.scale(1 - 0.6 * t);
            d.look((float) (-fx.face() + Math.sin(t * 18) * 30), 20f);
        });
        fx.later(36, () -> {
            fx.sound("minecraft:entity.player.attack.knockback", 1.2f, 1.2f);
            fx.p(Particle.SWEEP_ATTACK, d.loc().add(0, 0.5, 0), 1, 0, 0);
        });
        fx.anim(36, 10, 1, t -> {
            Kit.dollAt(fx, d, -3 * t, 2.2 * Math.sin(t * Math.PI), 1.0 * t);
            d.look((float) (-fx.face() + t * 900), 0);
        });
        fx.later(46, () -> {
            Kit.firework(fx, d.loc().add(0, 0.6, 0), FireworkEffect.Type.BURST, false, true, Kit.Y, Kit.O);
            poof(fx, d, 0.5);
        });
        // the letters burst into fireworks
        fx.later(54, () -> {
            Location c = L(fx, 0, 1.6, -2.6);
            pose.accept(12.5, 3);
            Kit.firework(fx, c, FireworkEffect.Type.BALL_LARGE, true, true, Kit.Y, Kit.O, 0xFFFFFF);
            Kit.firework(fx, c.clone().add(Kit.dir(fx, 2.0, 0.3, 0)), FireworkEffect.Type.STAR, false, true, Kit.Y);
            Kit.firework(fx, c.clone().add(Kit.dir(fx, -2.0, 0.3, 0)), FireworkEffect.Type.STAR, false, true, Kit.O);
            fx.sound("minecraft:entity.firework_rocket.large_blast", 1.2f, 1f);
            fx.sound("minecraft:entity.firework_rocket.twinkle", 1f, 1.2f);
        });
        fx.later(57, () -> pose.accept(0.001, 4));
        fx.later(62, () -> layers.forEach(t -> { if (t.isValid()) t.remove(); }));
    }

    /** A jagged bolt from the sky made of glowing planes. */
    private static List<Kit.Glow> bolt(Fx fx, Location ground, double height, int rgb) {
        List<Kit.Glow> parts = new ArrayList<>();
        double x = 0, z = 0;
        double step = height / 9;
        for (int i = 0; i < 9; i++) {
            double y0 = height - i * step, y1 = y0 - step;
            double nx = i == 8 ? 0 : x + fx.rand(0.7), nz = i == 8 ? 0 : z + fx.rand(0.7);
            Location a = ground.clone().add(x, y0, z), b = ground.clone().add(nx, y1, nz);
            Location mid = a.clone().add(b).multiply(0.5);
            Vector dv = b.toVector().subtract(a.toVector());
            double len = dv.length();
            Quaternionf q = new Quaternionf().rotateTo(0, 1, 0, (float) dv.getX(), (float) dv.getY(), (float) dv.getZ());
            for (int k = 0; k < 2; k++) {
                double w = k == 0 ? 0.55 : 0.18;
                int c = k == 0 ? (0x70 << 24 | rgb) : 0xF8FFFFFF;
                Kit.Glow g = Kit.glow(fx, mid, w, len + 0.1, c, Display.Billboard.FIXED);
                g.rot(new Quaternionf(q).rotateY((float) Math.toRadians(fx.face()))).apply(0);
                Kit.Glow g2 = Kit.glow(fx, mid, w, len + 0.1, c, Display.Billboard.FIXED);
                g2.rot(new Quaternionf(q).rotateY((float) Math.toRadians(fx.face() + 90))).apply(0);
                parts.add(g);
                parts.add(g2);
            }
            x = nx;
            z = nz;
        }
        return parts;
    }

    private static void strike(Fx fx, Location at, boolean last) {
        List<Kit.Glow> b = bolt(fx, at, 24, 0x9FD8FF);
        fx.particle(Particle.FLASH, at.clone().add(0, 1, 0), 1, 0, 0, 0, 0, Kit.col(0xDDEEFF));
        fx.p(Particle.ELECTRIC_SPARK, at.clone().add(0, 0.5, 0), 40, 0.5, 0.4);
        fx.p(Particle.EXPLOSION, at.clone().add(0, 0.3, 0), 1, 0, 0);
        fx.block(at.clone().add(0, 0.1, 0), fx.groundBlock(), 25, 0.6, 0.2);
        fx.sound("minecraft:entity.lightning_bolt.thunder", last ? 1.2f : 0.8f, last ? 0.8f : 1.1f);
        fx.sound("minecraft:entity.lightning_bolt.impact", 1.2f, 0.8f);
        fx.later(2, () -> b.forEach(g -> g.color(g.argb & 0x00FFFFFF | 0x60 << 24).apply(2)));
        fx.later(3, () -> b.forEach(g -> g.color(g.argb | 0xFF000000).apply(0)));
        fx.later(5, () -> b.forEach(g -> g.size(0.001, g.h).apply(3)));
        fx.later(9, () -> b.forEach(Kit.Glow::remove));
    }

    private static void smite(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        fx.sound(2, "minecraft:item.trident.thunder", 0.8f, 1.2f);
        fx.repeat(10, 2, i -> fx.p(Particle.ELECTRIC_SPARK, fx.base.clone().add(0, 1, 0), 4, 0.5, 0.1));
        fx.later(8, () -> strike(fx, L(fx, 1.8, 0, -0.8), false));
        fx.later(18, () -> strike(fx, L(fx, -1.6, 0, -0.4), false));
        fx.later(30, () -> {
            strike(fx, fx.base.clone(), true);
            Rig ash = fx.rig(0, 0, 0);
            ash.recolor(Material.BLACKSTONE);
            ash.show(0);
            d.remove();
            fx.later(1, () -> { ash.recolor(Material.COAL_BLOCK); headTo(ash, Material.COAL_BLOCK); });
            fx.p(Particle.LARGE_SMOKE, fx.base.clone().add(0, 1, 0), 20, 0.4, 0.03);
            fx.p(Particle.FLAME, fx.base.clone().add(0, 1, 0), 20, 0.4, 0.04);
            fx.sound("minecraft:block.fire.extinguish", 1f, 0.6f);
            fx.later(1, () -> Kit.wave(fx, 0.8, 2.6, 5, 3, 0.35));
            // smoulders, then crumbles to ash
            fx.repeat(12, 3, i -> {
                fx.p(Particle.SMOKE, fx.base.clone().add(fx.rand(0.25), 0.4 + fx.rnd.nextDouble() * 1.5, fx.rand(0.25)), 2, 0.05, 0.01);
                fx.p(Particle.ELECTRIC_SPARK, fx.base.clone().add(0, 1, 0), 1, 0.3, 0.05);
            });
            fx.later(36, () -> {
                fx.sound("minecraft:block.sand.break", 1.2f, 0.6f);
                fx.sound("minecraft:block.soul_sand.break", 1f, 0.5f);
                fx.block(fx.base.clone().add(0, 1, 0), Material.COAL_BLOCK, 50, 0.35, 0.05);
                fx.particle(Particle.FALLING_DUST, fx.base.clone().add(0, 1.2, 0), 30, 0.3, 0.6, 0.3, 0, Material.BLACK_CONCRETE_POWDER.createBlockData());
                ash.root.shatter(0, 16, 0.6, -0.4);
            });
        });
    }

    private static void phantom(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Phantom ph = Kit.mob(fx, Phantom.class, L(fx, -8, 9, -6), 2.2);
        ph.setShouldBurnInDay(false);
        ph.setSize(1);
        fx.sound(1, "minecraft:entity.phantom.ambient", 1.2f, 0.6f);
        fx.sound(10, "minecraft:entity.phantom.swoop", 1.4f, 0.8f);
        // swoop down in a curve
        fx.anim(1, 22, 1, t -> {
            double e = Kit.easeInOut(t);
            double x = -8 + 8 * e, z = -6 + 6 * e, y = 9 - 6.4 * e + 1.2 * Math.sin(e * Math.PI);
            Location l = L(fx, x, y, z);
            Vector v = Kit.dir(fx, 8, -6.4, 6);
            l.setDirection(v);
            Kit.move(ph, l);
            fx.p(Particle.SOUL_FIRE_FLAME, l.clone().add(0, 0.3, 0), 1, 0.3, 0.01);
        });
        fx.later(23, () -> {
            fx.sound("minecraft:entity.phantom.bite", 1.4f, 0.7f);
            fx.sound("minecraft:entity.player.hurt", 1f, 0.6f);
            fx.p(Particle.SWEEP_ATTACK, fx.base.clone().add(0, 1.4, 0), 1, 0, 0);
            fx.p(Particle.SOUL, fx.base.clone().add(0, 1.2, 0), 8, 0.3, 0.03);
        });
        // flies away with them
        fx.anim(23, 34, 1, t -> {
            double e = Kit.easeIn(t);
            double x = 9 * e, z = -7 * e, y = 2.6 + 14 * e + Math.sin(t * Math.PI * 4) * 0.2;
            Location l = L(fx, x, y, z);
            l.setDirection(Kit.dir(fx, 9, 14, -7));
            Kit.move(ph, l);
            Kit.dollAt(fx, d, x, y - 2.0, z);
            d.look((float) (-fx.face() + Math.sin(t * 20) * 25), 20f);
            fx.p(Particle.SOUL_FIRE_FLAME, l.clone().add(0, 0.3, 0), 1, 0.4, 0.01);
            fx.p(Particle.WHITE_SMOKE, d.loc().add(0, 0.6, 0), 1, 0.1, 0.01);
            if ((int) (t * 34) % 6 == 0) fx.sound("minecraft:entity.phantom.flap", 1.2f, 0.8f);
        });
        fx.later(58, () -> {
            fx.p(Particle.SOUL, d.loc().add(0, 1, 0), 20, 0.5, 0.05);
            poof(fx, d, 1);
            if (ph.isValid()) ph.remove();
            fx.sound("minecraft:entity.phantom.death", 1f, 0.7f);
        });
    }

    // ==================================================================== legendary

    private static void meteor(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Location g = fx.base.clone().add(0, 0.04, 0);
        // warning circle
        Kit.Ring warn = new Kit.Ring(fx, g, 1.6, 0.12, 24, 0xC0FC0000);
        warn.apply(0);
        Kit.Ring warn2 = new Kit.Ring(fx, g.clone().add(0, 0.01, 0), 0.9, 0.08, 16, 0x90FC0000);
        warn2.apply(0);
        fx.repeat(5, 6, i -> {
            fx.sound("minecraft:block.note_block.bit", 1f, 0.6f);
            warn.argb = i % 2 == 0 ? 0xE0FC0000 : 0x70FC0000;
            warn.spin += 0.2;
            warn.apply(2);
            warn2.spin -= 0.3;
            warn2.argb = i % 2 == 0 ? 0x60FC0000 : 0xC0FC0000;
            warn2.apply(2);
        });
        // the meteor
        Model root = Kit.root(fx);
        Model rock = root.child(-9, 12, -4);
        rock.cube(Material.MAGMA_BLOCK, 0, 0, 0, 1.4)
                .cube(Material.BLACKSTONE, 0.5, 0.3, 0.1, 0.8)
                .cube(Material.OBSIDIAN, -0.45, -0.2, 0.35, 0.7)
                .cube(Material.MAGMA_BLOCK, 0.1, 0.55, -0.45, 0.6)
                .cube(Material.GILDED_BLACKSTONE, -0.3, 0.45, 0.3, 0.5);
        rock.show(0);
        fx.sound(6, "minecraft:entity.blaze.shoot", 1.2f, 0.5f);
        fx.sound(10, "minecraft:item.firecharge.use", 1.2f, 0.5f);
        fx.anim(6, 24, 1, t -> {
            double e = t * t;
            double x = -9 + 9 * e, y = 12 - 11.6 * e, z = -4 + 4 * e;
            rock.at(x, y, z).rot(t * 400, t * 260, 0).apply(1);
            Location l = L(fx, x, y, z);
            fx.p(Particle.FLAME, l, 10, 0.45, 0.04);
            fx.p(Particle.LAVA, l, 2, 0.4, 0);
            fx.p(Particle.LARGE_SMOKE, l, 2, 0.4, 0.02);
            fx.p(Particle.CAMPFIRE_SIGNAL_SMOKE, l, 1, 0.3, 0.02);
        });
        fx.later(30, () -> {
            warn.remove();
            warn2.remove();
            d.remove();
            Location c = fx.base.clone().add(0, 0.5, 0);
            rock.at(0, 0.2, 0).rot(40, 30, 0).apply(1);
            fx.particle(Particle.FLASH, c.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0, Kit.col(0xFFB040));
            fx.p(Particle.EXPLOSION_EMITTER, c, 1, 0, 0);
            fx.p(Particle.EXPLOSION, c, 8, 1.6, 0);
            fx.p(Particle.LAVA, c, 40, 0.8, 0);
            fx.p(Particle.FLAME, c, 60, 0.8, 0.2);
            fx.p(Particle.LARGE_SMOKE, c, 14, 1.2, 0.06);
            Kit.burst(fx, c, 4.5, 40, O, 14);
            fx.sound("minecraft:entity.generic.explode", 1.8f, 0.5f);
            fx.sound("minecraft:entity.dragon_fireball.explode", 1.2f, 0.6f);
            fx.sound("minecraft:entity.lightning_bolt.thunder", 0.6f, 0.6f);
            Kit.wave(fx, 1.3, 4.5, 12, 5, 0.65);
            Kit.debris(fx, c, fx.groundBlock(), 18, 0.25, 0.6, 0.45, 36);
            Kit.debris(fx, c, Material.MAGMA_BLOCK, 6, 0.2, 0.55, 0.35, 34);
        });
        // crater: the rock glows and smokes, then cools down and sinks
        fx.later(34, () -> fx.repeat(12, 3, i -> {
            fx.p(Particle.CAMPFIRE_COSY_SMOKE, fx.base.clone().add(fx.rand(0.6), 0.8, fx.rand(0.6)), 1, 0.1, 0.03);
            fx.p(Particle.LAVA, fx.base.clone().add(0, 0.8, 0), 1, 0.4, 0);
        }));
        fx.later(74, () -> {
            rock.at(0, -1.6, 0).apply(14);
            fx.sound("minecraft:block.fire.extinguish", 1f, 0.6f);
        });
        fx.later(90, root::remove);
    }

    private static void dragonBreath(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Model root = Kit.root(fx);
        Model head = root.child(0, 6, -4.5);
        head.item(Material.DRAGON_HEAD, 0, 0, 0, 4.6);
        head.rot(0, 30, 0).size(0.001);
        head.show(0);
        fx.later(1, () -> {
            fx.p(Particle.REVERSE_PORTAL, L(fx, 0, 6, -4.5), 80, 1.2, 0.05);
            fx.sound("minecraft:entity.ender_dragon.growl", 1.2f, 0.9f);
        });
        fx.later(2, () -> head.at(0, 4.6, -3.6).size(1).apply(10));
        fx.later(14, () -> {
            fx.sound("minecraft:entity.ender_dragon.shoot", 1.2f, 0.7f);
            fx.sound("minecraft:entity.ender_dragon.ambient", 1f, 0.8f);
        });
        // breath stream
        fx.anim(16, 30, 1, t -> {
            head.at(fx.rand(0.04), 4.6 + Math.sin(t * 30) * 0.05, -3.6).apply(1);
            Location mouth = L(fx, 0, 3.9, -2.6);
            Location target = fx.base.clone().add(0, 0.9, 0);
            Vector v = target.toVector().subtract(mouth.toVector());
            for (int i = 0; i < 8; i++) {
                Vector dv = v.clone().add(new Vector(fx.rand(0.5), fx.rand(0.5), fx.rand(0.5))).multiply(0.11);
                fx.particle(Particle.DRAGON_BREATH, mouth, 0, dv.getX(), dv.getY(), dv.getZ(), 1.0, 1.0f);
            }
            Kit.streak(fx, mouth, target.clone().add(fx.rand(0.4), fx.rand(0.5), fx.rand(0.4)), t < 0.5 ? PU : P, 8);
            fx.particle(Particle.DRAGON_BREATH, target, 4, 0.5, 0.5, 0.5, 0.02, 1.0f);
        });
        // the body dissolves in purple
        fx.anim(26, 22, 1, t -> {
            d.scale(1 - 0.9 * t);
            Kit.dollAt(fx, d, 0, 0.6 * t, 0);
            fx.p(Particle.WITCH, d.loc().add(0, 0.8 * (1 - t), 0), 4, 0.3, 0.02);
            fx.particle(Particle.FALLING_DUST, d.loc().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0, Material.PURPLE_CONCRETE_POWDER.createBlockData());
        });
        fx.later(48, () -> {
            d.remove();
            fx.particle(Particle.DRAGON_BREATH, fx.base.clone().add(0, 0.6, 0), 60, 1.0, 1.0, 1.0, 0.03, 1.0f);
            fx.p(Particle.REVERSE_PORTAL, fx.base.clone().add(0, 1, 0), 40, 0.4, 0.08);
            fx.sound("minecraft:entity.ender_dragon.flap", 1.2f, 0.7f);
            head.at(0, 9, -6).size(0.001).apply(14);
        });
        fx.later(48, () -> fx.repeat(10, 4, i -> fx.particle(Particle.DRAGON_BREATH, fx.base.clone().add(fx.rand(1.2), 0.2, fx.rand(1.2)), 3, 0.2, 0.2, 0.2, 0.01, 1.0f)));
        fx.later(64, root::remove);
    }

    private static void ascension(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Location base = fx.base.clone();
        Kit.Beam beam = new Kit.Beam(fx, base, 0xFFF0A0);
        fx.later(2, () -> {
            beam.set(2.8, 1, 8);
            fx.sound("minecraft:block.beacon.activate", 1.4f, 1.2f);
            fx.sound("minecraft:block.amethyst_block.resonate", 1f, 1.4f);
        });
        Kit.Ring halo = new Kit.Ring(fx, base.clone().add(0, 2.2, 0), 0.42, 0.1, 16, 0xF0FCE300);
        halo.apply(0);
        fx.repeat(4, 6, i -> fx.sound("minecraft:block.amethyst_block.chime", 1.2f, 0.8f + i * 0.2f));
        // rises slowly, turning, with the halo
        fx.anim(8, 56, 1, t -> {
            double y = 4.5 * Kit.easeInOut(t);
            Kit.dollAt(fx, d, 0, y, 0);
            d.look((float) (-fx.face() + t * 360), -25f);
            halo.c.setY(base.getY() + y + 2.15);
            halo.spin += 0.12;
            halo.apply(1);
            Location b = d.loc().add(0, 1, 0);
            fx.p(Particle.END_ROD, b.clone().add(fx.rand(0.6), fx.rand(0.8), fx.rand(0.6)), 1, 0, 0.02);
            fx.p(Particle.TOTEM_OF_UNDYING, b, 2, 0.4, 0.08);
            fx.dust(b.clone().add(fx.rand(0.9), -0.6, fx.rand(0.9)), Y, 1.0f, 1, 0.05);
        });
        fx.repeat(12, 4, i -> fx.p(Particle.END_ROD, base.clone().add(fx.rand(1.0), 0.1, fx.rand(1.0)), 1, 0, 0.05));
        fx.later(64, () -> {
            Location top = d.loc().add(0, 1, 0);
            fx.particle(Particle.FLASH, top, 1, 0, 0, 0, 0, Kit.col(0xFFFFFF));
            fx.p(Particle.TOTEM_OF_UNDYING, top, 80, 0.6, 0.5);
            fx.p(Particle.END_ROD, top, 30, 0.3, 0.15);
            Kit.burst(fx, top, 3.5, 30, Y, 12);
            fx.sound("minecraft:item.totem.use", 0.9f, 1.3f);
            fx.sound("minecraft:block.beacon.deactivate", 1f, 1.4f);
            d.remove();
            halo.remove();
            beam.set(0.01, 0, 10);
        });
        fx.later(76, beam::remove);
    }

    private static void shattered(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        fx.sound(2, "minecraft:block.amethyst_block.resonate", 1.2f, 0.8f);
        fx.sound(2, "minecraft:block.glass.place", 1f, 0.6f);
        // a glass copy takes their place
        Rig glass = fx.rig(0, 0, 0);
        glass.show(0);
        fx.later(1, () -> glass.recolor(Material.LIGHT_BLUE_STAINED_GLASS));
        fx.later(3, () -> {
            d.remove();
            glass.recolor(Material.LIGHT_BLUE_STAINED_GLASS);
            headTo(glass, Material.LIGHT_BLUE_STAINED_GLASS);
            fx.p(Particle.END_ROD, fx.base.clone().add(0, 1, 0), 20, 0.4, 0.05);
        });
        // light glints over the glass
        fx.repeat(10, 3, i -> fx.p(Particle.WAX_OFF, fx.base.clone().add(fx.rand(0.35), 0.3 + fx.rnd.nextDouble() * 1.6, fx.rand(0.35)), 1, 0, 0));
        for (int k = 0; k < 3; k++) {
            int at = 16 + k * 6;
            float pitch = 1.2f + k * 0.25f;
            fx.later(at, () -> {
                fx.sound("minecraft:block.glass.hit", 1.2f, pitch);
                fx.sound("minecraft:block.amethyst_cluster.hit", 1f, pitch);
                glass.root.at(fx.rand(0.03), 0, fx.rand(0.03)).apply(1);
                fx.block(fx.base.clone().add(0, 1 + fx.rand(0.6), 0), Material.GLASS, 5, 0.3, 0.05);
            });
        }
        fx.later(36, () -> {
            Location c = fx.base.clone().add(0, 1, 0);
            fx.sound("minecraft:block.glass.break", 1.4f, 0.8f);
            fx.sound("minecraft:block.glass.break", 1.2f, 1.2f);
            fx.sound("minecraft:block.amethyst_cluster.break", 1.2f, 0.8f);
            fx.block(c, Material.GLASS, 80, 0.5, 0.3);
            fx.block(c, Material.LIGHT_BLUE_STAINED_GLASS, 30, 0.5, 0.3);
            fx.p(Particle.END_ROD, c, 25, 0.4, 0.12);
            Kit.burst(fx, c, 3, 30, 0xCFF4FF, 10);
            glass.root.shatter(0, 20, 2.4, 0.6);
            Kit.debris(fx, c, Material.GLASS, 8, 0.18, 0.4, 0.3, 26);
            Kit.debris(fx, c, Material.LIGHT_BLUE_STAINED_GLASS, 6, 0.18, 0.4, 0.28, 26);
        });
    }

    // ==================================================================== mythic

    private static void blackHole(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Location c = fx.base.clone().add(0, 2.4, 0);
        Kit.Disc aura = Kit.disc(fx, L(fx, 0, 2.4, -0.15), 0x00A303F9);
        Kit.Disc rim = Kit.disc(fx, L(fx, 0, 2.4, 0), 0x00FC00E3);
        Kit.Disc hole = Kit.disc(fx, L(fx, 0, 2.4, 0.12), 0x00000000);
        Kit.Ring disk1 = new Kit.Ring(fx, c, 0.1, 0.22, 26, 0xD0F97603);
        Kit.Ring disk2 = new Kit.Ring(fx, c, 0.1, 0.16, 22, 0xD0A303F9);
        Quaternionf tilt = new Quaternionf().rotateY((float) Math.toRadians(fx.face())).rotateX(0.38f).rotateZ(0.12f);
        disk1.tilt = tilt;
        disk2.tilt = new Quaternionf(tilt).rotateX(-0.12f);
        disk1.apply(0);
        disk2.apply(0);
        fx.sound(2, "minecraft:block.respawn_anchor.charge", 1.2f, 0.5f);
        fx.sound(2, "minecraft:entity.warden.sonic_charge", 1.2f, 0.6f);
        fx.sound(30, "minecraft:entity.warden.sonic_charge", 1.2f, 0.8f);
        fx.sound(10, "minecraft:block.portal.ambient", 1.2f, 0.6f);
        // the hole opens and spins
        fx.anim(2, 70, 1, t -> {
            double open = Math.min(1, t * 2.5);
            double s = (0.4 + 0.7 * open) * (t > 0.92 ? (1 - t) / 0.08 : 1);
            aura.set(3.4 * s, ((int) (0x55 * open)) << 24 | 0x7A20C0, 1);
            rim.set(1.75 * s, ((int) (0xB0 * open)) << 24 | 0xE070FF, 1);
            hole.set(1.45 * s, ((int) (0xF4 * open)) << 24 | 0x05000A, 1);
            disk1.r = 2.3 * s; disk1.spin += 0.22; disk1.apply(1);
            disk2.r = 1.6 * s; disk2.spin += 0.32; disk2.apply(1);
            for (int i = 0; i < 3; i++) {
                Vector v = new Vector(fx.rand(1), fx.rand(0.5), fx.rand(1)).normalize().multiply(4 + fx.rnd.nextDouble() * 2);
                Kit.streak(fx, c.clone().add(v), c, i == 0 ? PU : (i == 1 ? O : 0x6A2BFF), 12);
            }
            fx.p(Particle.REVERSE_PORTAL, c, 4, 1.5, 0.02);
        });
        // ground blocks are torn up and spiral in
        Material g = fx.groundBlock();
        for (int i = 0; i < 10; i++) {
            double a0 = fx.rnd.nextDouble() * Math.PI * 2, r0 = 2.2 + fx.rnd.nextDouble() * 2.3;
            int start = 8 + fx.rnd.nextInt(30);
            double sz = 0.35 + fx.rnd.nextDouble() * 0.3;
            Model m = fx.model(Math.cos(a0) * r0, -sz / 2, Math.sin(a0) * r0);
            m.cube(g, 0, 0, 0, sz);
            m.show(0);
            fx.later(start, () -> fx.block(fx.base.clone().add(Math.cos(a0) * r0, 0.2, Math.sin(a0) * r0), g, 8, 0.3, 0.1));
            fx.anim(start, 26, 1, t -> {
                double a = a0 + t * 5, r = r0 * (1 - Kit.easeIn(t));
                m.at(Math.cos(a) * r, -sz / 2 + (2.4 + sz / 2) * Kit.easeOut(t), Math.sin(a) * r).rot(t * 600, t * 400, 0).size(1 - 0.85 * t).apply(1);
            });
            fx.later(start + 27, m::remove);
        }
        // the body is pulled up, stretched and swallowed
        fx.anim(20, 40, 1, t -> {
            double e = Kit.easeIn(t);
            Kit.dollAt(fx, d, Math.sin(t * 12) * 0.4 * (1 - t), 1.6 * e, Math.cos(t * 12) * 0.4 * (1 - t));
            d.look((float) (-fx.face() + t * t * 1500), (float) (t * 60));
            d.scale(1 - 0.92 * e);
            fx.p(Particle.PORTAL, d.loc().add(0, 0.8, 0), 4, 0.3, 0.2);
        });
        fx.later(60, d::remove);
        fx.later(74, () -> {
            disk1.remove();
            disk2.remove();
            hole.remove();
            rim.remove();
            fx.particle(Particle.FLASH, c, 1, 0, 0, 0, 0, Kit.col(0xC060FF));
            fx.p(Particle.SONIC_BOOM, c, 1, 0, 0);
            fx.p(Particle.EXPLOSION_EMITTER, c, 1, 0, 0);
            fx.p(Particle.REVERSE_PORTAL, c, 150, 0.4, 1.0);
            fx.p(Particle.END_ROD, c, 40, 0.3, 0.3);
            Kit.burst(fx, c, 6, 50, PU, 14);
            Kit.burst(fx, c, 5, 30, O, 12);
            fx.sound("minecraft:entity.warden.sonic_boom", 1.6f, 0.7f);
            fx.sound("minecraft:entity.generic.explode", 1.4f, 0.5f);
            fx.sound("minecraft:block.end_portal.spawn", 0.8f, 0.8f);
            aura.set(12, 0x00A303F9, 10);
            Kit.Ring shock = new Kit.Ring(fx, fx.base.clone().add(0, 0.06, 0), 0.5, 0.35, 32, 0xE0A303F9);
            shock.apply(0);
            fx.later(1, () -> { shock.r = 7; shock.argb = 0x00A303F9; shock.apply(12); });
            fx.later(14, shock::remove);
            Kit.wave(fx, 1.0, 4.2, 10, 5, 0.5);
        });
        fx.later(86, aura::remove);
    }

    private static org.bukkit.entity.TextDisplay word(Fx fx, Location at, String text, String hex, float back) {
        org.bukkit.entity.TextDisplay t = fx.world.spawn(at, org.bukkit.entity.TextDisplay.class, d -> {
            d.text(net.kyori.adventure.text.Component.text(text).color(net.kyori.adventure.text.format.TextColor.fromHexString(hex))
                    .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
            d.setDefaultBackground(false);
            d.setBackgroundColor(org.bukkit.Color.fromARGB(0));
            d.setShadowed(false);
            d.setSeeThrough(false);
            fx.prepareDisplay(d);
            d.setBillboard(Display.Billboard.FIXED);
            d.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(0, 0, -back),
                    new Quaternionf().rotateY((float) Math.toRadians(fx.face())), new org.joml.Vector3f(0.001f, 0.001f, 0.001f), new Quaternionf()));
        });
        fx.trackEntity(t);
        return t;
    }

    /** Exclusive: red beam, the victim is lifted and judged, BANNED in 3D letters, the Banhammer slams down. */
    private static void banned(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Location base = fx.base.clone();
        fx.sound("minecraft:entity.elder_guardian.curse", 1f, 0.8f);
        fx.sound(1, "minecraft:block.beacon.activate", 1.4f, 0.5f);
        fx.sound(1, "minecraft:block.respawn_anchor.charge", 1.2f, 0.6f);
        // red light from the sky
        Kit.Beam beam = new Kit.Beam(fx, base, R);
        fx.later(2, () -> beam.set(2.6, 1, 8));
        // spinning red rings on the ground, closing in
        Kit.Ring outer = new Kit.Ring(fx, base.clone().add(0, 0.06, 0), 2.6, 0.14, 28, 0x00FC0000);
        Kit.Ring inner = new Kit.Ring(fx, base.clone().add(0, 0.07, 0), 1.6, 0.08, 20, 0x00FF5050);
        outer.apply(0); inner.apply(0);
        fx.anim(1, 52, 1, t -> {
            outer.r = 2.6 - 1.4 * t; outer.spin += 0.12; outer.argb = ((int) (0xD0 * Math.min(1, t * 6))) << 24 | R; outer.apply(1);
            inner.r = 1.6 - 0.9 * t; inner.spin -= 0.2; inner.argb = ((int) (0xB0 * Math.min(1, t * 6))) << 24 | 0xFF5050; inner.apply(1);
        });
        // lifted and held in the beam, shaking, energy pulled in
        fx.anim(4, 40, 1, t -> {
            double y = 2.2 * Kit.easeOut(t);
            Kit.dollAt(fx, d, fx.rand(0.04), y, fx.rand(0.04));
            d.look((float) -fx.face(), -35f);
            Location b = d.loc().add(0, 1, 0);
            fx.dust(b.clone().add(fx.rand(0.5), fx.rand(0.8), fx.rand(0.5)), R, 1.3f, 2, 0.05);
            fx.p(Particle.ELECTRIC_SPARK, b, 2, 0.4, 0.05);
        });
        fx.repeat(12, 4, i -> Kit.converge(fx, d.loc().add(0, 1, 0), 3.2, 3, i % 2 == 0 ? R : 0xFF6A00, 10));
        fx.repeat(5, 9, i -> fx.sound("minecraft:block.note_block.basedrum", 1.2f, 0.5f + i * 0.15f));
        fx.anim(44, 18, 1, t -> Kit.dollAt(fx, d, fx.rand(0.07), 2.2 + fx.rand(0.04), fx.rand(0.07)));
        // BANNED in 3D letters behind the victim
        String[] cols = {"#FF7A7A", "#FC0000", "#E00000", "#C40000", "#A00000", "#7C0000", "#5A0000"};
        Location at = L(fx, 0, 5.2, -2.8);
        at.setYaw(0);
        at.setPitch(0);
        List<org.bukkit.entity.TextDisplay> layers = new ArrayList<>();
        for (int i = 0; i < cols.length; i++) layers.add(word(fx, at, "BANNED", cols[i], i * 0.03f));
        java.util.function.BiConsumer<Double, Integer> pose = (sc, dur) -> {
            for (int i = 0; i < layers.size(); i++) {
                org.bukkit.entity.TextDisplay t = layers.get(i);
                if (!t.isValid()) continue;
                t.setInterpolationDelay(0);
                t.setInterpolationDuration(dur);
                float f = sc.floatValue();
                t.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(0, 0, -i * 0.03f * f),
                        new Quaternionf().rotateY((float) Math.toRadians(fx.face())), new org.joml.Vector3f(f, f, f), new Quaternionf()));
            }
        };
        fx.later(10, () -> {
            pose.accept(7.0, 6);
            beam.set(1.5, 0.8, 6);
            fx.sound("minecraft:entity.warden.sonic_charge", 1.2f, 0.8f);
            fx.sound("minecraft:block.anvil.land", 1f, 0.5f);
        });
        fx.later(16, () -> pose.accept(6.0, 4));
        // the Banhammer (giant mace) appears high above
        Model root = Kit.root(fx);
        Model mace = root.child(0, 13, 0);
        mace.item(Material.MACE, 0, 0, 0, 5.0);
        mace.rot(0, 0, -135).size(0.001);
        mace.show(0);
        fx.later(20, () -> {
            mace.at(0, 8.5, 0).size(1).rot(0, 0, -135).apply(14);
            mace.parts.forEach(p -> { p.d.setGlowing(true); p.d.setGlowColorOverride(Kit.col(R)); });
            fx.p(Particle.END_ROD, L(fx, 0, 8.5, 0), 30, 1.5, 0.05);
        });
        fx.later(56, () -> {
            fx.sound("minecraft:item.mace.smash_air", 1.4f, 0.5f);
            fx.sound("minecraft:entity.wither.shoot", 0.8f, 0.5f);
        });
        // SLAM
        fx.anim(58, 5, 1, t -> mace.at(0, 8.5 - 6.3 * t * t, 0).apply(1));
        fx.later(63, () -> {
            Location c = base.clone().add(0, 0.3, 0);
            List<Kit.Glow> b = bolt(fx, base, 24, 0xFF4A4A);
            fx.later(3, () -> b.forEach(g -> g.size(0.001, g.h).apply(3)));
            fx.later(7, () -> b.forEach(Kit.Glow::remove));
            d.remove();
            fx.particle(Particle.FLASH, c.clone().add(0, 1.5, 0), 1, 0, 0, 0, 0, Kit.col(0xFF4040));
            fx.p(Particle.EXPLOSION_EMITTER, c.clone().add(0, 1, 0), 1, 0, 0);
            fx.dust(c.clone().add(0, 1.5, 0), R, 2f, 90, 1.0);
            fx.p(Particle.LARGE_SMOKE, c.clone().add(0, 1, 0), 30, 0.7, 0.05);
            fx.particle(Particle.DUST_PILLAR, c, 100, 2.0, 0.1, 2.0, 0, fx.groundBlock().createBlockData());
            fx.block(c, fx.groundBlock(), 70, 1.4, 0.4);
            Kit.burst(fx, c.clone().add(0, 1, 0), 6, 40, R, 14);
            Kit.wave(fx, 1.2, 4.8, 14, 6, 0.75);
            Kit.debris(fx, c, fx.groundBlock(), 16, 0.25, 0.7, 0.45, 36);
            fx.sound("minecraft:entity.lightning_bolt.thunder", 1.4f, 0.7f);
            fx.sound("minecraft:item.mace.smash_ground_heavy", 2f, 0.6f);
            fx.sound("minecraft:entity.generic.explode", 1.5f, 0.6f);
            outer.remove(); inner.remove();
            beam.set(6, 0, 10);
            pose.accept(7.2, 2);
            Kit.Ring shock = new Kit.Ring(fx, base.clone().add(0, 0.07, 0), 0.6, 0.3, 32, 0xE0FC0000);
            shock.apply(0);
            fx.later(1, () -> { shock.r = 8; shock.argb = 0x00FC0000; shock.apply(14); });
            fx.later(16, shock::remove);
        });
        fx.later(66, () -> mace.at(0, 2.6, 0).size(0.001).apply(10));
        fx.later(76, () -> pose.accept(0.001, 8));
        fx.later(86, () -> { layers.forEach(t -> { if (t.isValid()) t.remove(); }); mace.remove(); beam.remove(); root.remove(); });
    }

    private static void judgement(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Location base = fx.base.clone();
        // light from above
        Kit.Beam beam = new Kit.Beam(fx, base, 0xFCE300);
        fx.later(1, () -> beam.set(2.4, 1, 10));
        fx.sound(2, "minecraft:block.beacon.activate", 1.2f, 0.6f);
        fx.sound(4, "minecraft:entity.elder_guardian.curse", 0.6f, 0.6f);
        // the giant mace appears high above, head down
        Model root = Kit.root(fx);
        Model mace = root.child(0, 12, 0);
        mace.item(Material.MACE, 0, 0, 0, 5.0);
        mace.rot(0, 0, -135).size(0.001);
        mace.show(0);
        fx.later(3, () -> {
            mace.at(0, 6.2, 0).size(1).rot(0, 0, -135).apply(14);
            mace.parts.forEach(p -> { p.d.setGlowing(true); p.d.setGlowColorOverride(Kit.col(Y)); });
            fx.p(Particle.END_ROD, L(fx, 0, 6.2, 0), 40, 1.5, 0.05);
        });
        fx.repeat(8, 3, i -> fx.p(Particle.ELECTRIC_SPARK, L(fx, 0, 5.5, 0), 6, 0.8, 0.1));
        // the doll looks up, trembling
        fx.later(4, () -> d.look((float) -fx.face(), -60f));
        fx.repeat(14, 2, i -> Kit.dollAt(fx, d, fx.rand(0.03), 0, fx.rand(0.03)));
        fx.later(24, () -> {
            fx.sound("minecraft:item.mace.smash_air", 1.4f, 0.5f);
            fx.sound("minecraft:entity.wither.shoot", 0.8f, 0.5f);
        });
        // SLAM
        fx.anim(26, 4, 1, t -> mace.at(0, 6.2 - 4.0 * t * t, 0).apply(1));
        fx.later(30, () -> {
            d.remove();
            Location c = base.clone().add(0, 0.3, 0);
            fx.particle(Particle.FLASH, c.clone().add(0, 1, 0), 1, 0, 0, 0, 0, Kit.col(0xFFF4B0));
            fx.p(Particle.EXPLOSION_EMITTER, c, 1, 0, 0);
            fx.particle(Particle.DUST_PILLAR, c, 120, 2.0, 0.1, 2.0, 0, fx.groundBlock().createBlockData());
            fx.block(c, fx.groundBlock(), 90, 1.6, 0.4);
            fx.p(Particle.CLOUD, c, 40, 2.0, 0.08);
            Kit.burst(fx, c.clone().add(0, 0.5, 0), 6, 40, Y, 14);
            fx.sound("minecraft:item.mace.smash_ground_heavy", 2f, 0.6f);
            fx.sound("minecraft:item.mace.smash_ground_heavy", 2f, 0.8f);
            fx.sound("minecraft:entity.generic.explode", 1.5f, 0.6f);
            fx.sound("minecraft:entity.lightning_bolt.thunder", 1f, 0.7f);
            Kit.wave(fx, 1.2, 4.8, 14, 6, 0.75);
            Kit.debris(fx, c, fx.groundBlock(), 20, 0.25, 0.7, 0.45, 38);
            beam.set(6, 0, 10);
            Kit.Ring shock = new Kit.Ring(fx, base.clone().add(0, 0.07, 0), 0.6, 0.3, 32, 0xE0FCE300);
            shock.apply(0);
            fx.later(1, () -> { shock.r = 8; shock.argb = 0x00FCE300; shock.apply(14); });
            fx.later(16, shock::remove);
        });
        fx.later(31, () -> mace.at(0, 2.0, 0).apply(2));
        fx.later(33, () -> mace.at(0, 2.2, 0).apply(3));
        fx.later(32, () -> fx.repeat(10, 3, i -> fx.p(Particle.ELECTRIC_SPARK, base.clone().add(fx.rand(1), 0.3, fx.rand(1)), 3, 0.3, 0.1)));
        // rises back into the sky
        fx.later(64, () -> {
            mace.at(0, 18, 0).size(0.001).apply(18);
            fx.sound("minecraft:block.beacon.deactivate", 1.2f, 0.6f);
        });
        fx.later(84, () -> { root.remove(); beam.remove(); });
    }

    private static void chickenGod(Fx fx) {
        Doll d = Kit.doll(fx);
        hurt(fx);
        Location base = fx.base.clone();
        Kit.Beam beam = new Kit.Beam(fx, base, 0xFFD84A);
        fx.later(1, () -> beam.set(4.0, 1, 10));
        Location start = L(fx, 0, 16, -0.4);
        start.setDirection(Kit.dir(fx, 0, 0, 1));
        Chicken ch = Kit.mob(fx, Chicken.class, start, 5.0);
        ch.setGlowing(true);
        fx.sound(1, "minecraft:block.beacon.activate", 1.2f, 1.6f);
        fx.sound(3, "minecraft:entity.chicken.ambient", 2f, 0.5f);
        fx.sound(14, "minecraft:entity.chicken.ambient", 2f, 0.6f);
        // descends from the sky, feathers falling
        fx.anim(2, 28, 1, t -> {
            Location l = L(fx, 0, 16 - 16 * Kit.easeOut(t), -0.4);
            l.setDirection(Kit.dir(fx, 0, 0, 1));
            Kit.move(ch, l);
            fx.item(l.clone().add(fx.rand(1.6), 1.5 + fx.rand(1), fx.rand(1.6)), Material.FEATHER, 2, 0.3, 0.02);
            fx.p(Particle.END_ROD, l.clone().add(0, 1, 0), 2, 1.5, 0.02);
        });
        fx.later(30, () -> {
            d.remove();
            fx.sound("minecraft:entity.player.big_fall", 1.4f, 0.5f);
            fx.sound("minecraft:entity.chicken.hurt", 2f, 0.5f);
            fx.sound("minecraft:item.mace.smash_ground", 1.2f, 0.7f);
            fx.block(base.clone().add(0, 0.1, 0), fx.groundBlock(), 60, 1.6, 0.3);
            fx.item(base.clone().add(0, 1.5, 0), Material.FEATHER, 30, 1.4, 0.1);
            Kit.wave(fx, 1.5, 4.5, 8, 4, 0.5);
        });
        // sits, wiggles, lays a golden egg
        fx.repeat(6, 3, i -> {
            Location l = L(fx, (i % 2 == 0 ? 0.1 : -0.1), 0, -0.4);
            l.setDirection(Kit.dir(fx, 0, 0, 1));
            Kit.move(ch, l);
        });
        fx.later(36, () -> fx.sound("minecraft:entity.chicken.egg", 2f, 0.5f));
        Model root = Kit.root(fx);
        Model egg = root.child(0, 0.7, 1.4);
        egg.item(Material.EGG, 0, 0, 0, 1.6).size(0.001);
        egg.show(0);
        fx.later(40, () -> {
            egg.parts.forEach(p -> { p.d.setGlowing(true); p.d.setGlowColorOverride(Kit.col(Y)); p.d.setBillboard(Display.Billboard.VERTICAL); });
            egg.at(0, 0.8, 2.0).size(1).apply(4);
            fx.sound("minecraft:entity.item.pickup", 1.4f, 0.5f);
            fx.p(Particle.WAX_ON, L(fx, 0, 0.9, 2.0), 15, 0.4, 0.1);
        });
        fx.later(44, () -> egg.at(0, 0.8, 2.0).size(1.25).apply(3));
        fx.later(47, () -> egg.at(0, 0.8, 2.0).size(1).apply(3));
        // flies off into the light
        fx.sound(50, "minecraft:entity.chicken.ambient", 2f, 0.7f);
        fx.sound(52, "minecraft:entity.ender_dragon.flap", 1f, 1.6f);
        fx.anim(52, 30, 1, t -> {
            Location l = L(fx, 0, 20 * Kit.easeIn(t), -0.4 - 2 * t);
            l.setDirection(Kit.dir(fx, 0, 0, 1));
            Kit.move(ch, l);
            if ((int) (t * 30) % 3 == 0) fx.item(l.clone().add(0, 1.5, 0), Material.FEATHER, 3, 0.8, 0.03);
        });
        // the egg hatches into fireworks
        fx.later(66, () -> {
            Location e = L(fx, 0, 1.0, 2.0);
            egg.remove();
            Kit.firework(fx, e.clone().add(0, 0.5, 0), FireworkEffect.Type.BALL_LARGE, true, true, Y, 0xFFD84A, O);
            fx.sound("minecraft:entity.firework_rocket.large_blast", 1.2f, 1f);
            fx.sound("minecraft:entity.chicken.egg", 1.4f, 1.2f);
            fx.item(e, Material.EGG, 20, 0.3, 0.15);
            fx.p(Particle.TOTEM_OF_UNDYING, e, 60, 0.4, 0.4);
            Kit.confetti(fx, e.clone().add(0, 1.5, 0), 30, 1.2);
        });
        fx.later(82, () -> {
            if (ch.isValid()) ch.remove();
            beam.set(0.01, 0, 8);
        });
        fx.later(92, () -> { beam.remove(); root.remove(); });
    }
}
