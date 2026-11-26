package dev.chickensmpac.module;

import dev.chickensmpac.ChickenSMPAC;
import dev.chickensmpac.Messages;
import dev.chickensmpac.util.Text;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /announce: title + chat block for everyone. {head} = skin head of the sender (chat object
 * component, 1.21.9+), {rank} = the sender's LuckPerms prefix (via PlaceholderAPI).
 * Patched 2026-11-26 (ChickenSMP owner: head + name with rank instead of "ANNOUNCEMENT").
 */
public final class AnnounceModule extends StaffModule {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    public AnnounceModule(ChickenSMPAC chickenSMPAC) {
        super(chickenSMPAC, "announce");
        this.command("announce", "chickensmpac.announce", this::announce, null);
        this.command("broadcast", "chickensmpac.broadcast", this::broadcast, null);
    }

    private static String rank(CommandSender sender) {
        if (!(sender instanceof Player p)) return "";
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Object r = papi.getMethod("setPlaceholders", OfflinePlayer.class, String.class).invoke(null, p, "%luckperms_prefix%");
            String s = r == null ? "" : r.toString();
            return s.contains("%") ? "" : s;
        } catch (Throwable t) {
            return "";
        }
    }

    private static Component head(CommandSender sender) {
        if (!(sender instanceof Player p)) return Component.empty();
        try {
            PlayerHeadObjectContents.Builder b = ObjectContents.playerHead().name(p.getName()).id(p.getUniqueId());
            for (var prop : p.getPlayerProfile().getProperties()) {
                if (!"textures".equals(prop.getName())) continue;
                b.profileProperty(prop.getSignature() == null
                        ? PlayerHeadObjectContents.property(prop.getName(), prop.getValue())
                        : PlayerHeadObjectContents.property(prop.getName(), prop.getValue(), prop.getSignature()));
            }
            return Component.object(b.build());
        } catch (Throwable t) {
            return Component.empty();
        }
    }

    /** Legacy line with {head} markers -> component. */
    private static Component line(String legacy, Component head) {
        String[] parts = legacy.split("\\{head\\}", -1);
        TextComponent.Builder out = Component.text();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) out.append(head);
            if (!parts[i].isEmpty()) out.append(LEGACY.deserialize(parts[i]));
        }
        return out.build();
    }

    private void announce(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            this.msg.send(sender, "usage.announce", new Object[0]);
            return;
        }
        String message = Text.color(String.join(" ", args));
        Map<String, String> map = Messages.map("message", message, "player", sender instanceof Player ? sender.getName() : "Console", "rank", rank(sender));
        Component head = head(sender);
        Component title = line(this.msg.get("announce.title", map), head);
        Component subtitle = line(this.msg.get("announce.subtitle", map), head);
        List<Component> chat = new ArrayList<>();
        List<String> plain = this.msg.list("announce.chat", map);
        for (String s : plain) chat.add(line(s, head));
        Title t = Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(t);
            for (Component c : chat) p.sendMessage(c);
            this.plugin.sounds().play((CommandSender) p, "announce.sound");
        }
        for (String s : plain) Bukkit.getConsoleSender().sendMessage(s.replace("{head}", ""));
    }

    private void broadcast(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            this.msg.send(sender, "usage.broadcast", new Object[0]);
            return;
        }
        String s = this.msg.get("announce.broadcast", "message", Text.color(String.join(" ", args)), "player", sender.getName());
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(s);
        Bukkit.getConsoleSender().sendMessage(s);
    }
}
