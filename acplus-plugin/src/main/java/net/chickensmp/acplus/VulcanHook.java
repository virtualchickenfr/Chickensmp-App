package net.chickensmp.acplus;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Listens to Vulcan's public API event (me.frep.vulcan.api.event.VulcanFlagEvent) by reflection,
 * so this plugin needs no Vulcan jar at compile time and still loads when Vulcan is missing.
 */
final class VulcanHook implements Listener {

    private final SusPlugin plugin;
    private boolean hooked;
    private boolean grimHooked;

    VulcanHook(SusPlugin plugin) {
        this.plugin = plugin;
    }

    boolean isHooked() {
        return hooked || grimHooked;
    }

    /** Grim fires ac.grim.grimac.api.events.FlagEvent as a Bukkit event (bridged from its own event bus). */
    @SuppressWarnings("unchecked")
    boolean tryHookGrim() {
        if (grimHooked) return true;
        Plugin grim = Bukkit.getPluginManager().getPlugin("GrimAC");
        if (grim == null || !grim.isEnabled()) return false;
        try {
            ClassLoader cl = grim.getClass().getClassLoader();
            Class<? extends Event> flagClass =
                    (Class<? extends Event>) Class.forName("ac.grim.grimac.api.events.FlagEvent", true, cl);
            Class<?> checkClass = Class.forName("ac.grim.grimac.api.AbstractCheck", true, cl);
            Class<?> identClass = Class.forName("ac.grim.grimac.api.GrimIdentity", true, cl);
            Class<?> userClass = Class.forName("ac.grim.grimac.api.GrimUser", true, cl);
            Method getUser = flagClass.getMethod("getUser");
            Method getCheck = flagClass.getMethod("getCheck");
            Method getVerbose = flagClass.getMethod("getVerbose");
            Method getVl = flagClass.getMethod("getViolations");
            Method uId = identClass.getMethod("getUniqueId");
            Method uName = userClass.getMethod("getName");
            Method cName = checkClass.getMethod("getCheckName");
            Method cExperimental = checkClass.getMethod("isExperimental");

            Bukkit.getPluginManager().registerEvent(flagClass, this, EventPriority.MONITOR, (listener, event) -> {
                if (!flagClass.isInstance(event)) return;
                if (event instanceof Cancellable c && c.isCancelled()) return;
                try {
                    Object user = getUser.invoke(event);
                    Object check = getCheck.invoke(event);
                    if (user == null || check == null) return;
                    java.util.UUID id = (java.util.UUID) uId.invoke(user);
                    String name = String.valueOf(uName.invoke(user));
                    String checkName = String.valueOf(cName.invoke(check));
                    boolean experimental = (boolean) cExperimental.invoke(check);
                    int vl = (int) Math.round((double) getVl.invoke(event));
                    String info = String.valueOf(getVerbose.invoke(event));
                    // Grim names look like "Reach", "Simulation", "AimModulo360", "BadPacketsA" ...
                    char type = 'A';
                    String base = checkName;
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.*?)([A-Z])$").matcher(checkName);
                    if (m.matches() && m.group(1).length() > 2 && Character.isLowerCase(m.group(1).charAt(m.group(1).length() - 1))) {
                        base = m.group(1); type = m.group(2).charAt(0);
                    }
                    plugin.recordFlag(id, name, base, type, grimCategory(checkName), experimental, vl, info);
                } catch (ReflectiveOperationException | ClassCastException ex) {
                    plugin.getLogger().warning("Could not read a Grim flag: " + ex);
                }
            }, plugin, true);
            grimHooked = true;
            plugin.getLogger().info("Hooked into GrimAC " + grim.getPluginMeta().getVersion() + " - recording flags.");
            return true;
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("GrimAC found, but its API could not be loaded: " + ex);
            return false;
        }
    }

    static String grimCategory(String check) {
        String c = check.toLowerCase(java.util.Locale.ROOT);
        if (c.matches(".*(reach|hitbox|aim|killaura|autoclicker|clicker|multiinteract|velocity|antikb|crash|fastbreak|nobreak|wallhit).*")) return "combat";
        if (c.matches(".*(simulation|phase|nofall|groundspoof|timer|speed|fly|elytra|vehicle|negativetimer|entitycontrol|noslow|sprint|jesus).*")) return "movement";
        return "player";
    }

    @SuppressWarnings("unchecked")
    boolean tryHook() {
        if (hooked) return true;
        Plugin vulcan = Bukkit.getPluginManager().getPlugin("Vulcan");
        if (vulcan == null || !vulcan.isEnabled()) return false;
        try {
            ClassLoader cl = vulcan.getClass().getClassLoader();
            Class<? extends Event> flagClass =
                    (Class<? extends Event>) Class.forName("me.frep.vulcan.api.event.VulcanFlagEvent", true, cl);
            Class<?> checkClass = Class.forName("me.frep.vulcan.api.check.Check", true, cl);

            Method getPlayer = flagClass.getMethod("getPlayer");
            Method getCheck = flagClass.getMethod("getCheck");
            Method getInfo = flagClass.getMethod("getInfo");
            Method cName = checkClass.getMethod("getDisplayName");
            Method cType = checkClass.getMethod("getDisplayType");
            Method cCategory = checkClass.getMethod("getCategory");
            Method cVl = checkClass.getMethod("getVl");
            Method cExperimental = checkClass.getMethod("isExperimental");

            Bukkit.getPluginManager().registerEvent(flagClass, this, EventPriority.MONITOR, (listener, event) -> {
                if (!flagClass.isInstance(event)) return;
                if (event instanceof Cancellable c && c.isCancelled()) return;
                try {
                    Player p = (Player) getPlayer.invoke(event);
                    Object check = getCheck.invoke(event);
                    if (p == null || check == null) return;
                    String name = String.valueOf(cName.invoke(check));
                    char type = (char) cType.invoke(check);
                    String category = String.valueOf(cCategory.invoke(check));
                    int vl = (int) cVl.invoke(check);
                    boolean experimental = (boolean) cExperimental.invoke(check);
                    String info = String.valueOf(getInfo.invoke(event));
                    plugin.recordFlag(p.getUniqueId(), p.getName(), name, Character.toUpperCase(type),
                            category, experimental, vl, info);
                } catch (ReflectiveOperationException | ClassCastException ex) {
                    plugin.getLogger().warning("Could not read a Vulcan flag: " + ex);
                }
            }, plugin, true);
            hooked = true;
            plugin.getLogger().info("Hooked into Vulcan " + vulcan.getPluginMeta().getVersion() + " - recording flags.");
            return true;
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("Vulcan found, but its API could not be loaded: " + ex);
            return false;
        }
    }
}
