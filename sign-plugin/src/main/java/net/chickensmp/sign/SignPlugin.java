package net.chickensmp.sign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SignPlugin extends JavaPlugin {
    private static final String LIMIT_PREFIX = "chickensmp.sign.limit.";
    private final MiniMessage mm = MiniMessage.miniMessage();
    private NamespacedKey keyBy, keyName, keyLines, keySigs;
    private File usesFile;
    private YamlConfiguration uses;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        keyBy = new NamespacedKey(this, "signed_by");
        keyName = new NamespacedKey(this, "signed_name");
        keyLines = new NamespacedKey(this, "sign_lines");
        keySigs = new NamespacedKey(this, "signatures");
        usesFile = new File(getDataFolder(), "uses.yml");
        uses = YamlConfiguration.loadConfiguration(usesFile);
        getLogger().info("ChickenSMPSign enabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        boolean sign = cmd.getName().equalsIgnoreCase("sign");
        if (sign && args.length == 1 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("chickensmp.sign.admin")) {
            reloadConfig();
            send(sender, "reloaded");
            return true;
        }
        if (!(sender instanceof Player p)) {
            send(sender, "players-only");
            return true;
        }
        if (sign) doSign(p, String.join(" ", args));
        else doUnsign(p);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return List.of();
    }

    private void doSign(Player p, String rawText) {
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item.getType().isAir()) { send(p, "no-item"); return; }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) { send(p, "no-item"); return; }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        List<Sig> sigs = sigs(pdc);
        String uuid = p.getUniqueId().toString();
        if (sigs.stream().anyMatch(x -> x.uuid().equals(uuid))) { send(p, "already-signed"); return; }
        int maxSigs = getConfig().getInt("max-signatures-per-item", 2);
        if (sigs.size() >= maxSigs && !p.hasPermission("chickensmp.sign.bypass")) {
            send(p, "max-signatures", Placeholder.unparsed("max", String.valueOf(maxSigs)));
            return;
        }
        String text = clean(rawText);
        int max = getConfig().getInt("max-message-length", 40);
        if (text.length() > max) { send(p, "too-long", Placeholder.unparsed("max", String.valueOf(max))); return; }

        int limit = limit(p);
        String day = today();
        int used = used(p, day);
        if (limit >= 0 && used >= limit) { send(p, "limit-reached", Placeholder.unparsed("limit", String.valueOf(limit))); return; }

        String date = LocalDate.now(zone()).format(DateTimeFormatter.ofPattern(getConfig().getString("date-format", "dd.MM.yyyy")));
        TagResolver tags = TagResolver.resolver(
                Placeholder.component("rank", rankComponent(p)),
                Placeholder.unparsed("player", p.getName()),
                Placeholder.unparsed("message", text),
                Placeholder.unparsed("date", date));
        String[] frame = frame(rankGroup(p));
        List<Component> added = new ArrayList<>();
        for (String line : getConfig().getStringList("lore")) {
            if (line.contains("<message>") && text.isEmpty()) continue;
            line = line.replace("<frame>", frame[0]).replace("</frame>", frame[1]);
            added.add(mm.deserialize(line, tags).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.addAll(added);
        meta.lore(lore);
        sigs.add(new Sig(uuid, p.getName(), added.size()));
        saveSigs(pdc, sigs);
        item.setItemMeta(meta);
        p.getInventory().setItemInMainHand(item);

        if (limit >= 0) {
            setUsed(p, day, used + 1);
            send(p, "signed", Placeholder.unparsed("left", String.valueOf(limit - used - 1)));
        } else {
            send(p, "signed-unlimited");
        }
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
        getLogger().info(p.getName() + " signed " + item.getType() + " [" + rankGroup(p) + "] #" + sigs.size() + (text.isEmpty() ? "" : " (\"" + text + "\")"));
    }

    private void doUnsign(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        ItemMeta meta = item.getType().isAir() ? null : item.getItemMeta();
        List<Sig> sigs = meta == null ? List.of() : sigs(meta.getPersistentDataContainer());
        if (sigs.isEmpty()) { send(p, "not-signed"); return; }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        sigs = new ArrayList<>(sigs);
        int idx = -1;
        for (int i = 0; i < sigs.size(); i++) if (sigs.get(i).uuid().equals(p.getUniqueId().toString())) idx = i;
        if (idx < 0) {
            if (!p.hasPermission("chickensmp.sign.admin")) { send(p, "not-yours"); return; }
            idx = sigs.size() - 1; // admins remove the newest signature
        }
        // Signature blocks are appended in order at the end of the lore.
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        int after = 0;
        for (int i = idx + 1; i < sigs.size(); i++) after += sigs.get(i).lines();
        int end = Math.max(0, lore.size() - after);
        int start = Math.max(0, end - sigs.get(idx).lines());
        lore.subList(start, end).clear();
        meta.lore(lore.isEmpty() ? null : lore);
        sigs.remove(idx);
        saveSigs(pdc, sigs);
        item.setItemMeta(meta);
        p.getInventory().setItemInMainHand(item);
        send(p, "unsigned");
    }

    record Sig(String uuid, String name, int lines) { }

    /** All signatures on an item, oldest first (also reads items signed by v1.0/1.1). */
    private List<Sig> sigs(PersistentDataContainer pdc) {
        List<Sig> out = new ArrayList<>();
        List<String> raw = pdc.get(keySigs, PersistentDataType.LIST.strings());
        if (raw != null) {
            for (String r : raw) {
                String[] a = r.split(";", 3);
                if (a.length == 3) try { out.add(new Sig(a[0], a[1], Integer.parseInt(a[2]))); } catch (NumberFormatException ignored) { }
            }
        } else if (pdc.has(keyBy, PersistentDataType.STRING)) {
            out.add(new Sig(pdc.get(keyBy, PersistentDataType.STRING),
                    pdc.getOrDefault(keyName, PersistentDataType.STRING, "?"),
                    pdc.getOrDefault(keyLines, PersistentDataType.INTEGER, 0)));
        }
        return out;
    }

    private void saveSigs(PersistentDataContainer pdc, List<Sig> sigs) {
        pdc.remove(keyBy);
        pdc.remove(keyName);
        pdc.remove(keyLines);
        if (sigs.isEmpty()) pdc.remove(keySigs);
        else pdc.set(keySigs, PersistentDataType.LIST.strings(), sigs.stream().map(x -> x.uuid() + ";" + x.name() + ";" + x.lines()).toList());
    }

    /** -1 = unlimited, otherwise the highest chickensmp.sign.limit.N (at least default-limit). */
    private int limit(Player p) {
        if (p.hasPermission("chickensmp.sign.unlimited")) return -1;
        int best = getConfig().getInt("default-limit", 1);
        for (PermissionAttachmentInfo pi : p.getEffectivePermissions()) {
            String n = pi.getPermission().toLowerCase();
            if (!pi.getValue() || !n.startsWith(LIMIT_PREFIX)) continue;
            try { best = Math.max(best, Integer.parseInt(n.substring(LIMIT_PREFIX.length()))); } catch (NumberFormatException ignored) { }
        }
        return best;
    }

    /** The signer's LuckPerms group with the highest weight, or "default" without LuckPerms. */
    private String rankGroup(Player p) {
        if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                String g = LpHook.topGroup(p);
                if (g != null) return g;
            } catch (Throwable t) {
                getLogger().warning("LuckPerms lookup failed: " + t);
            }
        }
        return "default";
    }

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();

    /** The signer's rank prefix from LuckPerms plus a space, or nothing. */
    private Component rankComponent(Player p) {
        if (!getConfig().getBoolean("show-rank", true) || !getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            return Component.empty();
        }
        try {
            String prefix = LpHook.rankPrefix(p);
            if (getConfig().getBoolean("debug", false)) getLogger().info("rank prefix of " + p.getName() + ": " + prefix));
            if (prefix == null) return Component.empty();
            prefix = prefix.replace('\u00a7', '&').stripTrailing();
            return Component.empty().append(LEGACY.deserialize(prefix)).append(Component.text(" "));
        } catch (Throwable t) {
            getLogger().warning("LuckPerms prefix lookup failed: " + t);
            return Component.empty();
        }
    }

    /** Opening and closing MiniMessage tag for the frame colour of a group. */
    private String[] frame(String group) {
        String colors = null, fallback = "#aaaaaa";
        for (Map<?, ?> m : getConfig().getMapList("frame-colors")) {
            String g = String.valueOf(m.get("group"));
            String c = String.valueOf(m.get("colors"));
            if (g.equalsIgnoreCase(group)) colors = c;
            if (g.equalsIgnoreCase("default")) fallback = c;
        }
        if (colors == null) colors = fallback;
        String[] stops = colors.split(":");
        if (stops.length < 2) return new String[]{"<color:" + stops[0] + ">", "</color>"};
        return new String[]{"<gradient:" + colors + ">", "</gradient>"};
    }

    private ZoneId zone() {
        try { return ZoneId.of(getConfig().getString("timezone", "Europe/Berlin")); } catch (Exception e) { return ZoneId.of("Europe/Berlin"); }
    }

    private String today() { return LocalDate.now(zone()).toString(); }

    private int used(Player p, String day) {
        String k = p.getUniqueId().toString();
        return day.equals(uses.getString(k + ".day")) ? uses.getInt(k + ".count") : 0;
    }

    private void setUsed(Player p, String day, int count) {
        String k = p.getUniqueId().toString();
        uses.set(k + ".day", day);
        uses.set(k + ".count", count);
        try { uses.save(usesFile); } catch (IOException e) { getLogger().warning("Could not save uses.yml: " + e.getMessage()); }
    }

    /** Plain text only: drop colour codes and control characters. */
    private static String clean(String s) {
        String plain = s.replaceAll("[§&][0-9a-fk-orA-FK-OR#x]", "").replaceAll("\\p{Cntrl}", "");
        return plain.trim().replaceAll("\\s+", " ");
    }

    private void send(CommandSender to, String key, TagResolver... tags) {
        String msg = getConfig().getString("messages." + key, key);
        to.sendMessage(mm.deserialize(getConfig().getString("prefix", "") + msg, TagResolver.resolver(tags)));
    }
}
