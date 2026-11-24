package net.chickensmp.admin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A chest GUI. Nothing can be put in or taken out; clicks on slots run handlers. */
public class Menu implements InventoryHolder {

    public interface Click { void run(Player p, ClickType type); }

    private static final ItemStack FILLER = Text.item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
    private static final ItemStack EDGE = Text.item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());

    protected final Inventory inv;
    private final Map<Integer, Click> clicks = new HashMap<>();

    public Menu(int rows, String title) {
        this.inv = Bukkit.createInventory(this, rows * 9, Text.c(title));
    }

    @Override
    public Inventory getInventory() { return inv; }

    public void set(int slot, ItemStack item, Click click) {
        inv.setItem(slot, item);
        if (click != null) clicks.put(slot, click); else clicks.remove(slot);
    }

    public void clear() {
        inv.clear();
        clicks.clear();
    }

    public void fill() {
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, FILLER);
    }

    /** Black frame around the menu (inner slots stay empty), gray corners. */
    public void frame() {
        int rows = inv.getSize() / 9;
        for (int i = 0; i < inv.getSize(); i++) {
            int r = i / 9, c = i % 9;
            boolean edge = r == 0 || r == rows - 1 || c == 0 || c == 8;
            if (!edge || inv.getItem(i) != null) continue;
            boolean corner = (r == 0 || r == rows - 1) && (c == 0 || c == 8);
            inv.setItem(i, corner ? EDGE : FILLER);
        }
    }

    /** Black bar in the bottom row only. */
    public void bar() {
        for (int i = inv.getSize() - 9; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, FILLER);
    }

    public int size() { return inv.getSize(); }

    public void open(Player p) {
        p.openInventory(inv);
    }

    protected void onClose(Player p) {}

    /** Slots for n items centred in a row (row 0 = top). */
    public static int[] centered(int row, int n) {
        int[] out = new int[n];
        int[][] layouts = {
                {}, {4}, {3, 5}, {2, 4, 6}, {1, 3, 5, 7}, {0, 2, 4, 6, 8}, {1, 2, 3, 5, 6, 7}, {1, 2, 3, 4, 5, 6, 7},
                {0, 1, 2, 3, 5, 6, 7, 8}, {0, 1, 2, 3, 4, 5, 6, 7, 8}};
        int[] l = layouts[Math.min(n, 9)];
        for (int i = 0; i < n && i < l.length; i++) out[i] = row * 9 + l[i];
        return out;
    }

    public static final class Listen implements Listener {
        @EventHandler(priority = EventPriority.LOWEST)
        public void onClick(InventoryClickEvent e) {
            if (!(e.getView().getTopInventory().getHolder(false) instanceof Menu m)) return;
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            if (e.getClickedInventory() != e.getView().getTopInventory()) return;
            Click c = m.clicks.get(e.getRawSlot());
            if (c == null) return;
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            try {
                c.run(p, e.getClick());
            } catch (Exception ex) {
                AdminPlugin.get().getLogger().warning("Menu click failed: " + ex);
                ex.printStackTrace();
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onDrag(InventoryDragEvent e) {
            if (e.getView().getTopInventory().getHolder(false) instanceof Menu) e.setCancelled(true);
        }

        @EventHandler
        public void onClose(InventoryCloseEvent e) {
            if (e.getInventory().getHolder(false) instanceof Menu m && e.getPlayer() instanceof Player p) m.onClose(p);
        }
    }
}
