package net.chickensmp.acplus;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Players with chickensmp.acplus.chat.spaced (owner group) get an empty line above and below
 * their chat messages. Wraps whatever renderer the chat plugin (EconomyCore) set.
 */
final class ChatSpacing implements Listener {
    static final String PERM = "chickensmp.acplus.chat.spaced";

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (!e.getPlayer().hasPermission(PERM)) return;
        ChatRenderer old = e.renderer();
        e.renderer((source, displayName, message, viewer) -> Component.text()
                .append(Component.newline())
                .append(old.render(source, displayName, message, viewer))
                .append(Component.newline())
                .build());
    }
}
