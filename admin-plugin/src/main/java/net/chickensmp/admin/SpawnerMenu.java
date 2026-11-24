package net.chickensmp.admin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Storage GUI of one placed spawner: 45 item slots per page, sell all, collect XP, drop page. */
public final class SpawnerMenu extends Menu {

    final Spawners sp;
    final Spawners.Data data;
    int page;
    private List<ItemStack> shown = List.of();

    public SpawnerMenu(Spawners sp, Spawners.Data data, int page) {
        super(6, "&8" + Text.small(sp.mob(data.mob) == null ? data.mob : sp.mob(data.mob).name()) + " ѕᴘᴀᴡɴᴇʀ");
        this.sp = sp;
        this.data = data;
        this.page = page;
        render();
    }

    private List<ItemStack> stacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Map.Entry<Material, Long> e : data.items.entrySet()) {
            long left = e.getValue();
            int max = e.getKey().getMaxStackSize();
            while (left > 0) {
                int n = (int) Math.min(max, left);
                out.add(new ItemStack(e.getKey(), n));
                left -= n;
            }
        }
        return out;
    }

    public void render() {
        clear();
        Spawners.Mob mob = sp.mob(data.mob);
        String color = mob == null ? Text.BLUE : mob.color();
        String name = mob == null ? data.mob : mob.name();
        List<ItemStack> all = stacks();
        int pages = Math.max(1, (all.size() + 44) / 45);
        if (page >= pages) page = pages - 1;
        shown = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i;
            if (idx >= all.size()) break;
            ItemStack it = all.get(idx);
            shown.add(it.clone());
            ItemStack show = it.clone();
            ItemMeta meta = show.getItemMeta();
            double price = sp.price(it.getType());
            List<String> lore = new ArrayList<>();
            if (price > 0) lore.add("&7Worth: &#00FC00$" + Text.num(Math.round(price * it.getAmount() * 100) / 100.0));
            lore.add("");
            lore.add(Text.action(Text.GREEN, "CLICK", "to take"));
            lore.add(Text.action(Text.YELLOW, "SHIFT-CLICK", "to take all " + Text.small(it.getType().name().replace('_', ' '))));
            meta.lore(Text.c(lore));
            show.setItemMeta(meta);
            Material m = it.getType();
            int amount = it.getAmount();
            set(i, show, (p, t) -> {
                long n = (t == ClickType.SHIFT_LEFT || t == ClickType.SHIFT_RIGHT) ? Long.MAX_VALUE : amount;
                sp.take(p, data, m, n);
            });
        }
        long stored = data.stored(), cap = sp.capacity(data);
        int pct = cap == 0 ? 0 : (int) Math.min(100, stored * 100 / cap);
        if (page > 0) set(45, Text.item(Material.ARROW, Text.title(Text.RED, "Previous Page"),
                Text.lore("Navigation", Text.RED, List.of("Go to page &#FCE300" + page + "&f."), Text.action(Text.RED, "CLICK", "to go back"))),
                (p, t) -> { page--; render(); });
        if (page < pages - 1) set(53, Text.item(Material.ARROW, Text.title(Text.GREEN, "Next Page"),
                Text.lore("Navigation", Text.GREEN, List.of("Go to page &#FCE300" + (page + 2) + "&f."), Text.action(Text.GREEN, "CLICK", "to go forward"))),
                (p, t) -> { page++; render(); });
        set(47, Text.item(Material.DROPPER, Text.title(Text.BLUE, "Drop Page"),
                Text.lore("Items", Text.BLUE, List.of("Drop all items on", "this page to the ground."), Text.action(Text.BLUE, "CLICK", "to drop"))),
                (p, t) -> sp.dropPage(p, data, shown));
        set(48, Text.item(Material.GOLD_INGOT, Text.title(Text.GREEN, "Sell All"),
                Text.lore("Economy", Text.GREEN, List.of("Sell every item in", "this spawner's storage.", "Value: &#00FC00$" + Text.num(Math.round(sp.value(data) * 100) / 100.0)),
                        Text.action(Text.GREEN, "CLICK", "to sell all"))),
                (p, t) -> sp.sellAll(p, data));
        ItemStack info = Text.item(mob == null ? Material.SPAWNER : mob.icon(), Text.title(color, name + " Spawner"),
                Text.lore("Virtual Spawner", color, List.of(
                        "Stack: &#FCE300×" + Text.num(data.stack) + " &7(max &#FCE300" + Text.num(sp.maxStack()) + "&7)",
                        "Storage: &#FCE300" + Text.num(stored) + " &f/ &#FCE300" + Text.num(cap) + " &7(&#FCE300" + pct + "%&7)",
                        "XP: &#00FC00" + Text.num(data.xp) + " &f/ &#00FC00" + Text.num(sp.xpCapacity(data)),
                        "Page: &#FCE300" + (page + 1) + " &f/ &#FCE300" + pages)));
        set(49, info, null);
        set(50, Text.item(Material.EXPERIENCE_BOTTLE, Text.title(Text.GREEN, "Collect XP"),
                Text.lore("Experience", Text.GREEN, List.of("Stored: &#00FC00" + Text.num(data.xp) + " XP"), Text.action(Text.GREEN, "CLICK", "to collect"))),
                (p, t) -> sp.collectXp(p, data));
        fill();
    }

    @Override
    public void open(Player p) {
        super.open(p);
    }
}
