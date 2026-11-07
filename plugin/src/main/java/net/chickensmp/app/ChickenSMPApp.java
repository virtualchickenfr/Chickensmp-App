package net.chickensmp.app;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ChickenSMPApp extends JavaPlugin implements Listener {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    private final Gson gson = new Gson();
    private final Map<UUID, Long> lastCode = new ConcurrentHashMap<>();
    private HttpClient http;
    private String apiUrl;
    private String secret;
    private volatile String lastStatus = "not connected yet";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        apiUrl = getConfig().getString("api-url", "https://store.chickensmp.net/api/mc");
        secret = getConfig().getString("secret", "");
        if (secret == null || secret.length() < 32) {
            byte[] b = new byte[32];
            random.nextBytes(b);
            secret = HexFormat.of().formatHex(b);
            getConfig().set("secret", secret);
            saveConfig();
        }
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        getServer().getPluginManager().registerEvents(this, this);

        JsonObject hello = new JsonObject();
        hello.addProperty("action", "hello");
        post(hello).thenAccept(ok -> getLogger().info(ok ? "Connected to the ChickenSMP app backend." : "Could not connect to the app backend: " + lastStatus));

        long period = Math.max(1, getConfig().getInt("sync-minutes", 5)) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(this, this::syncOnline, 20L * 30, period);
        // Daily-login rewards claimed in the app: check every minute for online players.
        Bukkit.getScheduler().runTaskTimer(this, this::deliverRewards, 20L * 20, 20L * 60);
        // Send everyone who ever joined once, a little after startup, in small batches.
        Bukkit.getScheduler().runTaskLater(this, this::syncAllOffline, 20L * 60);
    }

    /* ---------- commands ---------- */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("chickenapp")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("sync")) {
                syncOnline();
                syncAllOffline();
                sender.sendMessage(Component.text("Sync started.", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("App backend: " + lastStatus, NamedTextColor.YELLOW));
            }
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /app.");
            return true;
        }
        long now = System.currentTimeMillis();
        Long last = lastCode.get(player.getUniqueId());
        if (last != null && now - last < 15_000) {
            player.sendMessage(Component.text("Wait a few seconds before requesting a new code.", NamedTextColor.RED));
            return true;
        }
        lastCode.put(player.getUniqueId(), now);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        String code = sb.toString();
        String shown = code.substring(0, 4) + "-" + code.substring(4);

        JsonObject body = new JsonObject();
        body.addProperty("action", "code");
        body.addProperty("code", code);
        body.addProperty("uuid", player.getUniqueId().toString());
        body.addProperty("name", player.getName());
        JsonArray players = new JsonArray();
        players.add(stats(player, true));
        JsonObject statsBody = new JsonObject();
        statsBody.addProperty("action", "stats");
        statsBody.add("players", players);

        player.sendMessage(Component.text("Creating your app code...", NamedTextColor.GRAY));
        post(statsBody);
        post(body).thenAccept(ok -> Bukkit.getScheduler().runTask(this, () -> {
            if (!player.isOnline()) return;
            if (!ok) {
                player.sendMessage(Component.text("The app is not reachable right now. Try again later.", NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("ChickenSMP App", NamedTextColor.AQUA, TextDecoration.BOLD));
            player.sendMessage(Component.text("Your login code: ", NamedTextColor.GRAY)
                    .append(Component.text(shown, NamedTextColor.GREEN, TextDecoration.BOLD)
                            .clickEvent(ClickEvent.copyToClipboard(code))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to copy")))));
            player.sendMessage(Component.text("Open the app → Profile and enter the code. Valid for 10 minutes.", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Dein Code gilt 10 Minuten – in der App unter Profil eingeben.", NamedTextColor.DARK_GRAY));
            player.sendMessage(Component.empty());
        }));
        return true;
    }

    /* ---------- rewards ---------- */

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(this, this::deliverRewards, 20L * 5);
    }

    private void deliverRewards() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        JsonArray online = new JsonArray();
        for (Player p : Bukkit.getOnlinePlayers()) online.add(p.getUniqueId().toString());
        JsonObject body = new JsonObject();
        body.addProperty("action", "pending");
        body.add("online", online);
        postForJson(body).thenAccept(res -> {
            if (res == null || !res.has("rewards")) return;
            JsonArray list = res.getAsJsonArray("rewards");
            if (list.size() == 0) return;
            Bukkit.getScheduler().runTask(this, () -> {
                JsonArray ids = new JsonArray();
                JsonArray uuids = new JsonArray();
                for (var el : list) {
                    JsonObject r = el.getAsJsonObject();
                    Player p = Bukkit.getPlayer(UUID.fromString(r.get("uuid").getAsString()));
                    if (p == null) continue;
                    String key = r.get("key").getAsString().replaceAll("[^a-zA-Z0-9_-]", "");
                    int amount = Math.max(1, Math.min(64, r.get("amount").getAsInt()));
                    String cmd = getConfig().getString("reward-command", "keymanager give {player} {key} {amount}")
                            .replace("{player}", p.getName()).replace("{key}", key).replace("{amount}", String.valueOf(amount));
                    boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    if (!ok) { getLogger().warning("Reward command failed: " + cmd); continue; }
                    ids.add(r.get("id").getAsString());
                    uuids.add(p.getUniqueId().toString());
                    p.sendMessage(Component.text("🎁 ", NamedTextColor.GOLD)
                            .append(Component.text("Daily reward from the app: ", NamedTextColor.GRAY))
                            .append(Component.text(amount + "x " + key.substring(0, 1).toUpperCase() + key.substring(1) + " Key", NamedTextColor.GREEN, TextDecoration.BOLD)));
                }
                if (ids.size() == 0) return;
                JsonObject ack = new JsonObject();
                ack.addProperty("action", "ack");
                ack.add("ids", ids);
                ack.add("uuids", uuids);
                post(ack);
            });
        });
    }

    /* ---------- stats ---------- */

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        JsonArray arr = new JsonArray();
        arr.add(stats(e.getPlayer(), false));
        JsonObject body = new JsonObject();
        body.addProperty("action", "stats");
        body.add("players", arr);
        post(body);
    }

    private void syncOnline() {
        JsonArray arr = new JsonArray();
        JsonArray names = new JsonArray();
        for (Player p : Bukkit.getOnlinePlayers()) {
            arr.add(stats(p, true));
            names.add(p.getName());
        }
        JsonObject body = new JsonObject();
        body.addProperty("action", "stats");
        body.add("players", arr);
        body.add("onlineNames", names);
        post(body);
    }

    private void syncAllOffline() {
        OfflinePlayer[] all = Bukkit.getOfflinePlayers();
        List<OfflinePlayer> list = new ArrayList<>();
        for (OfflinePlayer p : all) if (!p.isOnline() && p.getName() != null) list.add(p);
        final int batch = 40;
        for (int start = 0; start < list.size() && start < 2000; start += batch) {
            final int from = start;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                JsonArray arr = new JsonArray();
                for (int i = from; i < Math.min(from + batch, list.size()); i++) {
                    try { arr.add(stats(list.get(i), false)); } catch (Exception ignored) { }
                }
                JsonObject body = new JsonObject();
                body.addProperty("action", "stats");
                body.add("players", arr);
                post(body);
            }, 2L * (from / batch));
        }
    }

    private JsonObject stats(OfflinePlayer p, boolean online) {
        JsonObject o = new JsonObject();
        o.addProperty("uuid", p.getUniqueId().toString());
        o.addProperty("name", p.getName());
        o.addProperty("online", online);
        o.addProperty("bedrock", p.getName() != null && p.getName().startsWith("."));
        o.addProperty("playtime", stat(p, Statistic.PLAY_ONE_MINUTE));
        o.addProperty("deaths", stat(p, Statistic.DEATHS));
        o.addProperty("playerKills", stat(p, Statistic.PLAYER_KILLS));
        o.addProperty("mobKills", stat(p, Statistic.MOB_KILLS));
        o.addProperty("jumps", stat(p, Statistic.JUMP));
        o.addProperty("walkCm", stat(p, Statistic.WALK_ONE_CM) + stat(p, Statistic.SPRINT_ONE_CM));
        o.addProperty("joins", stat(p, Statistic.LEAVE_GAME) + (online ? 1 : 0));
        o.addProperty("firstPlayed", p.getFirstPlayed());
        o.addProperty("lastSeen", online ? System.currentTimeMillis() : p.getLastPlayed());
        String rank = vaultRank(p);
        if (rank == null) rank = luckPermsRank(p);
        if (rank != null) o.addProperty("rank", rank);
        JsonArray groups = vaultGroups(p);
        if (groups != null) o.add("groups", groups);
        String prefix = vaultPrefix(p);
        if (prefix != null && !prefix.isBlank()) o.addProperty("prefix", prefix);
        Double bal = vaultBalance(p);
        if (bal != null) o.addProperty("balance", bal);
        return o;
    }

    private long stat(OfflinePlayer p, Statistic s) {
        try { return p.getStatistic(s); } catch (Exception e) { return 0; }
    }

    /* ---------- Vault (optional, via reflection) ---------- */

    private Object service(String className) {
        try {
            Class<?> c = Class.forName(className);
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(c);
            return rsp == null ? null : rsp.getProvider();
        } catch (Throwable t) {
            return null;
        }
    }

    private String vaultRank(OfflinePlayer p) {
        Object perms = service("net.milkbowl.vault.permission.Permission");
        if (perms == null) return null;
        try {
            Method m = Class.forName("net.milkbowl.vault.permission.Permission").getMethod("getPrimaryGroup", String.class, OfflinePlayer.class);
            Object r = m.invoke(perms, null, p);
            return r == null ? null : r.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private JsonArray vaultGroups(OfflinePlayer p) {
        Object perms = service("net.milkbowl.vault.permission.Permission");
        if (perms == null) return null;
        try {
            Method m = Class.forName("net.milkbowl.vault.permission.Permission").getMethod("getPlayerGroups", String.class, OfflinePlayer.class);
            Object r = m.invoke(perms, null, p);
            if (!(r instanceof String[] arr)) return null;
            JsonArray out = new JsonArray();
            for (String g : arr) if (g != null && out.size() < 10) out.add(g);
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    private String vaultPrefix(OfflinePlayer p) {
        Object chat = service("net.milkbowl.vault.chat.Chat");
        if (chat == null) return null;
        try {
            Method m = Class.forName("net.milkbowl.vault.chat.Chat").getMethod("getPlayerPrefix", String.class, OfflinePlayer.class);
            Object r = m.invoke(chat, null, p);
            if (r == null) return null;
            // strip legacy colour codes (&a, §a, &#rrggbb) and MiniMessage tags
            return r.toString()
                    .replaceAll("(?i)[&§]#[0-9a-f]{6}", "")
                    .replaceAll("(?i)[&§][0-9a-fk-orx]", "")
                    .replaceAll("<[^>]{1,40}>", "")
                    .replace("[", "").replace("]", "")
                    .trim();
        } catch (Throwable t) {
            return null;
        }
    }

    private String luckPermsRank(OfflinePlayer p) {
        try {
            Class<?> prov = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object lp = prov.getMethod("get").invoke(null);
            Object um = Class.forName("net.luckperms.api.LuckPerms").getMethod("getUserManager").invoke(lp);
            Object user = Class.forName("net.luckperms.api.model.user.UserManager").getMethod("getUser", UUID.class).invoke(um, p.getUniqueId());
            if (user == null) return null;
            Object g = Class.forName("net.luckperms.api.model.user.User").getMethod("getPrimaryGroup").invoke(user);
            return g == null ? null : g.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private Double vaultBalance(OfflinePlayer p) {
        Object eco = service("net.milkbowl.vault.economy.Economy");
        if (eco == null) return null;
        try {
            Method m = Class.forName("net.milkbowl.vault.economy.Economy").getMethod("getBalance", OfflinePlayer.class);
            Object r = m.invoke(eco, p);
            return r instanceof Number n ? n.doubleValue() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /* ---------- http ---------- */

    private CompletableFuture<JsonObject> postForJson(JsonObject body) {
        body.addProperty("secret", secret);
        HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> r.statusCode() == 200 ? gson.fromJson(r.body(), JsonObject.class) : null)
                .exceptionally(t -> null);
    }

    private CompletableFuture<Boolean> post(JsonObject body) {
        body.addProperty("secret", secret);
        HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {
                    boolean ok = r.statusCode() == 200;
                    lastStatus = ok ? "OK" : ("HTTP " + r.statusCode() + " " + r.body());
                    if (!ok) getLogger().warning("App backend answered " + lastStatus);
                    return ok;
                })
                .exceptionally(t -> {
                    lastStatus = "error: " + t.getMessage();
                    getLogger().warning("App backend unreachable: " + t.getMessage());
                    return false;
                });
    }
}
