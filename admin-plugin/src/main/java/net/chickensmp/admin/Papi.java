package net.chickensmp.admin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/** PlaceholderAPI without a compile dependency. */
final class Papi {
    private Papi() {}
    private static Method set;
    private static boolean tried;

    static String apply(Player p, String text) {
        if (!tried) {
            tried = true;
            try {
                if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
                    set = Class.forName("me.clip.placeholderapi.PlaceholderAPI").getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
            } catch (Exception ignored) {}
        }
        if (set == null) return text;
        try { return (String) set.invoke(null, p, text); } catch (Exception e) { return text; }
    }
}
