package net.chickensmp.acplus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiConsumer;

/** /sus as a chest GUI (EconomyCore look). Console keeps the chat version. */
final class SusMenu implements Listener {

    private static final LegacyComponentSerializer S = LegacyComponentSerializer.builder().character('&').hexColors().build();
    private static final String RED = "FC0000", YELLOW = "FCE300", GREEN = "00FC00", BLUE = "00A4FC", PURPLE = "A303F9", GRAY = "AAAAAA";

    private final SusPlugin plugin;

    SusMenu(SusPlugin plugin) { this.plugin = plugin; }

    // ---------------------------------------------------------------- menu plumbing

    private static final class Holder implements InventoryHolder {
        Inventory inv;
        final Map<Integer, BiConsumer<Player, ClickType>> clicks = new HashMap<>();
        @Override public Inventory getInventory() { return inv; }
    }

    private static Component c(String s) { return S.deserialize(s).decoration(TextDecoration.ITALIC, false); }

    private static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(c(name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(c(s));
        meta.lore(l);
        meta.addItemFlags(ItemFlag.values());
        it.setItemMeta(meta);
        return it;
    }

    private static List<String> lore(String category, String color, List<String> info, String... actions) {
        List<String> l = new ArrayList<>();
        l.add("&7" + category);
        l.add("");
        l.add("&#" + color + "Information");
        for (String s : info) l.add("&f" + s);
        if (actions.length > 0) l.add("");
        l.addAll(Arrays.asList(actions));
        return l;
    }

    private static String action(String color, String click, String what) {
        return "&#" + color + "▶ &#" + color + "&l&n" + click + "&r &#" + color + what;
    }

    private static final ItemStack FILL = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());

    private static void set(Holder h, int slot, ItemStack it, BiConsumer<Player, ClickType> click) {
        h.inv.setItem(slot, it);
        if (click != null) h.clicks.put(slot, click);
    }

    private static Holder create(int rows, String title) {
        Holder h = new Holder();
        h.inv = Bukkit.createInventory(h, rows * 9, c(title));
        return h;
    }

    private static void bar(Holder h) {
        for (int i = h.inv.getSize() - 9; i < h.inv.getSize(); i++) if (h.inv.getItem(i) == null) h.inv.setItem(i, FILL);
    }

    private static void frame(Holder h) {
        int rows = h.inv.getSize() / 9;
        for (int i = 0; i < h.inv.getSize(); i++) {
            int r = i / 9, col = i % 9;
            if ((r == 0 || r == rows - 1 || col == 0 || col == 8) && h.inv.getItem(i) == null) h.inv.setItem(i, FILL);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Holder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getView().getTopInventory()) return;
        BiConsumer<Player, ClickType> c = h.clicks.get(e.getRawSlot());
        if (c == null) return;
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
        c.accept(p, e.getClick());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Holder) e.setCancelled(true);
    }

    // ---------------------------------------------------------------- helpers

    private String state(double sc) {
        return sc >= plugin.susScore() ? "&#" + RED + "&lSUSPICIOUS" : sc >= plugin.watchScore() ? "&#" + YELLOW + "WATCH" : "&#" + GREEN + "OK";
    }

    private String stateColor(double sc) {
        return sc >= plugin.susScore() ? RED : sc >= plugin.watchScore() ? YELLOW : GREEN;
    }

    private static String ago(long time) {
        if (time == 0) return "-";
        long s = (System.currentTimeMillis() - time) / 1000;
        if (s < 60) return s + "s ago";
        if (s < 3600) return s / 60 + "m ago";
        if (s < 86400) return s / 3600 + "h ago";
        return s / 86400 + "d ago";
    }

    private ItemStack head(PlayerData d, List<String> lore, String titleColor) {
        ItemStack it = item(Material.PLAYER_HEAD, "&#" + titleColor + "&l" + d.name, lore);
        if (it.getItemMeta() instanceof SkullMeta sm) {
            OfflinePlayer op = Bukkit.getPlayer(d.uuid);
            if (op != null) sm.setOwningPlayer(op);
            it.setItemMeta(sm);
        }
        return it;
    }

    // ---------------------------------------------------------------- list

    void openList(Player p, int page) {
        List<Map.Entry<PlayerData, Double>> rows = plugin.ranking();
        int pages = Math.max(1, (rows.size() + 44) / 45);
        int pg = Math.max(0, Math.min(page, pages - 1));
        Holder h = create(6, "&8ѕᴜѕᴘɪᴄɪᴏᴜѕ ᴘʟᴀʏᴇʀѕ");
        for (int i = 0; i < 45; i++) {
            int idx = pg * 45 + i;
            if (idx >= rows.size()) break;
            PlayerData d = rows.get(idx).getKey();
            double sc = rows.get(idx).getValue();
            boolean online = Bukkit.getPlayer(d.uuid) != null;
            String col = stateColor(sc);
            List<String> info = new ArrayList<>(List.of(
                    "Rank: &#" + YELLOW + "#" + (idx + 1),
                    "Status: " + state(sc),
                    "Score: &#" + col + SusPlugin.fmt(sc) + " &f/ &#" + YELLOW + SusPlugin.fmt(plugin.susScore()),
                    "Flags: &#" + YELLOW + d.totalFlags + " &7(" + d.susTimes + "x sus)",
                    "Top: &#" + YELLOW + PlayerData.top(d.totalChecks(), 2),
                    "Last flag: &#" + YELLOW + ago(d.lastFlag),
                    online ? "&#" + GREEN + "● Online" : "&#" + GRAY + "○ Offline"));
            List<String> acts = new ArrayList<>();
            acts.add(action(BLUE, "LEFT-CLICK", "for Details"));
            if (online) acts.add(action(GREEN, "RIGHT-CLICK", "to Teleport"));
            ItemStack it = head(d, lore("Suspicious Player", col, info, acts.toArray(new String[0])), col);
            if (sc >= plugin.susScore()) {
                ItemMeta m = it.getItemMeta();
                m.setEnchantmentGlintOverride(true);
                it.setItemMeta(m);
            }
            String name = d.name;
            set(h, i, it, (q, t) -> {
                if (t.isRightClick() && Bukkit.getPlayer(d.uuid) != null) { q.closeInventory(); q.performCommand("sus tp " + name); }
                else openPlayer(q, d);
            });
        }
        if (rows.isEmpty()) set(h, 22, item(Material.LIME_DYE, "&#" + GREEN + "&lALL CLEAN", lore("Anticheat", GREEN,
                List.of("Nobody has been flagged.", plugin.hooked() ? "Anticheat is connected." : "&#" + RED + "No anticheat connected!"))), null);
        if (pg > 0) set(h, 48, item(Material.ARROW, "&#" + RED + "&lPREVIOUS PAGE", lore("Navigation", RED, List.of("Page &#" + YELLOW + pg + " &fof &#" + YELLOW + pages + "&f."),
                action(RED, "CLICK", "to Go Back"))), (q, t) -> openList(q, pg - 1));
        if (pg < pages - 1) set(h, 50, item(Material.ARROW, "&#" + GREEN + "&lNEXT PAGE", lore("Navigation", GREEN, List.of("Page &#" + YELLOW + (pg + 2) + " &fof &#" + YELLOW + pages + "&f."),
                action(GREEN, "CLICK", "to Go Forward"))), (q, t) -> openList(q, pg + 1));
        set(h, 45, item(Material.BOOK, "&#" + BLUE + "&lHOW IT WORKS", lore("Help", BLUE, List.of(
                "Every anticheat flag adds points.",
                "&#" + YELLOW + SusPlugin.fmt(plugin.watchScore()) + " &fpoints = &#" + YELLOW + "WATCH",
                "&#" + RED + SusPlugin.fmt(plugin.susScore()) + " &fpoints = &#" + RED + "SUSPICIOUS",
                "Points never expire. Banned", "players are removed."))), null);
        set(h, 49, item(Material.BARRIER, "&#" + RED + "&lCLOSE", lore("Navigation", RED, List.of("Close this menu."), action(RED, "CLICK", "to Close"))),
                (q, t) -> q.closeInventory());
        set(h, 53, item(Material.CLOCK, "&#" + YELLOW + "&lREFRESH", lore("Navigation", YELLOW, List.of("Players listed: &#" + YELLOW + rows.size()), action(YELLOW, "CLICK", "to Refresh"))),
                (q, t) -> openList(q, pg));
        bar(h);
        p.openInventory(h.inv);
    }

    // ---------------------------------------------------------------- player

    void openPlayer(Player p, PlayerData d) {
        double sc = d.score();
        String col = stateColor(sc);
        boolean online = Bukkit.getPlayer(d.uuid) != null;
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        Holder h = create(5, "&8ѕᴜѕ » " + d.name);
        set(h, 4, head(d, lore("Suspicious Player", col, List.of(
                "Status: " + state(sc),
                "Score: &#" + col + SusPlugin.fmt(sc) + " &f/ &#" + YELLOW + SusPlugin.fmt(plugin.susScore()),
                "Flags: &#" + YELLOW + d.totalFlags,
                "Sus alerts: &#" + YELLOW + d.susTimes + "x",
                "First flag: &#" + YELLOW + (d.firstFlag == 0 ? "-" : df.format(new Date(d.firstFlag))),
                "Last flag: &#" + YELLOW + (d.lastFlag == 0 ? "-" : df.format(new Date(d.lastFlag))),
                online ? "&#" + GREEN + "● Online" : "&#" + GRAY + "○ Offline")), col), null);

        List<String> checks = new ArrayList<>();
        int n = 0;
        for (Map.Entry<String, Integer> e : d.totalChecks().entrySet()) {
            if (n++ >= 8) break;
            checks.add(e.getKey() + " &#" + YELLOW + "x" + e.getValue());
        }
        if (checks.isEmpty()) checks.add("&7None");
        set(h, 20, item(Material.WRITABLE_BOOK, "&#" + PURPLE + "&lTOP CHECKS", lore("Anticheat", PURPLE, checks)), null);

        List<String> last = new ArrayList<>();
        List<PlayerData.Flag> flags = d.lastFlags(8);
        for (int i = flags.size() - 1; i >= 0; i--) {
            PlayerData.Flag f = flags.get(i);
            last.add(f.check() + " &7VL " + f.vl() + " &#" + YELLOW + ago(f.time()));
        }
        if (last.isEmpty()) last.add("&7None");
        set(h, 21, item(Material.PAPER, "&#" + BLUE + "&lLAST FLAGS", lore("Anticheat", BLUE, last)), null);

        String name = d.name;
        if (online) {
            set(h, 22, item(Material.ENDER_PEARL, "&#" + GREEN + "&lTELEPORT", lore("Staff Mode", GREEN, List.of("Turns on staff mode", "and teleports you."), action(GREEN, "CLICK", "to Teleport"))),
                    (q, t) -> { q.closeInventory(); q.performCommand("sus tp " + name); });
            set(h, 23, item(Material.ENDER_EYE, "&#" + BLUE + "&lWATCH", lore("Staff Mode", BLUE, List.of("Turns on staff mode and", "watches from their view."), action(BLUE, "CLICK", "to Watch"))),
                    (q, t) -> { q.closeInventory(); q.performCommand("sus watch " + name); });
        } else {
            set(h, 22, item(Material.GRAY_DYE, "&#" + GRAY + "&lTELEPORT", lore("Staff Mode", GRAY, List.of("Player is offline."))), null);
            set(h, 23, item(Material.GRAY_DYE, "&#" + GRAY + "&lWATCH", lore("Staff Mode", GRAY, List.of("Player is offline."))), null);
        }
        set(h, 24, item(Material.BOOK, "&#" + YELLOW + "&lHISTORY", lore("Punishments", YELLOW, List.of("Show their punishment", "history in chat."), action(YELLOW, "CLICK", "to Show"))),
                (q, t) -> { q.closeInventory(); q.performCommand("history " + name); });
        set(h, 31, item(Material.MACE, "&#" + RED + "&lPUNISH", lore("Punishments", RED, List.of("Open the punishment", "menu for this player."), action(RED, "CLICK", "to Punish"))),
                (q, t) -> { q.closeInventory(); q.performCommand("punish " + name); });
        set(h, 40, item(Material.ARROW, "&#" + RED + "&lBACK", lore("Navigation", RED, List.of("Back to the list."), action(RED, "CLICK", "to Go Back"))),
                (q, t) -> openList(q, 0));
        set(h, 44, item(Material.LAVA_BUCKET, "&#" + RED + "&lDELETE ENTRY", lore("Data", RED, List.of("Remove this player", "from /sus."), action(RED, "SHIFT-CLICK", "to Delete"))),
                (q, t) -> {
                    if (!t.isShiftClick()) { q.playSound(q.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f); return; }
                    q.performCommand("sus clear " + name);
                    openList(q, 0);
                });
        frame(h);
        p.openInventory(h.inv);
    }
}
