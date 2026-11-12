package net.chickensmp.announce;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class AnnouncePlugin extends JavaPlugin {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Random rng = new Random();
    private NamespacedKey offKey;
    private BukkitTask task;
    private List<String> ids = new ArrayList<>();
    private int index = 0;
    private String last = null;
    private boolean papi;

    @Override
    public void onEnable() {
        offKey = new NamespacedKey(this, "off");
        saveDefaultConfig();
        papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        start();
        getLogger().info("Loaded " + ids.size() + " announcements, every " + getConfig().getInt("interval-minutes", 5) + " min.");
    }

    private void start() {
        if (task != null) task.cancel();
        ConfigurationSection s = getConfig().getConfigurationSection("messages");
        ids = s == null ? new ArrayList<>() : new ArrayList<>(s.getKeys(false));
        index = 0;
        long ticks = Math.max(1, getConfig().getInt("interval-minutes", 5)) * 60L * 20L;
        task = Bukkit.getScheduler().runTaskTimer(this, this::tick, ticks, ticks);
    }

    private void tick() {
        if (ids.isEmpty() || Bukkit.getOnlinePlayers().size() < getConfig().getInt("min-players", 1)) return;
        String id;
        if (getConfig().getBoolean("random", true) && ids.size() > 1) {
            do { id = ids.get(rng.nextInt(ids.size())); } while (id.equals(last));
        } else {
            id = ids.get(index++ % ids.size());
        }
        last = id;
        for (Player p : Bukkit.getOnlinePlayers()) send(p, id);
    }

    private boolean isOff(Player p) {
        return p.getPersistentDataContainer().has(offKey, PersistentDataType.BYTE);
    }

    private void send(Player p, String id) {
        if (isOff(p)) return;
        String lang = "en";
        // English-only server
        List<String> lines = getConfig().getStringList("messages." + id + "." + lang);
        if (lines.isEmpty()) lines = getConfig().getStringList("messages." + id + ".en");
        if (lines.isEmpty()) return;
        boolean blank = getConfig().getBoolean("blank-lines", true);
        if (blank) p.sendMessage(Component.empty());
        String prefix = getConfig().getString("prefix", "");
        boolean first = true;
        for (String line : lines) {
            String text = fill(p, (first ? prefix : "") + line);
            first = false;
            try { p.sendMessage(mm.deserialize(text)); } catch (Exception e) { p.sendMessage(Component.text(text)); }
        }
        if (blank) p.sendMessage(Component.empty());
        String snd = getConfig().getString("sound", "");
        if (snd != null && !snd.isBlank()) {
            try {
                String[] a = snd.split("\\|");
                p.playSound(Sound.sound(Key.key(a[0]), Sound.Source.MASTER,
                        a.length > 1 ? Float.parseFloat(a[1]) : 1f, a.length > 2 ? Float.parseFloat(a[2]) : 1f));
            } catch (Exception ignored) { }
        }
    }

    private String fill(Player p, String s) {
        ConfigurationSection l = getConfig().getConfigurationSection("links");
        if (l != null) for (String k : l.getKeys(false)) s = s.replace("{" + k + "}", l.getString(k, ""));
        s = s.replace("{player}", p.getName()).replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        if (papi && s.contains("%")) {
            try { s = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, s); } catch (Throwable ignored) { }
        }
        return s;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        boolean admin = s.hasPermission("chickenannounce.admin");
        if (a.length == 0 || !admin) {
            if (!(s instanceof Player p)) { s.sendMessage("/announcer reload | list | send <id> | next"); return true; }
            boolean de = false;
            String prefix = getConfig().getString("prefix", "");
            if (isOff(p)) {
                p.getPersistentDataContainer().remove(offKey);
                p.sendMessage(mm.deserialize(prefix + (de ? "<green>Automatische Nachrichten sind wieder an." : "<green>Automatic announcements turned on.")));
            } else {
                p.getPersistentDataContainer().set(offKey, PersistentDataType.BYTE, (byte) 1);
                p.sendMessage(mm.deserialize(prefix + (de ? "<gray>Automatische Nachrichten sind aus. Mit <aqua>/announcer</aqua> wieder an." : "<gray>Automatic announcements turned off. <aqua>/announcer</aqua> turns them back on.")));
            }
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "reload" -> { reloadConfig(); start(); s.sendMessage("§aReloaded: " + ids.size() + " announcements, every " + getConfig().getInt("interval-minutes", 5) + " min."); }
            case "list" -> s.sendMessage("§bAnnouncements: §f" + String.join(", ", ids));
            case "next" -> { tick(); s.sendMessage("§aSent the next announcement."); }
            case "send" -> {
                if (a.length < 2 || !ids.contains(a[1])) { s.sendMessage("§c/announcer send <" + String.join("|", ids) + ">"); return true; }
                for (Player p : Bukkit.getOnlinePlayers()) send(p, a[1]);
                s.sendMessage("§aSent " + a[1] + ".");
            }
            case "preview" -> {
                if (!(s instanceof Player p) || a.length < 2) { s.sendMessage("§c/announcer preview <id>"); return true; }
                send(p, a[1]);
            }
            default -> s.sendMessage("§c/announcer reload | list | send <id> | preview <id> | next");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (!s.hasPermission("chickenannounce.admin")) return List.of();
        if (a.length == 1) return List.of("reload", "list", "send", "preview", "next");
        if (a.length == 2 && (a[0].equalsIgnoreCase("send") || a[0].equalsIgnoreCase("preview"))) return ids;
        return List.of();
    }
}
