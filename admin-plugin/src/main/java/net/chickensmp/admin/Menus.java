package net.chickensmp.admin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;

/** The /admin GUI (EconomyCore look: framed menus, CATEGORY / Information / ▶ CLICK lore). */
public final class Menus {

    private final AdminPlugin plugin;

    public Menus(AdminPlugin plugin) { this.plugin = plugin; }

    // ---------------------------------------------------------------- helpers

    private boolean perm(Player p, String node) {
        if (p.hasPermission("chickensmp.admin." + node)) return true;
        plugin.msg(p, "&#FC0000You don't have permission for that.");
        bad(p);
        return false;
    }

    static void good(Player p) { p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.3f); }
    static void bad(Player p) { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f); }

    private static ItemStack button(Material m, String color, String name, String category, List<String> info, String... actions) {
        return Text.item(m, Text.title(color, name), Text.lore(category, color, info, actions));
    }

    /** Back (bottom left), close (bottom middle). */
    private void nav(Menu m, Menu.Click back) {
        int last = m.size() - 9;
        if (back != null) m.set(last, button(Material.ARROW, Text.RED, "Back", "Navigation", List.of("Go back to the", "previous menu."),
                Text.action(Text.RED, "CLICK", "to Go Back")), back);
        m.set(last + 4, button(Material.BARRIER, Text.RED, "Close", "Navigation", List.of("Close this menu."),
                Text.action(Text.RED, "CLICK", "to Close")), (q, c) -> q.closeInventory());
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

    /** Highest configured rank the player has (staff first, then special, then store). */
    private Rank topRank(Player t) {
        for (String cat : List.of("staff", "other", "store")) {
            List<Rank> list = new ArrayList<>(ranks(cat));
            Collections.reverse(list);
            for (Rank r : list) if (t.hasPermission("group." + r.group())) return r;
        }
        return null;
    }

    private List<String> playerInfo(String target) {
        Player t = Bukkit.getPlayerExact(target);
        List<String> info = new ArrayList<>();
        info.add("Status: " + (t != null ? "&#00FC00Online" : "&#FC0000Offline"));
        if (t != null) {
            Rank r = topRank(t);
            info.add("Rank: " + (r == null ? "&#AAAAAAMember" : "&#" + Text.last(r.colors()) + r.name()));
        }
        if (Eco.available()) {
            OfflinePlayer op = t != null ? t : Bukkit.getOfflinePlayerIfCached(target);
            if (op != null) info.add("Balance: &#00FC00$" + Text.compact(Eco.balance(op)));
        }
        if (t != null) {
            info.add("World: &#FCE300" + t.getWorld().getName());
            info.add("Ping: &#FCE300" + t.getPing() + "ms");
        }
        return info;
    }

    private ItemStack targetHead(String target) {
        return head(target, Text.title(Text.YELLOW, target), Text.lore("Selected Player", Text.YELLOW, playerInfo(target)));
    }

    // ---------------------------------------------------------------- main

    public void main(Player p) {
        Menu m = new Menu(5, "&8ᴀᴅᴍɪɴ ᴍᴇɴᴜ");
        int staffOnline = 0;
        for (Player o : Bukkit.getOnlinePlayers()) if (o.hasPermission("group.jr.helper")) staffOnline++;
        m.set(4, head(p.getName(), Text.title(Text.RED, "Admin Menu"), Text.lore("ChickenSMP", Text.RED, List.of(
                "Players online: &#FCE300" + Bukkit.getOnlinePlayers().size(),
                "Staff online: &#FCE300" + staffOnline,
                "Spawners placed: &#FCE300" + plugin.spawners().count(),
                "", "&7Every action is logged."))), null);
        m.set(20, button(Material.NETHER_STAR, Text.PURPLE, "Ranks", "Players", List.of("Give or remove store,", "staff and special ranks,", "forever or for a time."),
                Text.action(Text.PURPLE, "CLICK", "to Open")), (q, t) -> { if (perm(q, "ranks")) pickPlayer(q, "Ranks", true, this::ranks); });
        m.set(21, button(Material.NETHERITE_PICKAXE, Text.BLUE, "Shard Tools", "Items", List.of("3x3 Pickaxe, 3x3 Shovel,", "Tree Axe, Infinite Rocket", "and Sponge Bucket."),
                Text.action(Text.BLUE, "CLICK", "to Open")), (q, t) -> { if (perm(q, "tools")) pickPlayer(q, "Shard Tools", false, this::tools); });
        m.set(22, button(Material.SPAWNER, Text.ORANGE, "Spawners", "Items", List.of("Stackable spawners of", "&#FCE300" + plugin.spawners().mobIds().size() + " &fmob types."),
                Text.action(Text.ORANGE, "CLICK", "to Open")), (q, t) -> { if (perm(q, "spawners")) pickPlayer(q, "Spawners", false, this::spawners); });
        m.set(23, button(Material.EMERALD, Text.GREEN, "Money", "Economy", List.of("Give or take money", "from any player."),
                Text.action(Text.GREEN, "CLICK", "to Open")), (q, t) -> { if (perm(q, "money")) pickPlayer(q, "Money", true, (a, b) -> amounts(a, b, false)); });
        m.set(24, button(Material.AMETHYST_SHARD, Text.PURPLE, "Shards", "Economy", List.of("Give or take shards", "from any player."),
                Text.action(Text.PURPLE, "CLICK", "to Open")), (q, t) -> { if (perm(q, "shards")) pickPlayer(q, "Shards", true, (a, b) -> amounts(a, b, true)); });
        String timer = Papi.apply(p, "%keyall_timer%");
        List<String> kinfo = new ArrayList<>(List.of("Start a key all for", "everyone online."));
        if (!timer.contains("%")) kinfo.add("Next automatic: &#FCE300" + timer);
        m.set(31, button(Material.TRIPWIRE_HOOK, Text.YELLOW, "Key All", "Events", kinfo, Text.action(Text.YELLOW, "CLICK", "to Open")),
                (q, t) -> { if (perm(q, "keyall")) keyAll(q, 1); });
        nav(m, null);
        m.frame();
        m.open(p);
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.4f);
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
            String name = online.get(idx).getName();
            m.set(i, head(name, Text.title(Text.YELLOW, name), Text.lore("Player", Text.YELLOW, playerInfo(name), Text.action(Text.YELLOW, "CLICK", "to Select"))),
                    (q, c) -> next.accept(q, name));
        }
        nav(m, (q, c) -> main(q));
        if (pg > 0) m.set(48, button(Material.ARROW, Text.RED, "Previous Page", "Navigation", List.of("Page &#FCE300" + pg + " &fof &#FCE300" + pages + "&f."),
                Text.action(Text.RED, "CLICK", "to Go Back")), (q, c) -> pickPlayer(q, what, allowOffline, next, pg - 1));
        if (pg < pages - 1) m.set(50, button(Material.ARROW, Text.GREEN, "Next Page", "Navigation", List.of("Page &#FCE300" + (pg + 2) + " &fof &#FCE300" + pages + "&f."),
                Text.action(Text.GREEN, "CLICK", "to Go Forward")), (q, c) -> pickPlayer(q, what, allowOffline, next, pg + 1));
        if (allowOffline) m.set(53, button(Material.NAME_TAG, Text.BLUE, "Enter a Name", "Offline Players", List.of("Type any player name", "in chat (also offline)."),
                Text.action(Text.BLUE, "CLICK", "to Type")),
                (q, c) -> plugin.ask(q, "Type the &#FCE300player name&f:", in -> {
                    if (!in.matches("[A-Za-z0-9_.]{2,17}")) { plugin.msg(q, "&#FC0000That is not a valid name."); bad(q); return; }
                    next.accept(q, in);
                }));
        m.bar();
        m.open(p);
    }

    // ---------------------------------------------------------------- confirm

    private void confirm(Player p, ItemStack what, Runnable yes, Runnable no) {
        Menu m = new Menu(3, "&8ᴄᴏɴꜰɪʀᴍ");
        m.set(13, what, null);
        ItemStack ok = button(Material.LIME_STAINED_GLASS_PANE, Text.GREEN, "Confirm", "Confirm", List.of("Do it."), Text.action(Text.GREEN, "CLICK", "to Confirm"));
        ItemStack no2 = button(Material.RED_STAINED_GLASS_PANE, Text.RED, "Cancel", "Cancel", List.of("Go back without", "changing anything."), Text.action(Text.RED, "CLICK", "to Cancel"));
        for (int s : new int[]{10, 11, 12}) m.set(s, ok, (q, c) -> { q.closeInventory(); yes.run(); });
        for (int s : new int[]{14, 15, 16}) m.set(s, no2, (q, c) -> no.run());
        m.frame();
        m.open(p);
    }

    // ---------------------------------------------------------------- ranks

    private record Rank(String group, String name, String colors, String icon, Material material, String category) {}

    private List<Rank> ranks(String category) {
        List<Rank> out = new ArrayList<>();
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("ranks." + category);
        if (s == null) return out;
        for (String g : s.getKeys(false)) {
            ConfigurationSection c = s.getConfigurationSection(g);
            if (c == null) continue;
            Material mat = Material.matchMaterial(c.getString("item", c.getString("icon", "PAPER")));
            out.add(new Rank(c.getString("group", g), c.getString("name", g), c.getString("colors", c.getString("color", Text.BLUE)),
                    c.getString("symbol", ""), mat == null ? Material.PAPER : mat, category));
        }
        return out;
    }

    private String rankTitle(Rank r) {
        return Text.gradient(r.name().toUpperCase(Locale.ROOT), r.colors(), true) + (r.icon().isEmpty() ? "" : " &#" + Text.last(r.colors()) + "&l" + r.icon());
    }

    private void ranks(Player p, String target) {
        Menu m = new Menu(6, "&8ʀᴀɴᴋѕ » " + target);
        m.set(4, targetHead(target), null);
        Player t = Bukkit.getPlayerExact(target);
        Object[][] rows = {
                {"store", "Store Ranks", "Ranks players buy", Material.YELLOW_STAINED_GLASS_PANE, Text.YELLOW},
                {"staff", "Staff Ranks", "Server team ranks", Material.RED_STAINED_GLASS_PANE, Text.RED},
                {"other", "Special Ranks", "Media and boosters", Material.MAGENTA_STAINED_GLASS_PANE, Text.PINK}};
        int row = 1;
        for (Object[] r : rows) {
            String cat = (String) r[0];
            List<Rank> list = ranks(cat);
            if (list.isEmpty()) continue;
            m.set(row * 9 + 1, button((Material) r[3], (String) r[4], (String) r[1], "Category", List.of(r[2] + ".")), null);
            for (int i = 0; i < list.size() && i < 6; i++) {
                Rank rank = list.get(i);
                boolean has = t != null && t.hasPermission("group." + rank.group());
                String col = Text.last(rank.colors());
                List<String> info = new ArrayList<>();
                info.add("Group: &#FCE300" + rank.group());
                if (t != null) info.add("Status: " + (has ? "&#00FC00✔ Has this rank" : "&#FC0000✘ Doesn't have it"));
                else info.add("Status: &#AAAAAAPlayer offline");
                ItemStack it = Text.item(rank.material(), rankTitle(rank), Text.lore(r[1].toString().replace("Ranks", "Rank"), col, info,
                        Text.action(Text.GREEN, "LEFT-CLICK", "to Give"), Text.action(Text.RED, "RIGHT-CLICK", "to Remove")));
                Text.glow(it, has);
                m.set(row * 9 + 2 + i, it, (q, c) -> {
                    if (rank.category().equals("staff") && !perm(q, "ranks.staff")) return;
                    if (c.isRightClick()) removeRank(q, target, rank);
                    else durations(q, target, rank);
                });
            }
            row++;
        }
        nav(m, (q, c) -> pickPlayer(q, "Ranks", true, this::ranks));
        m.frame();
        m.open(p);
    }

    private void durations(Player p, String target, Rank rank) {
        Menu m = new Menu(5, "&8" + Text.small(rank.name()) + " » " + target);
        String col = Text.last(rank.colors());
        m.set(4, Text.item(rank.material(), rankTitle(rank), Text.lore("Choose a Duration", col, List.of("For &#FCE300" + target + "&f."))), null);
        List<String> durs = new ArrayList<>(plugin.getConfig().getStringList("rank-durations"));
        if (durs.isEmpty()) durs = new ArrayList<>(List.of("permanent", "1h", "1d", "7d", "30d", "90d"));
        durs.add("custom");
        int[] slots = Menu.centered(1, Math.min(7, durs.size()));
        for (int i = 0; i < slots.length; i++) {
            String d = durs.get(i);
            boolean perm = d.equalsIgnoreCase("permanent");
            boolean custom = d.equalsIgnoreCase("custom");
            Material icon = perm ? Material.NETHER_STAR : custom ? Material.NAME_TAG : Material.CLOCK;
            String color = perm ? Text.PURPLE : custom ? Text.BLUE : Text.YELLOW;
            String label = perm ? "Forever" : custom ? "Custom Time" : pretty(d);
            ItemStack it = button(icon, color, label, "Duration",
                    List.of(perm ? "The rank never expires." : custom ? "Type any time in chat." : "Expires after &#FCE300" + pretty(d) + "&f."),
                    Text.action(color, "CLICK", "to Choose"));
            if (!perm && !custom) {
                try { it.setAmount(Math.max(1, Math.min(64, Integer.parseInt(d.replaceAll("[^0-9]", ""))))); } catch (NumberFormatException ignored) {}
            }
            m.set(slots[i] + 9, it, (q, c) -> {
                if (custom) {
                    plugin.ask(q, "Type the time, e.g. &#FCE30012h&f, &#FCE30014d&f, &#FCE3002w&f, &#FCE3006mo&f:", in -> {
                        String x = in.toLowerCase(Locale.ROOT).replace(" ", "");
                        if (!x.matches("(\\d+(mo|[smhdwy]))+")) { plugin.msg(q, "&#FC0000Invalid time."); bad(q); return; }
                        giveRank(q, target, rank, x);
                    });
                } else giveRank(q, target, rank, perm ? null : d);
            });
        }
        nav(m, (q, c) -> ranks(q, target));
        m.frame();
        m.open(p);
    }

    private static String pretty(String d) {
        if (!d.matches("\\d+(mo|[smhdwy])")) return d;
        String n = d.replaceAll("[^0-9]", "");
        String u = d.replaceAll("[0-9]", "");
        String word = switch (u) {
            case "s" -> "Second"; case "m" -> "Minute"; case "h" -> "Hour"; case "d" -> "Day";
            case "w" -> "Week"; case "mo" -> "Month"; case "y" -> "Year"; default -> u;
        };
        return n + " " + word + ("1".equals(n) ? "" : "s");
    }

    private void giveRank(Player p, String target, Rank rank, String duration) {
        String when = duration == null ? "forever" : "for " + pretty(duration).toLowerCase(Locale.ROOT);
        String col = Text.last(rank.colors());
        ItemStack what = Text.item(rank.material(), rankTitle(rank), Text.lore("Give Rank", col,
                List.of("Player: &#FCE300" + target, "Rank: &#" + col + rank.name(), "Time: &#FCE300" + when)));
        confirm(p, what, () -> {
            if (rank.category().equals("staff") && !p.hasPermission("chickensmp.admin.ranks.staff")) return;
            String cmd = duration == null
                    ? "lp user " + target + " parent add " + rank.group()
                    : "lp user " + target + " parent addtemp " + rank.group() + " " + duration + " accumulate";
            plugin.console(p, cmd);
            plugin.msg(p, "&fGave &#" + col + rank.name() + " &fto &#FCE300" + target + " &f" + when + ".");
            good(p);
            Player t = Bukkit.getPlayerExact(target);
            if (t != null && t != p) plugin.msg(t, "&fYou got the &#" + col + rank.name() + " &frank " + when + "!");
        }, () -> durations(p, target, rank));
    }

    private void removeRank(Player p, String target, Rank rank) {
        String col = Text.last(rank.colors());
        ItemStack what = Text.item(rank.material(), Text.title(Text.RED, "Remove " + rank.name()), Text.lore("Remove Rank", Text.RED,
                List.of("Player: &#FCE300" + target, "Rank: &#" + col + rank.name(), "Removes the permanent", "and the timed rank.")));
        confirm(p, what, () -> {
            plugin.console(p, "lp user " + target + " parent remove " + rank.group());
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.console(p, "lp user " + target + " parent removetemp " + rank.group()), 30L);
            plugin.msg(p, "&fRemoved &#" + col + rank.name() + " &ffrom &#FCE300" + target + "&f.");
            good(p);
        }, () -> ranks(p, target));
    }

    // ---------------------------------------------------------------- tools

    private void tools(Player p, String target) {
        Menu m = new Menu(5, "&8ѕʜᴀʀᴅ ᴛᴏᴏʟѕ » " + target);
        m.set(4, targetHead(target), null);
        int[] slots = {20, 21, 22, 23, 24};
        for (int i = 0; i < slots.length && i < Tools.IDS.size(); i++) {
            String id = Tools.IDS.get(i);
            ItemStack it = plugin.tools().create(id);
            if (it == null) continue;
            ItemMeta meta = it.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
            lore.add(Text.c(""));
            lore.add(Text.c(Text.action(Text.GREEN, "CLICK", "to Give")));
            meta.lore(lore);
            it.setItemMeta(meta);
            m.set(slots[i], it, (q, c) -> {
                if (!perm(q, "tools")) return;
                Player t = Bukkit.getPlayerExact(target);
                if (t == null) { plugin.msg(q, "&#FCE300" + target + " &#FC0000is not online."); bad(q); return; }
                plugin.give(t, plugin.tools().create(id));
                plugin.log(q.getName() + " gave tool " + id + " to " + t.getName());
                plugin.msg(q, "&fGave a " + plugin.tools().name(id) + " &fto &#FCE300" + t.getName() + "&f.");
                good(q);
                if (t != q) plugin.msg(t, "&fYou got a " + plugin.tools().name(id) + "&f!");
            });
        }
        nav(m, (q, c) -> pickPlayer(q, "Shard Tools", false, this::tools));
        m.frame();
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
                    Text.action(Text.GREEN, "LEFT-CLICK", "to Give 1"), Text.action(Text.YELLOW, "RIGHT-CLICK", "to Give 16"),
                    Text.action(Text.BLUE, "SHIFT-CLICK", "to Type an Amount"))),
                    (q, c) -> {
                        if (c == ClickType.SHIFT_LEFT || c == ClickType.SHIFT_RIGHT) {
                            plugin.ask(q, "How many &#FCE300" + mob.name() + " &fspawners?", in -> {
                                double n = Text.parseAmount(in);
                                if (n < 1 || n > 2304) { plugin.msg(q, "&#FC0000Type a number from 1 to 2304."); bad(q); return; }
                                giveSpawners(q, target, id, (int) n);
                                spawners(q, target);
                            });
                        } else giveSpawners(q, target, id, c.isRightClick() ? 16 : 1);
                    });
        }
        nav(m, (q, c) -> pickPlayer(q, "Spawners", false, this::spawners));
        m.set(53, targetHead(target), null);
        m.bar();
        m.open(p);
    }

    private void giveSpawners(Player q, String target, String mob, int amount) {
        if (!q.hasPermission("chickensmp.admin.spawners")) return;
        Player t = Bukkit.getPlayerExact(target);
        if (t == null) { plugin.msg(q, "&#FCE300" + target + " &#FC0000is not online."); bad(q); return; }
        plugin.giveSpawners(t, mob, amount);
        Spawners.Mob m = plugin.spawners().mob(mob);
        plugin.log(q.getName() + " gave " + amount + "x " + mob + " spawner to " + t.getName());
        plugin.msg(q, "&fGave &#FCE300" + amount + "x &#" + m.color() + m.name() + " Spawner &fto &#FCE300" + t.getName() + "&f.");
        good(q);
        if (t != q) plugin.msg(t, "&fYou got &#FCE300" + amount + "x &#" + m.color() + m.name() + " Spawner&f!");
    }

    // ---------------------------------------------------------------- money / shards

    private void amounts(Player p, String target, boolean shards) {
        String what = shards ? "Shards" : "Money";
        String color = shards ? Text.PURPLE : Text.GREEN;
        String unit = shards ? "" : "$";
        Menu m = new Menu(5, "&8" + Text.small(what) + " » " + target);
        m.set(4, targetHead(target), null);
        List<Double> list = new ArrayList<>();
        for (String s : plugin.getConfig().getStringList(shards ? "amounts.shards" : "amounts.money")) {
            double d = Text.parseAmount(s);
            if (d > 0) list.add(d);
        }
        int n = Math.min(6, list.size());
        int[] slots = Menu.centered(1, n + 1);
        Material icon = shards ? Material.AMETHYST_SHARD : Material.EMERALD;
        for (int i = 0; i < n; i++) {
            double amount = list.get(i);
            String label = unit + Text.compact(amount) + (shards ? " Shards" : "");
            ItemStack it = button(i >= n - 2 ? (shards ? Material.AMETHYST_CLUSTER : Material.EMERALD_BLOCK) : icon, color, label, what,
                    List.of("Amount: &#FCE300" + unit + Text.num(amount)),
                    Text.action(Text.GREEN, "LEFT-CLICK", "to Give"), Text.action(Text.RED, "RIGHT-CLICK", "to Take"));
            it.setAmount(Math.min(64, i + 1));
            m.set(slots[i] + 9, it, (q, c) -> changeAmount(q, target, shards, amount, c.isRightClick()));
        }
        m.set(slots[n] + 9, button(Material.NAME_TAG, Text.BLUE, "Custom Amount", what,
                List.of("Type any amount in chat,", "e.g. &#FCE300250k &for &#FCE3001.5m&f."),
                Text.action(Text.GREEN, "LEFT-CLICK", "to Give"), Text.action(Text.RED, "RIGHT-CLICK", "to Take")),
                (q, c) -> {
                    boolean take = c.isRightClick();
                    plugin.ask(q, "How much " + (shards ? "&#A303F9shards" : "&#00FC00money") + " &fto " + (take ? "&#FC0000take from" : "&#00FC00give to") + " &#FCE300" + target + "&f?", in -> {
                        double d = Text.parseAmount(in);
                        if (d <= 0) { plugin.msg(q, "&#FC0000That is not a valid amount."); bad(q); return; }
                        changeAmount(q, target, shards, d, take);
                    });
                });
        nav(m, (q, c) -> pickPlayer(q, what, true, (a, b) -> amounts(a, b, shards)));
        m.frame();
        m.open(p);
    }

    private void changeAmount(Player p, String target, boolean shards, double amount, boolean take) {
        String what = shards ? "Shards" : "Money";
        String color = shards ? Text.PURPLE : Text.GREEN;
        String unit = shards ? "" : "$";
        String amt = shards ? String.valueOf((long) Math.floor(amount)) : (amount == Math.floor(amount) ? String.valueOf((long) amount) : String.valueOf(amount));
        double big = plugin.getConfig().getDouble(shards ? "confirm-above.shards" : "confirm-above.money", shards ? 1000 : 10_000_000);
        Runnable run = () -> {
            if (!p.hasPermission(shards ? "chickensmp.admin.shards" : "chickensmp.admin.money")) return;
            plugin.console(p, (shards ? "shardmanager " : "moneymanager ") + (take ? "remove " : "add ") + target + " " + amt);
            plugin.msg(p, (take ? "&fTook &#FC0000" : "&fGave &#" + color) + unit + Text.num(Double.parseDouble(amt)) + (shards ? " shards" : "")
                    + (take ? " &ffrom &#FCE300" : " &fto &#FCE300") + target + "&f.");
            good(p);
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
        Menu m = new Menu(5, "&8ᴋᴇʏ ᴀʟʟ");
        File f = new File(plugin.getDataFolder().getParentFile(), "ChickenSMPKeyAll/config.yml");
        List<String[]> keys = new ArrayList<>();
        if (f.exists()) {
            ConfigurationSection ks = YamlConfiguration.loadConfiguration(f).getConfigurationSection("keys");
            if (ks != null) for (String k : ks.getKeys(false)) {
                String col = ks.getString(k + ".color", "&7");
                String hex = col.startsWith("&#") ? col.substring(2, Math.min(8, col.length())) : Text.GRAY;
                keys.add(new String[]{k, ks.getString(k + ".name", k), hex, String.valueOf(ks.getDouble(k + ".chance"))});
            }
        }
        keys.add(new String[]{"random", "Random Key", Text.YELLOW, ""});
        String timer = Papi.apply(p, "%keyall_timer%");
        m.set(4, button(Material.CLOCK, Text.YELLOW, "Key All", "Events", timer.contains("%")
                ? List.of("Players online: &#FCE300" + Bukkit.getOnlinePlayers().size())
                : List.of("Players online: &#FCE300" + Bukkit.getOnlinePlayers().size(), "Next automatic: &#FCE300" + timer)), null);
        int[] slots = Menu.centered(1, Math.min(7, keys.size()));
        for (int i = 0; i < slots.length; i++) {
            String[] k = keys.get(i);
            List<String> info = new ArrayList<>();
            info.add("Everyone online gets");
            info.add("&#FCE300" + amount + "x " + k[1] + "&f.");
            if (!k[3].isEmpty()) info.add("Random chance: &#FCE300" + k[3].replace(".0", "") + "%");
            Material icon = k[0].equals("random") ? Material.ENDER_EYE : Material.TRIPWIRE_HOOK;
            ItemStack it = Text.item(icon, Text.title(k[2], k[1]), Text.lore("Key All", k[2], info, Text.action(k[2], "CLICK", "to Start")));
            it.setAmount(Math.max(1, Math.min(64, amount)));
            m.set(slots[i] + 9, it, (q, c) -> {
                ItemStack what = Text.item(icon, Text.title(k[2], "Key All: " + k[1]), Text.lore("Key All", k[2],
                        List.of("Amount: &#FCE300" + amount + "x", "Players online: &#FCE300" + Bukkit.getOnlinePlayers().size())));
                confirm(q, what, () -> {
                    if (!q.hasPermission("chickensmp.admin.keyall")) return;
                    plugin.console(q, "ckeyall " + k[0] + " " + amount);
                    plugin.msg(q, "&fStarted a key all: &#" + k[2] + amount + "x " + k[1] + "&f.");
                    good(q);
                }, () -> keyAll(q, amount));
            });
        }
        int[] steps = {1, 2, 3, 5, 10};
        int idx = 0;
        for (int i = 0; i < steps.length; i++) if (steps[i] == amount) idx = i;
        int nextAmount = steps[(idx + 1) % steps.length];
        ItemStack am = button(Material.HOPPER, Text.BLUE, "Amount: " + amount + "x", "Settings", List.of("How many keys each", "player gets."),
                Text.action(Text.BLUE, "CLICK", "to Change to " + nextAmount + "x"));
        am.setAmount(amount);
        m.set(44, am, (q, c) -> keyAll(q, nextAmount));
        nav(m, (q, c) -> main(q));
        m.frame();
        m.open(p);
    }
}
