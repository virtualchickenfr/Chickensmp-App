package net.chickensmp.stocks;

import static net.chickensmp.stocks.StocksPlugin.BLUE;
import static net.chickensmp.stocks.StocksPlugin.GREEN;
import static net.chickensmp.stocks.StocksPlugin.MONEY;
import static net.chickensmp.stocks.StocksPlugin.PCT;
import static net.chickensmp.stocks.StocksPlugin.RED;
import static net.chickensmp.stocks.StocksPlugin.YELLOW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.chickensmp.stocks.StocksPlugin.Ore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** The chest menus: the market with all ores, and one trade screen per ore. */
final class Menus implements Listener {

    private static final String MINT = "&#00FC99";
    private static final String CHART = "▁▂▃▄▅▆▇█";
    private static final int[] ORE_SLOTS = {20, 21, 22, 23, 24};
    private final StocksPlugin plugin;

    Menus(StocksPlugin plugin) { this.plugin = plugin; }

    /** Holder that remembers what the menu is and what each slot does. */
    static final class Screen implements InventoryHolder {
        final Player viewer;
        final Ore ore; // null = market
        final Map<Integer, Consumer<Player>> actions = new java.util.HashMap<>();
        Inventory inv;

        Screen(Player viewer, Ore ore) { this.viewer = viewer; this.ore = ore; }

        @Override public Inventory getInventory() { return inv; }
    }

    // ------------------------------------------------------------------ open / refresh

    void openMarket(Player p) {
        Screen s = new Screen(p, null);
        s.inv = Bukkit.createInventory(s, 45, StocksPlugin.color(MINT + "&lORE MARKET"));
        drawMarket(s);
        p.openInventory(s.inv);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.7f, 1.6f);
    }

    void openOre(Player p, Ore o) {
        Screen s = new Screen(p, o);
        s.inv = Bukkit.createInventory(s, 45, StocksPlugin.color(o.name));
        drawOre(s);
        p.openInventory(s.inv);
    }

    /** Redraw every open menu (prices, timer, balance). */
    void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Screen s) redraw(s);
        }
    }

    private void redraw(Screen s) {
        if (s.ore == null) drawMarket(s); else drawOre(s);
    }

    // ------------------------------------------------------------------ market screen

    private void drawMarket(Screen s) {
        s.actions.clear();
        fill(s.inv);
        Player p = s.viewer;
        UUID u = p.getUniqueId();
        long left = Math.max(0, (plugin.nextTick - System.currentTimeMillis()) / 1000);
        double fee = plugin.getConfig().getDouble("fee-percent", 1.5);

        s.inv.setItem(4, item(Material.BOOK, 1, MINT + "&lHOW IT WORKS",
                "&7Ore market", "",
                MINT + "Information",
                "&f1. &7Buy an ore when its price is &flow&7.",
                "&f2. &7Wait until the price goes &fup&7.",
                "&f3. &7Sell it - the difference is your &fprofit&7.", "",
                "&fPrices change every &#FCE300" + span(plugin.tickSeconds) + "&f.",
                "&fNext change in &#FCE300" + left + "s&f.",
                "&fFee: &#FCE300" + PCT.format(fee) + "% &7on every buy and sell.", "",
                "&7Higher risk = bigger ups and downs."));

        int i = 0;
        for (Ore o : plugin.ores.values()) {
            if (i >= ORE_SLOTS.length) break;
            int have = plugin.owned(u, o);
            double worth = have * o.price;
            List<String> lore = new ArrayList<>(List.of(
                    "&7Ore • Risk: " + risk(o.risk), "",
                    BLUE + "Price",
                    "&f• Now: &#FCE300$" + MONEY.format(o.price),
                    "&f• Last hour: " + StocksPlugin.arrow(o.hourChange()),
                    "&f• Chart: " + chart(o), "",
                    BLUE + "You",
                    "&f• You own: &#FCE300" + have + (have >= o.max ? " &7(max)" : "")));
            if (have > 0) {
                lore.add("&f• Worth: &#FCE300$" + MONEY.format(worth));
                lore.add("&f• Profit: " + StocksPlugin.profit(worth - plugin.paid(u, o)));
            }
            lore.add("");
            lore.add(GREEN + "▶ " + GREEN + "&l&nCLICK&r " + GREEN + "to buy or sell");
            int slot = ORE_SLOTS[i++];
            s.inv.setItem(slot, item(o.material, Math.max(1, Math.min(64, have)), o.name, lore.toArray(String[]::new)));
            s.actions.put(slot, pl -> openOre(pl, o));
        }

        // Your ores
        double worth = plugin.worth(u), paid = plugin.paidTotal(u);
        List<String> mine = new ArrayList<>(List.of("&7Your ores", "", BLUE + "Information"));
        boolean any = false;
        for (Ore o : plugin.ores.values()) {
            int have = plugin.owned(u, o);
            if (have <= 0) continue;
            any = true;
            mine.add("&f• " + StocksPlugin.plain(o) + " &7x" + have + " &f= &#FCE300$" + MONEY.format(have * o.price));
        }
        if (!any) mine.add("&7You don't own any ores yet.");
        mine.add("");
        mine.add("&fTotal worth: &#FCE300$" + MONEY.format(worth));
        if (any) mine.add("&fTotal profit: " + StocksPlugin.profit(worth - paid));
        mine.add("&fYour money: &#FCE300$" + MONEY.format(plugin.balance(p)));
        s.inv.setItem(38, item(Material.CHEST, 1, BLUE + "&lYOUR ORES", mine.toArray(String[]::new)));

        // Close
        s.inv.setItem(40, item(Material.BARRIER, 1, RED + "&lCLOSE", "&7Close the menu"));
        s.actions.put(40, Player::closeInventory);

        // Top traders
        List<Map.Entry<String, Double>> top = plugin.top();
        List<String> tl = new ArrayList<>(List.of("&7Who has the most ore value", "", YELLOW + "Information"));
        String[] medal = {YELLOW, "&f", "&#F97603"};
        for (int r = 0; r < Math.min(5, top.size()); r++) {
            tl.add((r < 3 ? medal[r] : "&7") + "#" + (r + 1) + " &f" + top.get(r).getKey() + " &7- &#FCE300$" + MONEY.format(top.get(r).getValue()));
        }
        if (top.isEmpty()) tl.add("&7Nobody yet. Be the first!");
        s.inv.setItem(42, item(Material.NETHER_STAR, 1, YELLOW + "&lTOP TRADERS", tl.toArray(String[]::new)));
    }

    // ------------------------------------------------------------------ ore screen

    private void drawOre(Screen s) {
        s.actions.clear();
        fill(s.inv);
        Player p = s.viewer;
        Ore o = s.ore;
        UUID u = p.getUniqueId();
        int have = plugin.owned(u, o);
        double worth = have * o.price;
        double money = plugin.balance(p);

        List<String> info = new ArrayList<>(List.of(
                "&7Ore • Risk: " + risk(o.risk), "",
                BLUE + "Price",
                "&f• Now: &#FCE300$" + MONEY.format(o.price),
                "&f• Normal: &7$" + MONEY.format(o.base),
                "&f• Last hour: " + StocksPlugin.arrow(o.hourChange()),
                "&f• Chart: " + chart(o), "",
                BLUE + "You",
                "&f• You own: &#FCE300" + have + " &7/ " + o.max));
        if (have > 0) {
            info.add("&f• Worth: &#FCE300$" + MONEY.format(worth));
            info.add("&f• Profit: " + StocksPlugin.profit(worth - plugin.paid(u, o)));
        }
        info.add("&f• Your money: &#FCE300$" + MONEY.format(money));
        info.add("");
        info.add(o.price < o.base ? GREEN + "Cheaper than normal right now." : (o.price > o.base ? RED + "More expensive than normal right now." : "&7At its normal price."));
        s.inv.setItem(13, item(o.material, Math.max(1, Math.min(64, have)), o.name, info.toArray(String[]::new)));

        int room = o.max - have;
        int[] buys = {1, 10, 64};
        int[] buySlots = {28, 29, 30};
        for (int k = 0; k < 3; k++) {
            int n = buys[k];
            int real = Math.min(n, room);
            String cost = real > 0 ? "&#FCE300$" + MONEY.format(plugin.buyCost(o, real)) : "&7-";
            s.inv.setItem(buySlots[k], item(Material.LIME_CONCRETE, n, GREEN + "&lBUY " + n,
                    "&7Buy " + n + " " + StocksPlugin.plain(o), "",
                    BLUE + "Information",
                    "&f• Costs: " + cost + " &7(fee included)",
                    real < n ? "&7You can only own " + o.max + "." : "&7You get what you can afford.", "",
                    GREEN + "▶ " + GREEN + "&l&nCLICK&r " + GREEN + "to buy"));
            s.actions.put(buySlots[k], pl -> { plugin.buy(pl, o, n); redraw(s); });
        }

        int[] sells = {1, 10, -1};
        int[] sellSlots = {32, 33, 34};
        for (int k = 0; k < 3; k++) {
            int n = sells[k];
            int real = n < 0 ? have : Math.min(n, have);
            String label = n < 0 ? "SELL ALL" : "SELL " + n;
            String get = real > 0 ? "&#FCE300$" + MONEY.format(plugin.sellMoney(o, real)) : "&7-";
            s.inv.setItem(sellSlots[k], item(Material.RED_CONCRETE, n < 0 ? Math.max(1, Math.min(64, have)) : n, RED + "&l" + label,
                    "&7Sell " + (n < 0 ? "all " + have : String.valueOf(n)) + " " + StocksPlugin.plain(o), "",
                    BLUE + "Information",
                    "&f• You get: " + get + " &7(fee taken off)",
                    have > 0 ? "&f• Profit on these: " + StocksPlugin.profit(plugin.sellMoney(o, real) - plugin.paid(u, o) * real / have) : "&7You don't own any yet.", "",
                    RED + "▶ " + RED + "&l&nCLICK&r " + RED + "to sell"));
            s.actions.put(sellSlots[k], pl -> { plugin.sell(pl, o, n); redraw(s); });
        }

        s.inv.setItem(40, item(Material.ARROW, 1, "&f&lBACK", "&7Back to the market"));
        s.actions.put(40, this::openMarket);
    }

    // ------------------------------------------------------------------ events

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Screen s)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        Consumer<Player> a = s.actions.get(e.getSlot());
        if (a == null) return;
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
        a.accept(p);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Screen) e.setCancelled(true);
    }

    // ------------------------------------------------------------------ helpers

    private static void fill(Inventory inv) {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, 1, " ");
        ItemStack edge = item(Material.BLACK_STAINED_GLASS_PANE, 1, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            int row = i / 9, col = i % 9;
            boolean border = row == 0 || row == inv.getSize() / 9 - 1 || col == 0 || col == 8;
            inv.setItem(i, border ? edge : pane);
        }
    }

    private static String risk(String r) {
        String c = switch (r.toUpperCase()) {
            case "LOW" -> GREEN;
            case "MEDIUM" -> YELLOW;
            case "HIGH" -> "&#F97603";
            default -> RED;
        };
        return c + r.toUpperCase();
    }

    /** 12 bars over the kept history, green when going up, red when going down. */
    private static String chart(Ore o) {
        List<Double> h = new ArrayList<>(o.history);
        int bars = 12;
        List<Double> pts = new ArrayList<>();
        for (int i = 0; i < bars; i++) {
            int idx = h.size() <= bars ? i : (int) Math.round(i * (h.size() - 1) / (double) (bars - 1));
            if (idx < h.size()) pts.add(h.get(idx));
        }
        double min = pts.stream().mapToDouble(d -> d).min().orElse(0), max = pts.stream().mapToDouble(d -> d).max().orElse(0);
        StringBuilder sb = new StringBuilder();
        double prev = pts.isEmpty() ? 0 : pts.get(0);
        for (double d : pts) {
            int lvl = max - min < 1e-9 ? 3 : (int) Math.round((d - min) / (max - min) * 7);
            sb.append(d >= prev ? GREEN : RED).append(CHART.charAt(lvl));
            prev = d;
        }
        return sb.toString();
    }

    private static String span(long sec) {
        return sec % 60 == 0 ? (sec / 60 == 1 ? "minute" : sec / 60 + " minutes") : sec + " seconds";
    }

    static ItemStack item(Material m, int amount, String name, String... lore) {
        ItemStack it = new ItemStack(m, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = it.getItemMeta();
        meta.displayName(StocksPlugin.line(name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(StocksPlugin.line(s));
        meta.lore(l);
        meta.addItemFlags(ItemFlag.values());
        it.setItemMeta(meta);
        return it;
    }
}
