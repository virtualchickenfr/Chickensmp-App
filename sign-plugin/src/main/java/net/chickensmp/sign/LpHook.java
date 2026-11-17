package net.chickensmp.sign;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.entity.Player;

import java.util.Comparator;

/** Only loaded when LuckPerms is installed. */
final class LpHook {
    private LpHook() { }

    private static Group top(Player p) {
        User u = LuckPermsProvider.get().getUserManager().getUser(p.getUniqueId());
        if (u == null) return null;
        Group best = null;
        for (Group g : u.getInheritedGroups(u.getQueryOptions())) {
            if (best == null || g.getWeight().orElse(0) > best.getWeight().orElse(0)) best = g;
        }
        return best;
    }

    static String topGroup(Player p) {
        Group g = top(p);
        if (g != null) return g.getName();
        User u = LuckPermsProvider.get().getUserManager().getUser(p.getUniqueId());
        return u == null ? null : u.getPrimaryGroup();
    }

    /**
     * The prefix of the top group with legacy colour codes (&, §, &#rrggbb).
     * Skips prefixes with placeholders (TAB animations) and colour-only prefixes,
     * so e.g. the owner gets his static "OWNER" prefix.
     */
    static String debug(Player p) {
        Group g = top(p);
        if (g == null) return "no group";
        StringBuilder sb = new StringBuilder(g.getName() + " all=" + g.getNodes().size() + " prefix=[");
        for (PrefixNode n : g.getNodes(NodeType.PREFIX)) sb.append(n.getPriority()).append(':').append(n.getMetaValue()).append(" v=").append(n.getValue()).append(", ");
        sb.append("] keys=");
        g.getNodes().forEach(n -> sb.append(n.getKey()).append(" | "));
        return sb.toString();
    }

    static String rankPrefix(Player p) {
        Group g = top(p);
        if (g == null) return null;
        return g.getNodes(NodeType.PREFIX).stream()
                .filter(n -> n.getValue() && !n.hasExpired())
                .filter(n -> !n.getMetaValue().contains("%"))
                .filter(n -> !visible(n.getMetaValue()).isBlank())
                .max(Comparator.comparingInt(PrefixNode::getPriority))
                .map(PrefixNode::getMetaValue)
                .orElse(null);
    }

    static String visible(String legacy) {
        return legacy.replaceAll("[&\u00a7]#[0-9a-fA-F]{6}", "").replaceAll("[&\u00a7][0-9a-fk-orxA-FK-ORX]", "");
    }
}
