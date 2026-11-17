package net.chickensmp.sign;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

/** Only loaded when LuckPerms is installed. */
final class LpHook {
    private LpHook() { }

    static String topGroup(Player p) {
        User u = LuckPermsProvider.get().getUserManager().getUser(p.getUniqueId());
        if (u == null) return null;
        Group best = null;
        for (Group g : u.getInheritedGroups(u.getQueryOptions())) {
            if (best == null || g.getWeight().orElse(0) > best.getWeight().orElse(0)) best = g;
        }
        return best == null ? u.getPrimaryGroup() : best.getName();
    }
}
