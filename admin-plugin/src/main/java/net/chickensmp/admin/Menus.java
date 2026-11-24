package net.chickensmp.admin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;

/** The /admin GUI. */
public final class Menus {

    private final AdminPlugin plugin;

    public Menus(AdminPlugin plugin) { this.plugin = plugin; }

    private boolean perm(Player p, String node) {
        if (p.hasPermission("chickensmp.admin." + node)) return true;
        plugin.msg(p, "&#FC0000You don't have permission for that.");
        p.closeInventory();
        return false;
    }

    private ItemStack back() {
        return Text.item(Material.ARROW, Text.title(Text.RED, "Back"), Text.lore("Navigation", Text.RED, List.of("Go back to the", "previous menu."), Text.action(Text.RED, "CLICK", "to go back")));
    }

    private ItemStack close() {
        return Text.item(Material.BARRIER, Text.title(Text.RED, "Close"), Text.lore("Navigation", Text.RED, List.of("Close this menu."), Text.action(Text.RED, "CLICK", "to close")));
    }

    private ItemStack head(String name, String title, List<String> lore) {
        ItemStack it = Text.item(Material.PLAYER_HEAD, title, lore);
        if (it.getItemMeta() instanceof SkullMeta sm) {
            Player online = Bukkit.getPlayerExact(name);
            if (online != null) sm.setOwningPlayer(online);
            it.setItemMeta(sm);
        }
        return it;
    }

    private ItemStack targetHead(String target) {
        Player t = Bukkit.getPlayerExact(target);
        List<String> info = new ArrayList<>();
        info.add("Status: " + (t != null ? "&#00FC00Online" : "&#FC0000Offline"));
        var eco = plugin.economy();
        if (eco != null) {
            OfflinePlayer op = t != null ? t : Bukkit.getOfflinePlayerIfCached(target);
            if (op != null) info.add("Balance: &#00FC00$" + Text.compact(eco.getBalance(op)));
        }
        return head(target, Text.title(Text.YELLOW, target), Text.lore("Selected Player", Text.YELLOW, info));
    }

    // ---------------------------------------------------------------- main

    public void main(Player p) {
        Menu m = new Menu(5, "&8ᴀᴅᴍɪɴ ᴍᴇɴᴜ");
        m.set(4, head(p.getName(), Text.title(Text.RED, "Admin Menu"), Text.lore("ChickenSMP", Text.RED,
                List.of("Logged in as &#FCE300" + p.getName() + "&f.", "Every action is logged."))), null);
        m.set(11, Text.item(Material.NETHER_STAR, Text.title(Text.PURPLE, "Ranks"), Text.lore("Players", Text.PURPLE,
                List.of("Give or remove store ranks", "and staff ranks, forever", "or for a limited time."), Text.action(Text.PURPLE, "CLICK", "to open"))),
                (q, t) -> { if (perm(q, "ranks")) pickPlayer(q, "Ranks", true, this::ranks); });
        m.set(13, Text.item(Material.NETHERITE_PICKAXE, Text.title(Text.PINK, "Shard Tools"), Text.lore("Items", Text.PINK,
                List.of("3x3 Pickaxe, 3x3 Shovel,", "Tree Axe, Infinite Rocket", "and Sponge Bucket."), Text.action(Text.PINK, "CLICK", "to open"))),
                (q, t) -> { if (perm(q, "tools")) pickPlayer(q, "Shard Tools", false, this::tools); });
        m.set(15, Text.item(Material.SPAWNER, Text.title(Text.BLUE, "Spawners"), Text.lore("Items", Text.BLUE,
                List.of("Give stackable spawners", "of every mob type."), Text.action(Text.BLUE, "CLICK", "to open"))),
                (q, t) -> { if (perm(q, "spawners")) pickPlayer(q, "Spawners", false, this::spawners); });
        m.set(29, Text.item(Material.EMERALD, Text.title(Text.GREEN, "Money"), Text.lore("Economy", Text.GREEN,
                List.of("Give or take money."), Text.action(Text.GREEN, "CLICK", "to open"))),
                (q, t) -> { if (perm(q, "money")) pickPlayer(q, "Money", true, (a, b) -> amounts(a, b, false)); });
        m.set(31, Text.item(Material.AMETHYST_SHARD, Text.title(Text.PINK, "Shards"), Text.lore("Economy", Text.PINK,
                List.of("Give or take shards."), Text.action(Text.PINK, "CLICK", "to open"))),
                (q, t) -> { if (perm(q, "shards")) pickPlayer(q, "Shards", true, (a, b) -> amounts(a, b, true)); });
        m.set(33, Text.item(Material.TRIPWIRE_HOOK, Text.title(Text.YELLOW, "Key All"), Text.lore("Events", Text.YELLOW,
                List.of("Start a key all for", "everyone online."), Text.action(Text.YELLOW, "CLICK", "to open"))),
                (q, t) -> { if (perm(q, "keyall")) keyAll(q, 1); });
        m.set(40, close(), (q, t) -> q.closeInventory());
        m.fill();
        m.open(p);
    }

    // ---------------------------------------------------------------- player picker

    private void pickPlayer(Player p, String what, boolean allowOffline, BiConsumer<Player, String> next) { pickPlayer(p, what, allowOffline, next, 0); }

    private void pickPlayer(Player p, String what, boolean allowOffline, BiConsumer<Player, String> next, int page) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        int pages = Math.max(1, (online.size() + 44) / 45);
        int pg = Math.min(page, pages - 1);
        Menu m = new Menu(6, "&8" + Text.small(what) + " » ѕᴇʟᴇᴄᴛ ᴘʟᴀʏᴇʀ");
        for (int i = 0; i < 45; i++) {
            int idx = pg * 45 + i;
            if (idx >= online.size()) break;
            Player t = online.get(idx);
            String name = t.getName();
            m.set(i, head(name, Text.title(Text.YELLOW, name), Text.lore("Player", Text.YELLOW,
                    List.of("World: &#FCE300" + t.getWorld().getName()), Text.action(Text.YELLOW, "CLICK", "to select"))),
                    (q, c) -> next.accept(q, name));
        }
        m.set(45, back(), (q, c) -> main(q));
        if (pg > 0) m.set(48, Text.item(Material.ARROW, Text.title(Text.RED, "Previous Page"), List.of()), (q, c) -> pickPlayer(q, what, allowOffline, next, pg - 1));
        if (allowOffline) m.set(49, Text.item(Material.NAME_TAG, Text.title(Text.BLUE, "Enter a Name"), Text.lore("Offline Players", Text.BLUE,
                List.of("Type any player name", "in chat (also offline)."), Text.action(Text.BLUE, "CLICK", "to type"))),
                (q, c) -> plugin.ask(q, "Type the &#FCE300player name&f:", in -> {
                    if (!in.matches("[A-Za-z0-9_.]{2,17}")) { plugin.msg(q, "&#FC0000That is not a valid name."); return; }
                    next.accept(q, in);
                }));
        if (pg < pages - 1) m.set(50, Text.item(Material.ARROW, Text.title(Text.GREEN, "Next Page"), List.of()), (q, c) -> pickPlayer(q, what, allowOffline, next, pg + 1));
        m.set(53, close(), (q, c) -> q.closeInventory());
        m.fill();
        m.open(p);
    }

    // ---------------------------------------------------------------- confirm

    private void confirm(Player p, ItemStack what, Runnable yes, Runnable no) {
        Menu m = new Menu(3, "&8ᴄᴏɴꜰɪʀᴍ");
        m.set(13, what, null);
        m.set(11, Text.item(Material.LIME_STAINED_GLASS_PANE, Text.title(Text.GREEN, "Confirm"), Text.lore("Confirm", Text.GREEN, List.of("Do it."), Text.action(Text.GREEN, "CLICK", "to confirm"))),
                (q, c) -> { q.closeInventory(); yes.run(); });
        m.set(15, Text.item(Material.RED_STAINED_GLASS_PANE, Text.title(Text.RED, "Cancel"), Text.lore("Cancel", Text.RED, List.of("Go back."), Text.action(Text.RED, "CLICK", "to cancel"))),
                (q, c) -> no.run());
        m.fill();
        m.open(p);
    }

    // ---------------------------------------------------------------- ranks

    private record Rank(String group, String name, String color, Material icon, String category) {}

    private List<Rank> ranks(String category) {
        List<Rank> out = new ArrayList<>();
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("ranks." + category);
        if (s == null) return out;
        for (String g : s.getKeys(false)) {
            ConfigurationSection c = s.getConfigurationSection(g);
            if (c == null) continue;
            Material icon = Material.matchMaterial(c.getString("icon", "PAPER"));
            out.add(new Rank(c.getString("group", g), c.getString("name", g), c.getString("color", Text.BLUE), icon == null ? Material.PAPER : icon, category));
        }
        return out;
    }

    private void ranks(Player p, String target) {
        Menu m = new Menu(6, "&8ʀᴀɴᴋѕ » " + target);
        m.set(4, targetHead(target), null);
        Player t = Bukkit.getPlayerExact(target);
        String[][] rows = {{"store", "Store Rank"}, {"staff", "Staff Rank"}, {"other", "Special Rank"}};
        int row = 1;
        for (String[] r : rows) {
            List<Rank> list = ranks(r[0]);
            if (list.isEmpty()) continue;
            int[] slots = Menu.centered(row, Math.min(9, list.size()));
            for (int i = 0; i < slots.length; i++) {
                Rank rank = list.get(i);
                boolean has = t != null && t.hasPermission("group." + rank.group());
                List<String> info = new ArrayList<>();
                info.add("Group: &#FCE300" + rank.group());
                if (t != null) info.add("Has it: " + (has ? "&#00FC00Yes" : "&#FC0000No"));
                ItemStack it = Text.glow(Text.item(rank.icon(), Text.title(rank.color(), rank.name()), Text.lore(r[1], rank.color(), info,
                        Text.action(Text.GREEN, "LEFT-CLICK", "to give"), Text.action(Text.RED, "RIGHT-CLICK", "to remove"))), has);
                m.set(slots[i], it, (q, c) -> {
                    if (rank.category().equals("staff") && !perm(q, "ranks.staff")) return;
                    if (c.isRightClick()) removeRank(q, target, rank);
                    else durations(q, target, rank);
                });
            }
            row += 1;
        }
        m.set(45, back(), (q, c) -> pickPlayer(q, "Ranks", true, this::ranks));
        m.set(53, close(), (q, c) -> q.closeInventory());
        m.fill();
        m.open(p);
    }

    private void durations(Player p, String target, Rank rank) {
        Menu m = new Menu(4, "&8" + Text.small(rank.name()) + " » " + target);
        m.set(4, Text.item(rank.icon(), Text.title(rank.color(), rank.name()), Text.lore("Choose a Duration", rank.color(),
                List.of("For &#FCE300" + target + "&f."))), null);
        List<String> durs = plugin.getConfig().getStringList("rank-durations");
        if (durs.isEmpty()) durs = List.of("permanent", "1h", "1d", "7d", "30d", "90d");
        List<String> all = new ArrayList<>(durs);
        all.add("custom");
        int[] slots = Menu.centered(1, Math.min(9, all.size()));
        for (int i = 0; i < slots.length; i++) {
            String d = all.get(i);
            boolean perm = d.equalsIgnoreCase("permanent");
            boolean custom = d.equalsIgnoreCase("custom");
            Material icon = perm ? Material.NETHER_STAR : custom ? Material.NAME_TAG : Material.CLOCK;
            String color = perm ? Text.PURPLE : custom ? Text.BLUE : Text.YELLOW;
            String label = perm ? "Forever" : custom ? "Custom Time" : pretty(d);
            m.set(slots[i], Text.item(icon, Text.title(color, label), Text.lore("Duration", color,
                    List.of(perm ? "The rank never expires." : custom ? "Type any time in chat." : "Expires after &#FCE300" + pretty(d) + "&f."), Text.action(color, "CLICK", "to choose"))),
                    (q, c) -> {
                        if (custom) {
                            plugin.ask(q, "Type the time, e.g. &#FCE30012h&f, &#FCE30014d&f, &#FCE3002w&f, &#FCE3006mo&f:", in -> {
                                String x = in.toLowerCase(Locale.ROOT).replace(" ", "");
                                if (!x.matches("(\\d+(mo|[smhdwy]))+")) { plugin.msg(q, "&#FC0000Invalid time."); return; }
                                giveRank(q, target, rank, x);
                            });
                        } else giveRank(q, target, rank, perm ? null : d);
                    });
        }
        m.set(27, back(), (q, c) -> ranks(q, target));
        m.set(35, close(), (q, c) -> q.closeInventory());
        m.fill();
        m.open(p);
    }

    private static String pretty(String d) {
        String n = d.replaceAll("[^0-9]", "");
        String u = d.replaceAll("[0-9]", "");
        String word = switch (u) {
            case "s" -> "second"; case "m" -> "minute"; case "h" -> "hour"; case "d" -> "day";
            case "w" -> "week"; case "mo" -> "month"; case "y" -> "year"; default -> u;
        };
        return n + " " + word + ("1".equals(n) ? "" : "s");
    }

    private void giveRank(Player p, String target, Rank rank, String duration) {
        String when = duration == null ? "forever" : "for " + pretty(duration);
        ItemStack what = Text.item(rank.icon(), Text.title(rank.color(), rank.name()), Text.lore("Give Rank", rank.color(),
                List.of("Player: &#FCE300" + target, "Rank: &#" + rank.color() + rank.name(), "Time: &#FCE300" + when)));
        confirm(p, what, () -> {
            if (rank.category().equals("staff") && !p.hasPermission("chickensmp.admin.ranks.staff")) return;
            String cmd = duration == null
                    ? "lp user " + target + " parent add " + rank.group()
                    : "lp user " + target + " parent addtemp " + rank.group() + " " + duration + " accumulate";
            plugin.console(p, cmd);
            plugin.msg(p, "&fGave &#" + rank.color() + rank.name() + " &fto &#FCE300" + target + " &f" + when + ".");
            Player t = Bukkit.getPlayerExact(target);
            if (t != null && t != p) plugin.msg(t, "&fYou got the &#" + rank.color() + rank.name() + " &frank " + when + "!");
        }, () -> durations(p, target, rank));
    }

    private void removeRank(Player p, String target, Rank rank) {
        ItemStack what = Text.item(rank.icon(), Text.title(Text.RED, "Remove " + rank.name()), Text.lore("Remove Rank", Text.RED,
                List.of("Player: &#FCE300" + target, "Rank: &#" + rank.color() + rank.name(), "Removes the permanent", "and the timed rank.")));
        confirm(p, what, () -> {
            plugin.console(p, "lp user " + target + " parent remove " + rank.group());
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.console(p, "lp user " + target + " parent removetemp " + rank.group()), 30L);
            plugin.msg(p, "&fRemoved &#" + rank.color() + rank.name() + " &ffrom &#FCE300" + target + "&f.");
        }, () -> ranks(p, target));
    }

    // ---------------------------------------------------------------- tools

    private void tools(Player p, String target) {
        Menu m = new Menu(3, "&8ѕʜᴀʀᴅ ᴛᴏᴏʟѕ » " + target);
        m.set(4, targetHead(target), null);
        int[] slots = Menu.centered(1, Tools.IDS.size());
        for (int i = 0; i < slots.length; i++) {
            String id = Tools.IDS.get(i);
            ItemStack it = plugin.tools().create(id);
            if (it == null) continue;
            var meta = it.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
            lore.add(Text.c(""));
            lore.add(Text.c(Text.action(Text.GREEN, "CLICK", "to give 1")));
            meta.lore(lore);
            it.setItemMeta(meta);
            m.set(slots[i], it, (q, c) -> {
                if (!perm(q, "tools")) return;
                Player t = Bukkit.getPlayerExact(target);
                if (t == null) { plugin.msg(q, "&#FCE300" + target + " &#FC0000is not online."); return; }
                plugin.give(t, plugin.tools().create(id));
                plugin.log(q.getName() + " gave tool " + id + " to " + t.getName());
                plugin.msg(q, "&fGave a " + plugin.tools().name(id) + " &fto &#FCE300" + t.getName() + "&f.");
                if (t != q) plugin.msg(t, "&fYou got a " + plugin.tools().name(id) + "&f!");
            });
        }
        m.set(18, back(), (q, c) -> pickPlayer(q, "Shard Tools", false, this::tools));
        m.set(26, close(), (q, c) -> q.closeInventory());
        m.fill();
        m.open(p);
    }

    // ---------------------------------------------------------------- spawners

    private void spawners(Player p, String target) {
        Menu m = new Menu(6, "&8ѕᴘᴀᴡɴᴇʀѕ » " + target);
        int i = 0;
        for (Spawners.Mob mob : plugin.spawners().mobs()) {
            if (i >= 45) break;
            String id = mob.id();
            m.set(i++, Text.item(mob.icon(), Text.title(mob.color(), mob.name() + " Spawner"), Text.lore("Spawner", mob.color(),
                    List.of("Give &#FCE300" + mob.name() + " &fspawners", "to &#FCE300" + target + "&f."),
                    Text.action(Text.GREEN, "LEFT-CLICK", "to give 1"), Text.action(Text.YELLOW, "RIGHT-CLICK", "to give 16"),
                    Text.action(Text.BLUE, "SHIFT-CLICK", "to type an amount"))),
                    (q, c) -> {
                        if (c == ClickType.SHIFT_LEFT || c == ClickType.SHIFT_RIGHT) {
                            plugin.ask(q, "How many &#FCE300" + mob.name() + " &fspawners?", in -> {
                                double n = Text.parseAmount(in);
                                if (n < 1 || n > 2304) { plugin.msg(q, "&#FC0000Type a number from 1 to 2304."); return; }
                                giveSpawners(q, target, id, (int) n);
                                spawners(q, target);
                            });
                        } else giveSpawners(q, target, id, c.isRightClick() ? 16 : 1);
                    });
        }
        m.set(45, back(), (q, c) -> pickPlayer(q, "Spawners", false, this::spawners));
        m.set(49, targetHead(target), null);
        m.set(53, close(), (q, c) -> q.closeInventory());
        m.fill();
        m.open(p);
    }

    private void giveSpawners(Player q, String target, String mob, int amount) {
        if (!q.hasPermission("chickensmp.admin.spawners")) return;
        Player t = Bukkit.getPlayerExact(target);
        if (t == null) { plugin.msg(q, "&#FCE300" + target + " &#FC0000is not online."); return; }
        plugin.giveSpawners(t, mob, amount);
        Spawners.Mob m = plugin.spawners().mob(mob);
        plugin.log(q.getName() + " gave " + amount + "x " + mob + " spawner to " + t.getName());
        plugin.msg(q, "&fGave &#FCE300" + amount + "x &#" + m.color() + m.name() + " Spawner &fto &#FCE300" + t.getName() + "&f.");
        if (t != q) plugin.msg(t, "&fYou got &#FCE300" + amount + "x &#" + m.color() + m.name() + " Spawner&f!");
    }

    // ---------------------------------------------------------------- money / shards

    private void amounts(Player p, String target, boolean shards) {
        String what = shards ? "Shards" : "Money";
        String color = shards ? Text.PINK : Text.GREEN;
        String unit = shards ? "" : "$";
        Menu m = new Menu(4, "&8" + Text.small(what) + " » " + target);
        m.set(4, targetHead(target), null);
        List<Double> list = new ArrayList<>();
        for (String s : plugin.getConfig().getStringList(shards ? "amounts.shards" : "amounts.money")) {
            double d = Text.parseAmount(s);
            if (d > 0) list.add(d);
        }
        int[] slots = Menu.centered(1, Math.min(8, list.size()) + 1);
        Material icon = shards ? Material.AMETHYST_SHARD : Material.EMERALD;
        for (int i = 0; i < slots.length - 1; i++) {
            double amount = list.get(i);
            String label = unit + Text.compact(amount) + (shards ? " Shards" : "");
            ItemStack it = Text.item(icon, Text.title(color, label), Text.lore(what, color, List.of("Amount: &#FCE300" + unit + Text.num(amount)),
                    Text.action(Text.GREEN, "LEFT-CLICK", "to give"), Text.action(Text.RED, "RIGHT-CLICK", "to take")));
            it.setAmount(Math.min(64, i + 1));
            m.set(slots[i], it, (q, c) -> changeAmount(q, target, shards, amount, c.isRightClick()));
        }
        m.set(slots[slots.length - 1], Text.item(Material.NAME_TAG, Text.title(Text.BLUE, "Custom Amount"), Text.lore(what, Text.BLUE,
                List.of("Type any amount in chat,", "e.g. &#FCE300250k &for &#FCE3001.5m&f."),
                Text.action(Text.GREEN, "LEFT-CLICK", "to give"), Text.action(Text.RED, "RIGHT-CLICK", "to take"))),
                (q, c) -> {
                    boolean take = c.isRightClick();
                    plugin.ask(q, "How much " + (shards ? "&#FC00E3shards" : "&#00FC00money") + " &fto " + (take ? "&#FC0000take from" : "&#00FC00give to") + " &#FCE300" + target + "&f?", in -> {
                        double d = Text.parseAmount(in);
                        if (d <= 0) { plugin.msg(q, "&#FC0000That is not a valid amount."); return; }
                        changeAmount(q, target, shards, d, take);
                    });
                });
        m.set(27, back(), (q, c) -> pickPlayer(q, what, true, (a, b) -> amounts(a, b, shards)));
        m.set(35, close(), (q, c) -> q.closeInventory());
        m.fill();
        m.open(p);
    }

    private void changeAmount(Player p, String target, boolean shards, double amount, boolean take) {
        String what = shards ? "Shards" : "Money";
        String color = shards ? Text.PINK : Text.GREEN;
        String unit = shards ? "" : "$";
        String amt = shards ? String.valueOf((long) Math.floor(amount)) : (amount == Math.floor(amount) ? String.valueOf((long) amount) : String.valueOf(amount));
        double big = plugin.getConfig().getDouble(shards ? "confirm-above.shards" : "confirm-above.money", shards ? 1000 : 10_000_000);
        Runnable run = () -> {
            if (!p.hasPermission(shards ? "chickensmp.admin.shards" : "chickensmp.admin.money")) return;
            plugin.console(p, (shards ? "shardmanager " : "moneymanager ") + (take ? "remove " : "add ") + target + " " + amt);
            plugin.msg(p, (take ? "&fTook &#FC0000" : "&fGave &#" + color) + unit + Text.num(Double.parseDouble(amt)) + (shards ? " shards" : "")
                    + (take ? " &ffrom &#FCE300" : " &fto &#FCE300") + target + "&f.");
        };
        if (amount >= big || take) {
            ItemStack it = Text.item(shards ? Material.AMETHYST_SHARD : Material.EMERALD, Text.title(take ? Text.RED : color, (take ? "Take " : "Give ") + what),
                    Text.lore(what, take ? Text.RED : color, List.of("Player: &#FCE300" + target, "Amount: &#FCE300" + unit + Text.num(Double.parseDouble(amt)))));
            confirm(p, it, run, () -> amounts(p, target, shards));
        } else {
            run.run();
            amounts(p, target, shards);
        }
    }

    // ---------------------------------------------------------------- key all

    private void keyAll(Player p, int amount) {
        Menu m = new Menu(4, "&8ᴋᴇʏ ᴀʟʟ");
        File f = new File(plugin.getDataFolder().getParentFile(), "ChickenSMPKeyAll/config.yml");
        List<String[]> keys = new ArrayList<>();
        if (f.exists()) {
            ConfigurationSection ks = YamlConfiguration.loadConfiguration(f).getConfigurationSection("keys");
            if (ks != null) for (String k : ks.getKeys(false)) {
                String col = ks.getString(k + ".color", "&7");
                String hex = col.startsWith("&#") ? col.substring(2, Math.min(8, col.length())) : "BFBFBF";
                keys.add(new String[]{k, ks.getString(k + ".name", k), hex, String.valueOf(ks.getDouble(k + ".chance"))});
            }
        }
        keys.add(new String[]{"random", "Random Key", Text.YELLOW, ""});
        int[] slots = Menu.centered(1, Math.min(9, keys.size()));
        for (int i = 0; i < slots.length; i++) {
            String[] k = keys.get(i);
            List<String> info = new ArrayList<>();
            info.add("Everyone online gets");
            info.add("&#FCE300" + amount + "x " + k[1] + "&f.");
            if (!k[3].isEmpty()) info.add("Random chance: &#FCE300" + k[3].replace(".0", "") + "%");
            Material icon = k[0].equals("random") ? Material.ENDER_EYE : Material.TRIPWIRE_HOOK;
            ItemStack it = Text.item(icon, Text.title(k[2], k[1]), Text.lore("Key All", k[2], info, Text.action(k[2], "CLICK", "to start")));
            it.setAmount(Math.max(1, Math.min(64, amount)));
            m.set(slots[i], it, (q, c) -> {
                ItemStack what = Text.item(icon, Text.title(k[2], "Key All: " + k[1]), Text.lore("Key All", k[2],
                        List.of("Amount: &#FCE300" + amount + "x", "Players online: &#FCE300" + Bukkit.getOnlinePlayers().size())));
                confirm(q, what, () -> {
                    if (!q.hasPermission("chickensmp.admin.keyall")) return;
                    plugin.console(q, "ckeyall " + k[0] + " " + amount);
                    plugin.msg(q, "&fStarted a key all: &#" + k[2] + amount + "x " + k[1] + "&f.");
                }, () -> keyAll(q, amount));
            });
        }
        int[] steps = {1, 2, 3, 5, 10};
        int idx = 0;
        for (int i = 0; i < steps.length; i++) if (steps[i] == amount) idx = i;
        int nextAmount = steps[(idx + 1) % steps.length];
        ItemStack am = Text.item(Material.HOPPER, Text.title(Text.BLUE, "Amount: " + amount + "x"), Text.lore("Settings", Text.BLUE,
                List.of("How many keys each", "player gets."), Text.action(Text.BLUE, "CLICK", "to change to " + nextAmount + "x")));
        am.setAmount(amount);
        m.set(31, am, (q, c) -> keyAll(q, nextAmount));
        m.set(27, back(), (q, c) -> main(q));
        m.set(35, close(), (q, c) -> q.closeInventory());
        m.fill();
        m.open(p);
    }
}
