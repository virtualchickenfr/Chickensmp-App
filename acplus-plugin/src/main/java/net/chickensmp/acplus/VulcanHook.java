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

    VulcanHook(SusPlugin plugin) {
        this.plugin = plugin;
    }

    boolean isHooked() {
        return hooked;
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
