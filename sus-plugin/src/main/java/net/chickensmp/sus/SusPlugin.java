package net.chickensmp.sus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class SusPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Map<UUID, PlayerData> data = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private VulcanHook hook;
    private File dataFile;

    // config cache
    private long windowMs;
    private double susScore, watchScore, expMult;
    private long alertCooldownMs;
    private int listSize;
    private Map<String, Double> weights = new HashMap<>();
    private Set<String> ignored = new HashSet<>();
    private Sound alertSound;
    private String webhook, webhookName;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        dataFile = new File(getDataFolder(), "data.yml");
        loadData();

        hook = new VulcanHook(this);
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("sus")).setExecutor(this);
        Objects.requireNonNull(getCommand("sus")).setTabCompleter(this);

        // Vulcan may enable after us: try now, on its enable event, and once after startup.
        if (!hook.tryHook()) {
            Bukkit.getScheduler().runTask(this, () -> {
                if (!hook.tryHook()) getLogger().warning("Vulcan is not installed or not enabled - no flags are recorded yet.");
            });
        }
        // Save every 5 minutes.
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::saveData, 6000L, 6000L);
        getLogger().info("ChickenSMPSus enabled (" + data.size() + " players in history).");
    }

    @Override
    public void onDisable() {
        saveData();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent e) {
        if (e.getPlugin().getName().equals("Vulcan")) hook.tryHook();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        PlayerData d = data.get(e.getPlayer().getUniqueId());
        if (d != null) d.name = e.getPlayer().getName();
    }

    // ------------------------------------------------------------ settings

    private void loadSettings() {
        reloadConfig();
        FileConfiguration c = getConfig();
        windowMs = Math.max(1, c.getLong("window-minutes", 30)) * 60_000L;
        susScore = c.getDouble("sus-score", 15);
        watchScore = c.getDouble("watch-score", 6);
        expMult = c.getDouble("experimental-multiplier", 0.5);
        alertCooldownMs = Math.max(0, c.getLong("alert-cooldown-minutes", 5)) * 60_000L;
        listSize = Math.max(1, c.getInt("list-size", 10));
        weights = new HashMap<>();
        ConfigurationSection w = c.getConfigurationSection("weights");
        if (w != null) for (String k : w.getKeys(false)) weights.put(k.toLowerCase(Locale.ROOT), w.getDouble(k));
        ignored = c.getStringList("ignored-checks").stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        String snd = c.getString("alert-sound", "");
        alertSound = null;
        if (snd != null && !snd.isBlank()) {
            try {
                alertSound = (Sound) Sound.class.getField(snd.toUpperCase(Locale.ROOT)).get(null);
            } catch (ReflectiveOperationException ex) {
                try { alertSound = Registry.SOUNDS.get(NamespacedKey.minecraft(snd.toLowerCase(Locale.ROOT))); }
                catch (IllegalArgumentException ignored2) { alertSound = null; }
            }
            if (alertSound == null) getLogger().warning("Unknown alert-sound: " + snd);
        }
        webhook = c.getString("discord-webhook-url", "").trim();
        webhookName = c.getString("discord-username", "ChickenSMP Sus");
    }

    private String msg(String key) {
        return getConfig().getString("messages." + key, key);
    }

    private Component mm(String key, Map<String, String> vars, boolean prefix) {
        String s = msg(key);
        for (Map.Entry<String, String> e : vars.entrySet()) s = s.replace("{" + e.getKey() + "}", e.getValue());
        return MM.deserialize((prefix ? msg("prefix") : "") + s);
    }

    private void send(CommandSender to, String key, Map<String, String> vars) {
        to.sendMessage(mm(key, vars, true));
    }

    private static String esc(String s) {
        return MM.escapeTags(s == null ? "" : s);
    }

    // ------------------------------------------------------------ flags

    void recordFlag(UUID uuid, String name, String checkName, char type, String category,
                    boolean experimental, int vl, String info) {
        if (ignored.contains(checkName.toLowerCase(Locale.ROOT))) return;
        double pts = weights.getOrDefault(category == null ? "default" : category.toLowerCase(Locale.ROOT),
                weights.getOrDefault("default", 1.0));
        if (experimental) pts *= expMult;
        if (pts <= 0) return;

        long now = System.currentTimeMillis();
        String check = checkName + " (" + type + ")";
        PlayerData d = data.computeIfAbsent(uuid, u -> new PlayerData(u, name));
        d.name = name;
        d.add(new PlayerData.Flag(now, check, category == null ? "?" : category, pts, vl, info == null ? "" : info));

        double score = d.score(now - windowMs);
        boolean alert = false;
        synchronized (d) {
            if (score >= susScore) {
                if (!d.sus) { d.sus = true; d.susTimes++; }
                if (now - d.lastAlert >= alertCooldownMs) { d.lastAlert = now; alert = true; }
            } else if (score < watchScore) {
                d.sus = false;
            }
        }
        if (alert) {
            String top = PlayerData.top(d.recentChecks(now - windowMs), 3);
            Bukkit.getScheduler().runTask(this, () -> alertStaff(d.name, score, top));
            postDiscord(d, score, top);
        }
    }

    private void alertStaff(String name, double score, String top) {
        Component c = mm("alert", Map.of("player", esc(name), "score", fmt(score),
                "window", String.valueOf(windowMs / 60_000L), "top", esc(top)), true);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("chickensmp.sus.alerts")) continue;
            p.sendMessage(c);
            if (alertSound != null) p.playSound(p.getLocation(), alertSound, 1f, 1.4f);
        }
        Bukkit.getConsoleSender().sendMessage(c);
    }

    private void postDiscord(PlayerData d, double score, String top) {
        if (webhook == null || !webhook.startsWith("https://")) return;
        String json = "{\"username\":" + jstr(webhookName) + ",\"embeds\":[{"
                + "\"title\":\"Suspicious player\",\"color\":15158332,"
                + "\"fields\":["
                + "{\"name\":\"Player\",\"value\":" + jstr(d.name) + ",\"inline\":true},"
                + "{\"name\":\"Score\",\"value\":" + jstr(fmt(score) + " in " + windowMs / 60_000L + "m") + ",\"inline\":true},"
                + "{\"name\":\"Times sus\",\"value\":" + jstr(String.valueOf(d.susTimes)) + ",\"inline\":true},"
                + "{\"name\":\"Top checks\",\"value\":" + jstr(top) + "}"
                + "],\"footer\":{\"text\":\"ChickenSMP\"}}]}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(webhook)).timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        http.sendAsync(req, HttpResponse.BodyHandlers.discarding()).thenAccept(r -> {
            if (r.statusCode() >= 300) getLogger().warning("Discord webhook answered HTTP " + r.statusCode());
        });
    }

    private static String jstr(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char ch : (s == null ? "" : s).toCharArray()) {
            switch (ch) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                default -> { if (ch < 0x20) b.append(String.format("\\u%04x", (int) ch)); else b.append(ch); }
            }
        }
        return b.append('"').toString();
    }

    private static String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.1f", d);
    }

    // ------------------------------------------------------------ command

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) { list(s); return true; }
        String sub = a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> send(s, "usage", Map.of());
            case "reload" -> {
                if (!admin(s)) return true;
                loadSettings();
                hook.tryHook();
                send(s, "reloaded", Map.of());
            }
            case "clear" -> {
                if (!admin(s) || a.length < 2) { if (a.length < 2) send(s, "usage", Map.of()); return true; }
                PlayerData d = find(a[1]);
                if (d == null) { send(s, "no-data", Map.of("player", esc(a[1]))); return true; }
                data.remove(d.uuid);
                Bukkit.getScheduler().runTaskAsynchronously(this, this::saveData);
                send(s, "cleared", Map.of("player", esc(d.name)));
            }
            case "tp" -> {
                if (!(s instanceof Player p) || a.length < 2) { send(s, "usage", Map.of()); return true; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { send(s, "not-online", Map.of("player", esc(a[1]))); return true; }
                p.teleportAsync(t.getLocation());
                send(s, "teleported", Map.of("player", esc(t.getName())));
            }
            case "test" -> {
                // /sus test <player> [check] [category] - simulates a Vulcan flag, for testing alerts.
                if (!admin(s) || a.length < 2) { if (a.length < 2) send(s, "usage", Map.of()); return true; }
                OfflinePlayer op = Bukkit.getOfflinePlayer(a[1]);
                String check = a.length > 2 ? a[2] : "Test";
                String cat = a.length > 3 ? a[3] : "combat";
                recordFlag(op.getUniqueId(), op.getName() == null ? a[1] : op.getName(), check, 'A', cat, false, 1, "simulated by " + s.getName());
                s.sendMessage(MM.deserialize(msg("prefix") + "<gray>Simulated flag <white>" + esc(check) + "</white> for <white>" + esc(a[1]) + "</white>.</gray>"));
            }
            default -> info(s, a[0]);
        }
        return true;
    }

    private boolean admin(CommandSender s) {
        if (s.hasPermission("chickensmp.sus.admin")) return true;
        s.sendMessage(MM.deserialize(msg("prefix") + "<red>No permission.</red>"));
        return false;
    }

    private PlayerData find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return data.get(online.getUniqueId());
        for (PlayerData d : data.values()) if (d.name != null && d.name.equalsIgnoreCase(name)) return d;
        return null;
    }

    private void list(CommandSender s) {
        long cutoff = System.currentTimeMillis() - windowMs;
        String win = String.valueOf(windowMs / 60_000L);
        if (!hook.isHooked()) send(s, "vulcan-missing", Map.of());
        List<Map.Entry<PlayerData, Double>> rows = data.values().stream()
                .map(d -> Map.entry(d, d.score(cutoff)))
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<PlayerData, Double>comparingByValue().reversed())
                .limit(listSize).toList();
        if (rows.isEmpty()) { send(s, "list-empty", Map.of("window", win)); return; }
        send(s, "list-header", Map.of("window", win));
        int rank = 1;
        for (Map.Entry<PlayerData, Double> r : rows) {
            PlayerData d = r.getKey();
            double sc = r.getValue();
            String color = sc >= susScore ? "<red><bold>" : sc >= watchScore ? "<yellow>" : "<green>";
            s.sendMessage(mm("list-line", Map.of(
                    "rank", String.valueOf(rank++),
                    "player", esc(d.name),
                    "color", color,
                    "score", fmt(sc),
                    "top", esc(PlayerData.top(d.recentChecks(cutoff), 2)),
                    "online", Bukkit.getPlayer(d.uuid) != null ? msg("online-tag") : ""), false));
        }
    }

    private void info(CommandSender s, String name) {
        PlayerData d = find(name);
        if (d == null) { send(s, "no-data", Map.of("player", esc(name))); return; }
        long now = System.currentTimeMillis();
        long cutoff = now - windowMs;
        double sc = d.score(cutoff);
        String state = sc >= susScore ? "<red><bold>SUSPICIOUS</bold></red>" : sc >= watchScore ? "<yellow>WATCH</yellow>" : "<green>OK</green>";
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        boolean online = Bukkit.getPlayer(d.uuid) != null;
        String n = esc(d.name);
        List<String> lines = new ArrayList<>();
        lines.add(msg("prefix") + "<white><bold>" + n + "</bold></white> " + state + (online ? " <green>● online</green>" : " <gray>offline</gray>"));
        lines.add("<gray>Score: <white>" + fmt(sc) + "</white> / " + fmt(susScore) + " <dark_gray>(" + d.recentCount(cutoff) + " flags in " + windowMs / 60_000L + "m)</dark_gray>");
        lines.add("<gray>Recent checks: <white>" + esc(PlayerData.top(d.recentChecks(cutoff), 5)) + "</white>");
        lines.add("<gray>All time: <white>" + d.totalFlags + "</white> flags, sus <white>" + d.susTimes + "</white>x, first " + (d.firstFlag == 0 ? "-" : df.format(new Date(d.firstFlag))) + ", last " + (d.lastFlag == 0 ? "-" : df.format(new Date(d.lastFlag))));
        lines.add("<gray>Top checks ever: <white>" + esc(PlayerData.top(d.totalChecks(), 5)) + "</white>");
        List<PlayerData.Flag> last = d.lastFlags(5);
        if (!last.isEmpty()) {
            lines.add("<gray>Last flags:</gray>");
            for (int i = last.size() - 1; i >= 0; i--) {
                PlayerData.Flag f = last.get(i);
                long ago = (now - f.time()) / 1000;
                String agoS = ago < 60 ? ago + "s" : ago < 3600 ? ago / 60 + "m" : ago / 3600 + "h";
                String inf = f.info().length() > 60 ? f.info().substring(0, 60) + "..." : f.info();
                lines.add(" <dark_gray>-</dark_gray> <white>" + esc(f.check()) + "</white> <gray>VL " + f.vl() + ", " + agoS + " ago</gray> <dark_gray>" + esc(inf) + "</dark_gray>");
            }
        }
        StringBuilder actions = new StringBuilder();
        if (online) actions.append("<click:run_command:'/sus tp ").append(n).append("'><green>[TP]</green></click> ");
        actions.append("<click:run_command:'/history ").append(n).append("'><aqua>[History]</aqua></click> ");
        actions.append("<click:suggest_command:'/punish ").append(n).append("'><red>[Punish]</red></click> ");
        if (s.hasPermission("chickensmp.sus.admin"))
            actions.append("<click:suggest_command:'/sus clear ").append(n).append("'><gray>[Clear]</gray></click>");
        lines.add(actions.toString());
        for (String l : lines) s.sendMessage(MM.deserialize(l));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            out.addAll(List.of("tp", "help"));
            if (s.hasPermission("chickensmp.sus.admin")) out.addAll(List.of("clear", "reload", "test"));
            for (PlayerData d : data.values()) if (d.name != null) out.add(d.name);
        } else if (a.length == 2 && List.of("tp", "clear", "test").contains(a[0].toLowerCase(Locale.ROOT))) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            if (!a[0].equalsIgnoreCase("tp")) for (PlayerData d : data.values()) if (d.name != null) out.add(d.name);
        }
        String pre = a[a.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().distinct().filter(x -> x.toLowerCase(Locale.ROOT).startsWith(pre)).sorted().toList();
    }

    // ------------------------------------------------------------ storage

    private void loadData() {
        if (!dataFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = y.getConfigurationSection("players");
        if (root == null) return;
        long cutoff = System.currentTimeMillis() - windowMs;
        for (String key : root.getKeys(false)) {
            try {
                ConfigurationSection p = root.getConfigurationSection(key);
                if (p == null) continue;
                PlayerData d = new PlayerData(UUID.fromString(key), p.getString("name", "?"));
                d.totalFlags = p.getInt("total");
                d.susTimes = p.getInt("sus-times");
                d.firstFlag = p.getLong("first");
                d.lastFlag = p.getLong("last");
                ConfigurationSection t = p.getConfigurationSection("checks");
                if (t != null) for (String c : t.getKeys(false)) d.totals.put(c.replace('_', '.'), t.getInt(c));
                for (Map<?, ?> m : p.getMapList("recent")) {
                    long time = ((Number) m.get("t")).longValue();
                    if (time < cutoff) continue;
                    d.recent.add(new PlayerData.Flag(time, String.valueOf(m.get("c")), String.valueOf(m.get("k")),
                            ((Number) m.get("p")).doubleValue(), ((Number) m.get("v")).intValue(), String.valueOf(m.get("i"))));
                }
                data.put(d.uuid, d);
            } catch (Exception ex) {
                getLogger().warning("Skipping broken entry " + key + ": " + ex.getMessage());
            }
        }
    }

    private synchronized void saveData() {
        YamlConfiguration y = new YamlConfiguration();
        long cutoff = System.currentTimeMillis() - windowMs;
        for (PlayerData d : data.values()) {
            synchronized (d) {
                d.prune(cutoff);
                String base = "players." + d.uuid;
                y.set(base + ".name", d.name);
                y.set(base + ".total", d.totalFlags);
                y.set(base + ".sus-times", d.susTimes);
                y.set(base + ".first", d.firstFlag);
                y.set(base + ".last", d.lastFlag);
                for (Map.Entry<String, Integer> e : d.totals.entrySet())
                    y.set(base + ".checks." + e.getKey().replace('.', '_'), e.getValue());
                List<Map<String, Object>> rec = new ArrayList<>();
                for (PlayerData.Flag f : d.recent) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("t", f.time()); m.put("c", f.check()); m.put("k", f.category());
                    m.put("p", f.points()); m.put("v", f.vl()); m.put("i", f.info());
                    rec.add(m);
                }
                y.set(base + ".recent", rec);
            }
        }
        try {
            File tmp = new File(getDataFolder(), "data.yml.tmp");
            y.save(tmp);
            if (!tmp.renameTo(dataFile)) { y.save(dataFile); tmp.delete(); }
        } catch (IOException ex) {
            getLogger().warning("Could not save data.yml: " + ex.getMessage());
        }
    }
}
