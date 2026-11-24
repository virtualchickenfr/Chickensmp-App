package net.chickensmp.admin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Colours (&-codes + &#rrggbb), item building and number formatting in the EconomyCore style. */
public final class Text {
    private Text() {}

    // EconomyCore palette
    public static final String GREEN = "00FC00", BLUE = "00A4FC", YELLOW = "FCE300", RED = "FC0000",
            PINK = "FC00E3", PURPLE = "A303F9", ORANGE = "F97603", GRAY = "AAAAAA", CYAN = "1EB8E9", MINT = "00FC99";

    private static final LegacyComponentSerializer S = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();

    public static Component c(String s) {
        return S.deserialize(s == null ? "" : s).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> c(List<String> lines) {
        List<Component> out = new ArrayList<>();
        for (String l : lines) out.add(c(l));
        return out;
    }

    public static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.displayName(c(name));
        if (lore != null) meta.lore(c(lore));
        meta.addItemFlags(ItemFlag.values());
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack glow(ItemStack it, boolean on) {
        ItemMeta meta = it.getItemMeta();
        if (meta != null && on) {
            meta.setEnchantmentGlintOverride(true);
            it.setItemMeta(meta);
        }
        return it;
    }

    /** EconomyCore button lore: category, blank, "Information" header, info lines, blank, action lines. */
    public static List<String> lore(String category, String color, List<String> info, String... actions) {
        List<String> l = new ArrayList<>();
        l.add("&7" + category);
        l.add("");
        l.add("&#" + color + "Information");
        for (String s : info) l.add("&f" + s);
        if (actions.length > 0) l.add("");
        l.addAll(Arrays.asList(actions));
        return l;
    }

    /** "&#C▶ &#C&l&nCLICK&r &#Cto give" */
    public static String action(String color, String click, String what) {
        return "&#" + color + "▶ &#" + color + "&l&n" + click + "&r &#" + color + what;
    }

    public static String title(String color, String name) {
        return "&#" + color + "&l" + name.toUpperCase(Locale.ROOT);
    }

    private static final DecimalFormat COMMA = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));

    public static String num(double n) {
        return COMMA.format(n);
    }

    /** 1500 -> 1.5K, 2000000 -> 2M */
    public static String compact(double n) {
        String[] u = {"", "K", "M", "B", "T", "Q"};
        int i = 0;
        double v = Math.abs(n);
        while (v >= 1000 && i < u.length - 1) { v /= 1000; i++; }
        String s = (v >= 100 || v == Math.floor(v)) ? String.valueOf((long) Math.floor(v)) : new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US)).format(v);
        return (n < 0 ? "-" : "") + s + u[i];
    }

    /** "1.5m", "250k", "1,000", "2b" -> number, or -1 */
    public static double parseAmount(String in) {
        if (in == null) return -1;
        String s = in.trim().toLowerCase(Locale.ROOT).replace(",", "").replace("$", "").replace("_", "");
        if (s.isEmpty()) return -1;
        double mul = 1;
        char last = s.charAt(s.length() - 1);
        switch (last) {
            case 'k' -> mul = 1e3;
            case 'm' -> mul = 1e6;
            case 'b' -> mul = 1e9;
            case 't' -> mul = 1e12;
            default -> {}
        }
        if (mul != 1) s = s.substring(0, s.length() - 1);
        try {
            double d = Double.parseDouble(s) * mul;
            return (d > 0 && d < 1e15 && !Double.isNaN(d)) ? d : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Per-letter gradient, e.g. gradient("PLUS", "7FE7FF:3B9BFF", true). One colour = plain. */
    public static String gradient(String text, String colors, boolean bold) {
        String[] cs = colors.split(":");
        String b = bold ? "&l" : "";
        if (cs.length < 2) return "&#" + cs[0] + b + text;
        int[] a = rgb(cs[0]), z = rgb(cs[cs.length - 1]);
        StringBuilder out = new StringBuilder();
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char ch = text.charAt(i);
            if (ch == ' ') { out.append(' '); continue; }
            double t = n <= 1 ? 0 : (double) i / (n - 1);
            out.append(String.format("&#%02X%02X%02X", (int) Math.round(a[0] + (z[0] - a[0]) * t),
                    (int) Math.round(a[1] + (z[1] - a[1]) * t), (int) Math.round(a[2] + (z[2] - a[2]) * t))).append(b).append(ch);
        }
        return out.toString();
    }

    private static int[] rgb(String hex) {
        int v = Integer.parseInt(hex.replace("#", ""), 16);
        return new int[]{(v >> 16) & 255, (v >> 8) & 255, v & 255};
    }

    /** First colour of "AAAAAA:BBBBBB". */
    public static String first(String colors) { return colors.split(":")[0]; }
    /** Last colour of "AAAAAA:BBBBBB". */
    public static String last(String colors) { String[] c = colors.split(":"); return c[c.length - 1]; }

    /** "ZOMBIE" -> "ᴢᴏᴍʙɪᴇ" */
    public static String small(String s) {
        String from = "abcdefghijklmnopqrstuvwxyz";
        String to = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀѕᴛᴜᴠᴡxʏᴢ";
        StringBuilder b = new StringBuilder();
        for (char ch : s.toLowerCase(Locale.ROOT).toCharArray()) {
            int i = from.indexOf(ch);
            b.append(i >= 0 ? to.charAt(i) : ch);
        }
        return b.toString();
    }
}
