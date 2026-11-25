package net.chickensmp.stocks;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** ChickenSMP Ore Market: five ores, prices move on their own, buy low and sell high. */
public final class StocksPlugin extends JavaPlugin implements Listener {

    static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().character('&').hexColors().build();
    static final DecimalFormat MONEY = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    static final DecimalFormat PCT = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US));
    static final String GREEN = "&#00FC00", RED = "&#FC0000", YELLOW = "&#FCE300", BLUE = "&#00A4FC";
    /** Prices kept for the "last hour" change and the chart. */
    static final int HISTORY = 60;

    final Map<String, Ore> ores = new LinkedHashMap<>();
    private final Map<String, Double> oldPrices = new java.util.HashMap<>();
    private File dataFile;
    YamlConfiguration data;
    private Economy economy;
    private BukkitTask tickTask, saveTask, refreshTask;
    long nextTick;
    long tickSeconds = 60;
    private boolean dirty;
    Menus menus;

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dataFile = new File(getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection oldSec = data.getConfigurationSection("prices");
        if (oldSec != null) for (String id : oldSec.getKeys(false)) oldPrices.put(id, data.getDouble("prices." + id + ".price"));
        loadOres();
        menus = new Menus(this);
        getServer().getPluginManager().registerEvents(menus, this);
        getServer().getPluginManager().registerEvents(this, this);
        hookEconomy();
        // EconomyCore registers its Vault economy late: try again once everything is loaded.
        Bukkit.getScheduler().runTask(this, () -> { hookEconomy(); migrateOldStocks(); });
        getCommand("stocks").setExecutor((sender, cmd, label, args) -> command(sender, args));
        getCommand("stocks").setTabCompleter((sender, cmd, label, args) -> {
            if (!sender.hasPermission("stocks.admin")) return List.of();
            if (args.length == 1) return List.of("reload", "set", "news");
            if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) return new ArrayList<>(ores.keySet());
            if (args.length == 3 && args[0].equalsIgnoreCase("news")) return List.of("up", "down");
            return List.of();
        });
        startTasks();
        getLogger().info("ChickenSMPStocks enabled with " + ores.size() + " ores.");
    }

    @Override
    public void onDisable() {
        storePrices();
        save();
    }

    private void startTasks() {
        if (tickTask != null) tickTask.cancel();
        if (saveTask != null) saveTask.cancel();
        if (refreshTask != null) refreshTask.cancel();
        tickSeconds = Math.max(5, getConfig().getLong("tick-seconds", 60));
        long ticks = tickSeconds * 20L;
        nextTick = System.currentTimeMillis() + tickSeconds * 1000L;
        tickTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            tick();
            nextTick = System.currentTimeMillis() + tickSeconds * 1000L;
        }, ticks, ticks);
        saveTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (dirty) { storePrices(); save(); }
        }, 600L, 600L);
        refreshTask = Bukkit.getScheduler().runTaskTimer(this, () -> menus.refreshAll(), 20L, 20L);
    }

    private void hookEconomy() {
        economy = null;
        if (getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) economy = rsp.getProvider();
    }

    // ------------------------------------------------------------------ ores and data

    private void loadOres() {
        ores.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("ores");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            Ore o = new Ore();
            o.id = id.toLowerCase(Locale.ROOT);
            o.name = s.getString("name", id);
            Material m = Material.matchMaterial(s.getString("material", "STONE"));
            o.material = m == null || !m.isItem() ? Material.STONE : m;
            o.base = Math.max(1, s.getDouble("price", 100));
            o.swing = Math.max(0, s.getDouble("swing", 1.0));
            o.risk = s.getString("risk", "MEDIUM");
            o.max = Math.max(1, s.getInt("max", 100));
            o.newsUp = s.getStringList("news-up");
            o.newsDown = s.getStringList("news-down");
            // Version 2 had companies called "diamond" and "netherite" (with a "change" key):
            // their prices belong to the old companies, so those ores start at their normal price.
            if (data.contains("prices." + o.id + ".change")) data.set("prices." + o.id, null);
            o.price = data.getDouble("prices." + o.id + ".price", o.base);
            for (double d : data.getDoubleList("prices." + o.id + ".history")) o.history.add(d);
            if (o.history.isEmpty()) o.history.add(o.price);
            while (o.history.size() > HISTORY) o.history.removeFirst();
            ores.put(o.id, o);
        }
    }

    private void storePrices() {
        for (Ore o : ores.values()) {
            data.set("prices." + o.id + ".price", round(o.price));
            data.set("prices." + o.id + ".history", new ArrayList<>(o.history));
        }
    }

    private void save() {
        dirty = false;
        try { data.save(dataFile); } catch (IOException e) { getLogger().warning("Could not save data.yml: " + e.getMessage()); }
    }

    /**
     * Version 2 traded made-up companies. Anything a player still holds that is not an ore now
     * is sold at its last price (no fee) and the money goes to the player.
     */
    private void migrateOldStocks() {
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) return;
        boolean changed = false;
        for (String key : players.getKeys(false)) {
            ConfigurationSection hold = data.getConfigurationSection("players." + key + ".holdings");
            if (hold == null) continue;
            for (String id : new ArrayList<>(hold.getKeys(false))) {
                // Old shares always have "shares"; ore holdings use "amount" (old ids can match an ore).
                if (ores.containsKey(id) && !hold.contains(id + ".shares")) continue;
                if (economy == null) {
                    getLogger().warning("Old stocks found but no economy yet - they will be paid out on the next start.");
                    return;
                }
                int shares = hold.getInt(id + ".shares", hold.getInt(id + ".amount"));
                double price = ores.containsKey(id) ? oldPrices.getOrDefault(id, 0.0) : data.getDouble("prices." + id + ".price", 0);
                double money = round(shares * price);
                UUID uuid;
                try { uuid = UUID.fromString(key); } catch (IllegalArgumentException e) { continue; }
                if (money > 0) {
                    economy.depositPlayer(Bukkit.getOfflinePlayer(uuid), money);
                    data.set("players." + key + ".refund", round(data.getDouble("players." + key + ".refund") + money));
                }
                getLogger().info("Paid out old stock " + id + " x" + shares + " = $" + MONEY.format(money)
                        + " to " + data.getString("players." + key + ".name", key));
                hold.set(id, null);
                changed = true;
            }
        }
        ConfigurationSection prices = data.getConfigurationSection("prices");
        if (prices != null) for (String id : prices.getKeys(false)) if (!ores.containsKey(id)) { prices.set(id, null); changed = true; }
        if (data.contains("news")) { data.set("news", null); changed = true; }
        if (changed) { storePrices(); save(); }
        for (Player p : Bukkit.getOnlinePlayers()) tellRefund(p);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(this, () -> tellRefund(e.getPlayer()), 60L);
    }

    private void tellRefund(Player p) {
        String path = "players." + p.getUniqueId() + ".refund";
        double money = data.getDouble(path);
        if (money <= 0 || !p.isOnline()) return;
        p.sendMessage(color(msg("refund").replace("%money%", MONEY.format(money))));
        data.set(path, null);
        dirty = true;
    }

    // ------------------------------------------------------------------ market

    private void tick() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double newsChance = getConfig().getDouble("news-chance", 1.0);
        for (Ore o : ores.values()) {
            double move = r.nextGaussian() * o.swing / 100.0;
            double next = o.price * (1 + move) + 0.02 * (o.base - o.price);
            o.price = clamp(o, next);
            push(o);
            if (r.nextDouble() * 100 < newsChance) news(o, r.nextBoolean());
        }
        dirty = true;
    }

    private void push(Ore o) {
        o.history.add(round(o.price));
        while (o.history.size() > HISTORY) o.history.removeFirst();
    }

    void news(Ore o, boolean up) {
        double before = o.price;
        double jump = 0.08 + ThreadLocalRandom.current().nextDouble() * 0.12;
        o.price = clamp(o, o.price * (up ? 1 + jump : 1 - jump));
        o.history.set(o.history.size() - 1, round(o.price));
        List<String> lines = up ? o.newsUp : o.newsDown;
        String headline = lines.isEmpty() ? (up ? "Prices are going up!" : "Prices are going down!")
                : lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
        double change = (o.price - before) / before * 100;
        Component c = color(msg("news").replace("%headline%", headline).replace("%ore%", o.name + "&r").replace("%change%", arrow(change)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(c);
            p.playSound(p.getLocation(), up ? Sound.BLOCK_NOTE_BLOCK_CHIME : Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, up ? 1.4f : 0.7f);
        }
        Bukkit.getConsoleSender().sendMessage(c);
        dirty = true;
    }

    static double clamp(Ore o, double v) {
        return round(Math.max(o.base * 0.25, Math.min(o.base * 4.0, v)));
    }

    double fee() { return getConfig().getDouble("fee-percent", 1.5) / 100.0; }

    /** Cost to buy {@code n}, fee included. */
    double buyCost(Ore o, int n) { return round(o.price * n * (1 + fee())); }

    /** Money for selling {@code n}, fee taken off. */
    double sellMoney(Ore o, int n) { return round(o.price * n * (1 - fee())); }

    int owned(UUID u, Ore o) { return data.getInt("players." + u + ".holdings." + o.id + ".amount"); }

    double paid(UUID u, Ore o) { return data.getDouble("players." + u + ".holdings." + o.id + ".cost"); }

    double worth(UUID u) {
        double w = 0;
        for (Ore o : ores.values()) w += owned(u, o) * o.price;
        return w;
    }

    double paidTotal(UUID u) {
        double w = 0;
        for (Ore o : ores.values()) w += paid(u, o);
        return w;
    }

    double balance(Player p) { return economy == null ? 0 : economy.getBalance(p); }

    void buy(Player p, Ore o, int n) {
        if (economy == null) { fail(p, msg("no-economy")); return; }
        UUID u = p.getUniqueId();
        int have = owned(u, o);
        n = Math.min(n, o.max - have);
        if (n <= 0) { fail(p, msg("max").replace("%max%", String.valueOf(o.max)).replace("%ore%", plain(o))); return; }
        double cost = buyCost(o, n);
        if (!economy.has(p, cost)) {
            // Buy as many as the player can afford instead of failing outright.
            int afford = (int) Math.floor(economy.getBalance(p) / (o.price * (1 + fee())));
            if (afford <= 0) { fail(p, msg("not-enough-money").replace("%cost%", MONEY.format(buyCost(o, 1)))); return; }
            n = Math.min(n, afford);
            cost = buyCost(o, n);
        }
        if (!economy.withdrawPlayer(p, cost).transactionSuccess()) { fail(p, msg("no-economy")); return; }
        String path = "players." + u + ".holdings." + o.id;
        data.set("players." + u + ".name", p.getName());
        data.set(path + ".amount", have + n);
        data.set(path + ".cost", round(paid(u, o) + cost));
        dirty = true;
        p.sendMessage(color(msg("bought").replace("%amount%", String.valueOf(n)).replace("%ore%", plain(o)).replace("%cost%", MONEY.format(cost))));
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
    }

    void sell(Player p, Ore o, int n) {
        if (economy == null) { fail(p, msg("no-economy")); return; }
        UUID u = p.getUniqueId();
        int have = owned(u, o);
        if (n < 0 || n > have) n = have;
        if (n <= 0) { fail(p, msg("not-enough").replace("%ore%", plain(o))); return; }
        double money = sellMoney(o, n);
        String path = "players." + u + ".holdings." + o.id;
        double cost = paid(u, o);
        if (have - n == 0) data.set(path, null);
        else {
            data.set(path + ".amount", have - n);
            data.set(path + ".cost", round(cost * (have - n) / have));
        }
        data.set("players." + u + ".name", p.getName());
        dirty = true;
        economy.depositPlayer(p, money);
        p.sendMessage(color(msg("sold").replace("%amount%", String.valueOf(n)).replace("%ore%", plain(o)).replace("%cost%", MONEY.format(money))));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.6f);
    }

    private void fail(Player p, String m) {
        p.sendMessage(color(m));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
    }

    /** Top traders by what their ores are worth now. */
    List<Map.Entry<String, Double>> top() {
        List<Map.Entry<String, Double>> list = new ArrayList<>();
        ConfigurationSection sec = data.getConfigurationSection("players");
        if (sec == null) return list;
        for (String key : sec.getKeys(false)) {
            try {
                double w = worth(UUID.fromString(key));
                if (w > 0) list.add(Map.entry(data.getString("players." + key + ".name", "?"), w));
            } catch (IllegalArgumentException ignored) { }
        }
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list;
    }

    // ------------------------------------------------------------------ command

    private boolean command(CommandSender sender, String[] args) {
        if (args.length >= 1 && sender.hasPermission("stocks.admin")) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "reload" -> {
                    storePrices();
                    reloadConfig();
                    loadOres();
                    hookEconomy();
                    startTasks();
                    sender.sendMessage(color(msg("reloaded")));
                    return true;
                }
                case "set" -> {
                    Ore o = args.length >= 3 ? ores.get(args[1].toLowerCase(Locale.ROOT)) : null;
                    if (o == null) { sender.sendMessage(color(args.length < 3 ? "&cUsage: /stocks set <ore> <price>" : msg("unknown-ore"))); return true; }
                    try {
                        o.price = clamp(o, Double.parseDouble(args[2]));
                        push(o);
                        dirty = true;
                        sender.sendMessage(color(msg("set").replace("%ore%", o.name).replace("%price%", MONEY.format(o.price))));
                    } catch (NumberFormatException e) { sender.sendMessage(color("&cThat is not a number.")); }
                    return true;
                }
                case "news" -> {
                    Ore o = args.length >= 3 ? ores.get(args[1].toLowerCase(Locale.ROOT)) : null;
                    if (o == null) { sender.sendMessage(color(args.length < 3 ? "&cUsage: /stocks news <ore> <up|down>" : msg("unknown-ore"))); return true; }
                    news(o, args[2].equalsIgnoreCase("up"));
                    return true;
                }
                default -> { }
            }
        }
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        menus.openMarket(p);
        return true;
    }

    // ------------------------------------------------------------------ text helpers

    String msg(String key) {
        return getConfig().getString("messages." + key, key).replace("%prefix%", getConfig().getString("messages.prefix", ""));
    }

    /** Ore name in its colour, without bold, for chat lines. */
    static String plain(Ore o) {
        return o.name.replace("&l", "");
    }

    static String arrow(double change) {
        if (change > 0.05) return GREEN + "▲ +" + PCT.format(change) + "%";
        if (change < -0.05) return RED + "▼ " + PCT.format(change) + "%";
        return "&7■ 0.0%";
    }

    static String profit(double v) {
        return (v >= 0 ? GREEN + "+$" : RED + "-$") + MONEY.format(Math.abs(v));
    }

    static Component color(String s) { return LEGACY.deserialize(s); }

    static Component line(String s) { return color(s).decoration(TextDecoration.ITALIC, false); }

    static double round(double d) { return Math.round(d * 100.0) / 100.0; }

    /** One ore of the market. */
    static final class Ore {
        String id, name, risk;
        Material material;
        double base, swing, price;
        int max;
        List<String> newsUp = List.of(), newsDown = List.of();
        final LinkedList<Double> history = new LinkedList<>();

        /** Change over the kept history (about the last hour), in percent. */
        double hourChange() {
            double first = history.getFirst();
            return first == 0 ? 0 : (price - first) / first * 100;
        }
    }
}
