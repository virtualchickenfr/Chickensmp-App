package dev.killeffects;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;

/**
 * An exact copy of the victim: a vanilla mannequin wearing their real skin (1:1).
 * It can move, turn, lie down, crouch, swim-pose, ride and change size – but not move single limbs.
 * Coordinates are relative to the effect location.
 */
final class Doll {
    final Fx fx;
    final Mannequin e;
    double x, y, z;
    float yaw, pitch;

    Doll(Fx fx, double x, double y, double z) {
        this.fx = fx;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = (float) -fx.face();
        Location l = fx.base.clone().add(x, y, z);
        l.setYaw(yaw);
        l.setPitch(0f);
        Player who = fx.victim != null ? fx.victim : fx.viewer;
        this.e = fx.world.spawn(l, Mannequin.class, m -> {
            m.setPersistent(false);
            m.setInvulnerable(true);
            m.setSilent(true);
            m.setGravity(false);
            m.setCollidable(false);
            m.setImmovable(true);
            m.setCustomNameVisible(false);
            try {
                m.setDescription(null);
            } catch (Exception ignored) {
            }
            if (who != null) {
                try {
                    m.setProfile(ResolvableProfile.resolvableProfile(who.getPlayerProfile()));
                } catch (Exception ignored) {
                }
            }
            m.setBodyYaw(yaw);
            if (fx.viewer != null) m.setVisibleByDefault(false);
        });
        fx.trackEntity(e);
    }

    Location loc() {
        return fx.base.clone().add(x, y, z);
    }

    /** Centre of the head (for overlays and particles). */
    Location headLoc() {
        return loc().add(0, 1.6 * scaleNow(), 0);
    }

    double scaleNow() {
        AttributeInstance a = e.getAttribute(Attribute.SCALE);
        return a == null ? 1.0 : a.getBaseValue();
    }

    Doll at(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return push();
    }

    /** Minecraft yaw / pitch in degrees. */
    Doll look(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        return push();
    }

    private Doll push() {
        if (!e.isValid()) return this;
        Location l = loc();
        l.setYaw(yaw);
        l.setPitch(pitch);
        e.teleport(l, TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
        e.setRotation(yaw, pitch);
        e.setBodyYaw(yaw);
        return this;
    }

    Doll pose(Pose p) {
        try {
            e.setPose(p, true);
        } catch (Exception ex) {
            fx.plugin.getLogger().fine("Pose " + p + " not allowed for mannequins");
        }
        return this;
    }

    Doll scale(double s) {
        AttributeInstance a = e.getAttribute(Attribute.SCALE);
        if (a != null) a.setBaseValue(Math.max(0.0625, Math.min(16, s)));
        return this;
    }

    void ride(Entity vehicle) {
        vehicle.addPassenger(e);
    }

    void remove() {
        if (e.isValid()) e.remove();
    }
}
