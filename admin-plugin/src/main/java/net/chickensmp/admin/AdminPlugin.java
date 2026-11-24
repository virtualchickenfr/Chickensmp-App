package net.chickensmp.admin;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class AdminPlugin extends JavaPlugin implements Listener, TabExecutor {

    private static AdminPlugin instance;
    public static AdminPlugin get() { return instance; }

    private Tools tools;
    private Spawners spawners;
    private Menus menus;

    private final Map<UUID, Consumer<String>> asks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> swallow = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        if (!new File(getDataFolder(), "mobs.yml").exists()) saveResource("mobs.yml", false);
        tools = new Tools(this);
        spawners = new Spawners(this);
        menus = new Menus(this);
        var pm = getServer().getPluginManager();
        pm.registerEvents(new Menu.Listen(), this);
        pm.registerEvents(this, this);
        pm.registerEvents(tools, this);
        pm.registerEvents(spawners, this);
        var cmd = getCommand("admin");
        if (cmd != null) { cmd.setExecutor(this); cmd.setTabCompleter(this); }
        spawners.enable();
        getLogger().info("ChickenSMPAdmin enabled: " + spawners.mobIds().size() + " spawner types, " + spawners.count() + " spawners placed.");
    }

    @Override
    public void onDisable() {
        if (spawners != null) spawners.disable();
    }

    public Tools tools() { return tools; }
    public Spawners spawners() { return spawners; }


    // ---------------------------------------------------------------- messages / log

    public String prefix() { return getConfig().getString("messages.prefix", "&#FC0000&lADMIN &r&7» &f"); }

    public void msg(CommandSender to, String text) { to.sendMessage(Text.c(prefix() + text)); }

    /** Runs a console command and writes it to the admin log. */
    public boolean console(CommandSender by, String command) {
        log((by == null ? "CONSOLE" : by.getName()) + " ran: /" + command);
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    public void log(String line) {
        getLogger().info("[Log] " + line);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        File f = new File(getDataFolder(), "log.txt");
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            synchronized (AdminPlugin.class) {
                try (PrintWriter w = new PrintWriter(new FileWriter(f, true))) {
                    w.println("[" + stamp + "] " + line);
                } catch (IOException ignored) {}
            }
        });
    }

    // ---------------------------------------------------------------- chat input

    /** Closes the menu and asks the player to type something in chat ("cancel" aborts). */
    public void ask(Player p, String question, Consumer<String> answer) {
        p.closeInventory();
        asks.put(p.getUniqueId(), answer);
        msg(p, question);
        msg(p, "&7Type &#FC0000cancel &7to abort.");
        UUID id = p.getUniqueId();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (asks.remove(id, answer)) {
                Player q = Bukkit.getPlayer(id);
                if (q != null) msg(q, "&#FC0000Input timed out.");
            }
        }, 20L * 60);
    }

    private boolean takeChat(Player p, String text) {
        UUID id = p.getUniqueId();
        Long sw = swallow.get(id);
        if (sw != null && System.currentTimeMillis() - sw < 3000) return true;
        Consumer<String> cb = asks.remove(id);
        if (cb == null) return false;
        swallow.put(id, System.currentTimeMillis());
        Bukkit.getScheduler().runTask(this, () -> {
            if (text.trim().equalsIgnoreCase("cancel")) { msg(p, "&#FC0000Cancelled."); return; }
            cb.accept(text.trim());
        });
        return true;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLegacyChat(AsyncPlayerChatEvent e) {
        if (asks.containsKey(e.getPlayer().getUniqueId()) || swallow.containsKey(e.getPlayer().getUniqueId())) {
            if (takeChat(e.getPlayer(), e.getMessage())) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (asks.containsKey(id) || swallow.containsKey(id)) {
            String text = PlainTextComponentSerializer.plainText().serialize(e.message());
            if (takeChat(e.getPlayer(), text)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        asks.remove(e.getPlayer().getUniqueId());
        swallow.remove(e.getPlayer().getUniqueId());
    }

    // ---------------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) {
            if (!(s instanceof Player p)) { msg(s, "Usage: /" + label + " give <player> <tool|spawner> <id> [amount] | reload"); return true; }
            if (!p.hasPermission("chickensmp.admin.use")) { msg(p, "&#FC0000You don't have permission."); return true; }
            menus.main(p);
            return true;
        }
        if (a[0].equalsIgnoreCase("reload")) {
            if (!s.hasPermission("chickensmp.admin.reload")) { msg(s, "&#FC0000You don't have permission."); return true; }
            reloadConfig();
            spawners.reload();
            msg(s, "&#00FC00Reloaded. &fSpawner types: &#FCE300" + spawners.mobIds().size());
            return true;
        }
        if (a[0].equalsIgnoreCase("give") && a.length >= 4) {
            Player t = Bukkit.getPlayerExact(a[1]);
            if (t == null) { msg(s, "&#FC0000Player &#FCE300" + a[1] + " &#FC0000is not online."); return true; }
            int amount = 1;
            if (a.length >= 5) try { amount = Math.max(1, Math.min(2304, Integer.parseInt(a[4]))); } catch (NumberFormatException ignored) {}
            if (a[2].equalsIgnoreCase("tool")) {
                if (!s.hasPermission("chickensmp.admin.tools")) { msg(s, "&#FC0000You don't have permission."); return true; }
                ItemStack it = tools.create(a[3].toLowerCase(Locale.ROOT));
                if (it == null) { msg(s, "&#FC0000Unknown tool. &fTools: &#FCE300" + String.join(", ", Tools.IDS)); return true; }
                for (int i = 0; i < amount; i++) give(t, it.clone());
                log(s.getName() + " gave " + amount + "x tool " + a[3] + " to " + t.getName());
                msg(s, "&fGave &#FCE300" + amount + "x " + a[3] + " &fto &#FCE300" + t.getName() + "&f.");
                return true;
            }
            if (a[2].equalsIgnoreCase("spawner")) {
                if (!s.hasPermission("chickensmp.admin.spawners")) { msg(s, "&#FC0000You don't have permission."); return true; }
                String mob = a[3].toLowerCase(Locale.ROOT);
                if (!spawners.mobIds().contains(mob)) { msg(s, "&#FC0000Unknown spawner. &fTypes: &#FCE300" + String.join(", ", spawners.mobIds())); return true; }
                giveSpawners(t, mob, amount);
                log(s.getName() + " gave " + amount + "x " + mob + " spawner to " + t.getName());
                msg(s, "&fGave &#FCE300" + amount + "x " + mob + " spawner &fto &#FCE300" + t.getName() + "&f.");
                return true;
            }
        }
        msg(s, "Usage: /" + label + " [give <player> <tool|spawner> <id> [amount] | reload]");
        return true;
    }

    public void giveSpawners(Player t, String mob, int amount) {
        while (amount > 0) {
            int n = Math.min(64, amount);
            ItemStack it = spawners.item(mob);
            it.setAmount(n);
            give(t, it);
            amount -= n;
        }
    }

    /** Adds to the inventory, drops what doesn't fit at the player's feet. */
    public void give(Player p, ItemStack it) {
        for (ItemStack left : p.getInventory().addItem(it).values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) out.addAll(List.of("give", "reload"));
        else if (a.length == 2 && a[0].equalsIgnoreCase("give")) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        else if (a.length == 3 && a[0].equalsIgnoreCase("give")) out.addAll(List.of("tool", "spawner"));
        else if (a.length == 4 && a[2].equalsIgnoreCase("tool")) out.addAll(Tools.IDS);
        else if (a.length == 4 && a[2].equalsIgnoreCase("spawner")) out.addAll(spawners.mobIds());
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
