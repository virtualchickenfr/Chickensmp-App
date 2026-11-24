/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.block.SignChangeEvent
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 */
package dev.chickensmpac.module;

import dev.chickensmpac.ChickenSMPAC;
import dev.chickensmpac.model.PunishmentType;
import dev.chickensmpac.module.StaffModule;
import dev.chickensmpac.service.PunishmentService;
import dev.chickensmpac.util.Durations;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ChatModule
extends StaffModule {
    private volatile boolean paused;
    private volatile long pausedUntil;
    private final Map<UUID, LastMessage> last = new ConcurrentHashMap<UUID, LastMessage>();
    private volatile Set<String> blocked = Set.of();
    private volatile Set<String> whitelist = Set.of();

    public ChatModule(ChickenSMPAC chickenSMPAC) {
        super(chickenSMPAC, "chat");
        this.command("chat", null, this::execute, (commandSender, stringArray) -> stringArray.length == 1 ? ChatModule.filter(List.of("clear", "pause", "unpause"), stringArray[0]) : (stringArray.length == 2 && stringArray[0].equalsIgnoreCase("pause") ? ChatModule.filter(List.of("5m", "15m", "1h"), stringArray[1]) : List.of()));
    }

    @Override
    public void start() {
        this.reload();
    }

    public void reload() {
        HashSet<String> hashSet = new HashSet<String>();
        for (Object object : this.plugin.getConfig().getStringList("chat-filter.swear.words")) {
            hashSet.add(ChatModule.normalize((String)object));
        }
        hashSet.remove("");
        HashSet hashSet2 = new HashSet();
        for (String string : this.plugin.getConfig().getStringList("chat-filter.swear.whitelist")) {
            hashSet2.add(ChatModule.normalize(string));
        }
        this.blocked = hashSet;
        this.whitelist = hashSet2;
    }

    private void execute(CommandSender commandSender, String string, String[] stringArray) {
        if (stringArray.length == 0) {
            this.msg.send(commandSender, "usage.chat", new Object[0]);
            return;
        }
        switch (stringArray[0].toLowerCase(Locale.ROOT)) {
            case "clear": {
                if (!commandSender.hasPermission("chickensmpac.chat.clear")) {
                    this.msg.send(commandSender, "error.no-permission", new Object[0]);
                    return;
                }
                int n = this.plugin.getConfig().getInt("chat.clear-lines", 150);
                // ChickenSMP fix: one blank message per line (works on Java + Bedrock), for everyone
                for (Player player : Bukkit.getOnlinePlayers()) {
                    for (int i = 0; i < n; ++i) {
                        player.sendMessage(" ");
                    }
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    this.msg.send((CommandSender)player, "chat.cleared", "player", commandSender.getName());
                }
                break;
            }
            case "pause": {
                long l;
                if (!commandSender.hasPermission("chickensmpac.chat.pause")) {
                    this.msg.send(commandSender, "error.no-permission", new Object[0]);
                    return;
                }
                long l2 = l = stringArray.length > 1 ? Durations.parse(stringArray[1]) : -1L;
                if (l == -2L) {
                    this.msg.send(commandSender, "error.invalid-duration", "input", stringArray[1]);
                    return;
                }
                this.paused = true;
                this.pausedUntil = l < 0L ? -1L : System.currentTimeMillis() + l;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (l < 0L) {
                        this.msg.send((CommandSender)player, "chat.paused", "player", commandSender.getName());
                        continue;
                    }
                    this.msg.send((CommandSender)player, "chat.paused-timed", "player", commandSender.getName(), "duration", Durations.format(l));
                }
                break;
            }
            case "unpause": 
            case "resume": {
                if (!commandSender.hasPermission("chickensmpac.chat.pause")) {
                    this.msg.send(commandSender, "error.no-permission", new Object[0]);
                    return;
                }
                this.paused = false;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    this.msg.send((CommandSender)player, "chat.unpaused", "player", commandSender.getName());
                }
                break;
            }
            default: {
                this.msg.send(commandSender, "usage.chat", new Object[0]);
            }
        }
    }

    private boolean isPaused() {
        if (this.paused && this.pausedUntil > 0L && System.currentTimeMillis() > this.pausedUntil) {
            this.paused = false;
            this.plugin.sync(() -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    this.msg.send((CommandSender)player, "chat.unpaused", "player", "ChickenSMPAC");
                }
            });
        }
        return this.paused;
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onChat(AsyncPlayerChatEvent asyncPlayerChatEvent) {
        if (!this.enabled()) {
            return;
        }
        Player player = asyncPlayerChatEvent.getPlayer();
        if (this.isPaused() && !player.hasPermission("chickensmpac.chat.bypass")) {
            asyncPlayerChatEvent.setCancelled(true);
            this.msg.send((CommandSender)player, "chat.is-paused", new Object[0]);
            return;
        }
        if (!this.plugin.getConfig().getBoolean("features.chat-filter", true) || player.hasPermission("chickensmpac.filter.bypass")) {
            return;
        }
        String string = asyncPlayerChatEvent.getMessage();
        long l = System.currentTimeMillis();
        long l2 = this.plugin.getConfig().getLong("chat-filter.cooldown.millis", 1500L);
        LastMessage lastMessage = this.last.get(player.getUniqueId());
        if (this.plugin.getConfig().getBoolean("chat-filter.cooldown.enabled", true) && lastMessage != null && l - lastMessage.time() < l2) {
            asyncPlayerChatEvent.setCancelled(true);
            this.msg.send((CommandSender)player, "filter.cooldown", "seconds", String.format(Locale.ROOT, "%.1f", (double)(l2 - (l - lastMessage.time())) / 1000.0));
            return;
        }
        String string2 = ChatModule.normalize(string);
        if (this.plugin.getConfig().getBoolean("chat-filter.anti-repeat.enabled", true) && lastMessage != null && lastMessage.text().equals(string2) && l - lastMessage.time() < this.plugin.getConfig().getLong("chat-filter.anti-repeat.window-seconds", 30L) * 1000L) {
            asyncPlayerChatEvent.setCancelled(true);
            this.msg.send((CommandSender)player, "filter.repeat", new Object[0]);
            return;
        }
        this.last.put(player.getUniqueId(), new LastMessage(string2, l));
        if (this.plugin.getConfig().getBoolean("chat-filter.swear.enabled", true)) {
            Check check = this.check(string);
            if (check.hit) {
                String string3 = this.plugin.getConfig().getString("chat-filter.swear.action", "censor").toLowerCase(Locale.ROOT);
                if (string3.equals("censor") && check.censored != null) {
                    string = check.censored;
                } else {
                    asyncPlayerChatEvent.setCancelled(true);
                    this.msg.send((CommandSender)player, "filter.swear", new Object[0]);
                    this.punish(player, string3);
                    this.staff("chickensmpac.notify", this.msg.get("filter.swear-notify", "player", player.getName(), "message", string));
                    return;
                }
            }
        }
        if (this.plugin.getConfig().getBoolean("chat-filter.caps.enabled", true)) {
            int n = this.plugin.getConfig().getInt("chat-filter.caps.min-length", 8);
            int n2 = this.plugin.getConfig().getInt("chat-filter.caps.max-percent", 60);
            int n3 = 0;
            int n4 = 0;
            for (char c : string.toCharArray()) {
                if (!Character.isLetter(c)) continue;
                ++n3;
                if (!Character.isUpperCase(c)) continue;
                ++n4;
            }
            if (n3 >= n && n4 * 100 / n3 > n2) {
                string = string.toLowerCase(Locale.ROOT);
            }
        }
        asyncPlayerChatEvent.setMessage(string);
    }

    private void punish(Player player, String string) {
        PunishmentType punishmentType;
        switch (string) {
            case "warn": {
                punishmentType = PunishmentType.WARN;
                break;
            }
            case "mute": {
                punishmentType = PunishmentType.MUTE;
                break;
            }
            case "kick": {
                punishmentType = PunishmentType.KICK;
                break;
            }
            default: {
                punishmentType = null;
            }
        }
        if (punishmentType == null) {
            return;
        }
        long l = punishmentType == PunishmentType.MUTE ? Durations.parse(this.plugin.getConfig().getString("chat-filter.swear.mute-duration", "10m")) : -1L;
        String string2 = this.plugin.getConfig().getString("chat-filter.swear.reason", "Inappropriate language");
        String string3 = PunishmentService.ipOf(player);
        PunishmentService.Target target = new PunishmentService.Target(player.getUniqueId(), player.getName(), string3, false);
        PunishmentService.Operator operator = new PunishmentService.Operator(this.plugin.getConfig().getString("chat-filter.operator", "AutoMod"), null);
        this.plugin.sync(() -> this.plugin.service().punish((CommandSender)Bukkit.getConsoleSender(), operator, target, punishmentType, l, string2, false, "automod-swear", false));
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onSign(SignChangeEvent signChangeEvent) {
        if (!this.enabled() || !this.plugin.getConfig().getBoolean("features.chat-filter", true) || !this.plugin.getConfig().getBoolean("chat-filter.swear.enabled", true) || signChangeEvent.getPlayer().hasPermission("chickensmpac.filter.bypass")) {
            return;
        }
        for (int i = 0; i < 4; ++i) {
            String string = signChangeEvent.getLine(i);
            if (string == null || string.isEmpty()) continue;
            Check check = this.check(string);
            if (!check.hit) continue;
            if (check.censored != null) {
                signChangeEvent.setLine(i, check.censored);
                continue;
            }
            signChangeEvent.setCancelled(true);
            this.msg.send((CommandSender)signChangeEvent.getPlayer(), "filter.swear", new Object[0]);
            return;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        this.last.remove(playerQuitEvent.getPlayer().getUniqueId());
    }

    private Check check(String string) {
        Set<String> set = this.blocked;
        if (set.isEmpty()) {
            return new Check(false, string);
        }
        boolean bl = false;
        String[] stringArray = string.split(" ");
        ArrayList<String> arrayList = new ArrayList<String>(stringArray.length);
        for (String string2 : stringArray) {
            String string3 = ChatModule.normalize(string2);
            boolean bl2 = false;
            if (!string3.isEmpty() && !this.whitelist.contains(string3)) {
                for (String string4 : set) {
                    if (!(string4.length() <= 3 ? string3.equals(string4) : string3.contains(string4))) continue;
                    bl2 = true;
                    break;
                }
            }
            if (bl2) {
                bl = true;
                arrayList.add("*".repeat(Math.max(3, string2.length())));
                continue;
            }
            arrayList.add(string2);
        }
        if (bl) {
            return new Check(true, String.join((CharSequence)" ", arrayList));
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i <= stringArray.length; ++i) {
            String string5;
            String string6 = string5 = i < stringArray.length ? ChatModule.normalize(stringArray[i]) : "";
            if (i < stringArray.length && string5.length() == 1) {
                stringBuilder.append(string5);
                continue;
            }
            if (stringBuilder.length() >= 3) {
                String string2;
                string2 = ChatModule.normalize(stringBuilder.toString());
                for (String string7 : set) {
                    if (!string2.contains(string7) || this.whitelist.contains(string2)) continue;
                    return new Check(true, null);
                }
            }
            stringBuilder.setLength(0);
        }
        return new Check(false, string);
    }

    static String normalize(String string) {
        String string2 = Normalizer.normalize(string.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        StringBuilder stringBuilder = new StringBuilder(string2.length());
        char c = '\u0000';
        for (char c2 : string2.toCharArray()) {
            char c3;
            switch (c2) {
                case '4': 
                case '@': {
                    c3 = 'a';
                    break;
                }
                case '3': {
                    c3 = 'e';
                    break;
                }
                case '!': 
                case '1': 
                case '|': {
                    c3 = 'i';
                    break;
                }
                case '0': {
                    c3 = 'o';
                    break;
                }
                case '$': 
                case '5': {
                    c3 = 's';
                    break;
                }
                case '+': 
                case '7': {
                    c3 = 't';
                    break;
                }
                case '8': {
                    c3 = 'b';
                    break;
                }
                case '9': {
                    c3 = 'g';
                    break;
                }
                case '\u00df': {
                    c3 = 's';
                    break;
                }
                default: {
                    c3 = c2;
                }
            }
            if (c3 < 'a' || c3 > 'z' || c3 == c) continue;
            stringBuilder.append(c3);
            c = c3;
        }
        return stringBuilder.toString();
    }

    private record LastMessage(String text, long time) {
    }

    private record Check(boolean hit, String censored) {
    }
}

