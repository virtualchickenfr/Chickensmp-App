package net.chickensmp.acplus;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.World;

/** Only loaded when WorldGuard is installed. */
final class WgHook {
    private WgHook() {}

    /** null = region not found, otherwise whether the block is inside it. */
    static Boolean inRegion(World world, String regionId, int x, int y, int z) {
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (rm == null) return null;
        ProtectedRegion r = rm.getRegion(regionId);
        if (r == null) return null;
        return r.contains(x, y, z);
    }
}
