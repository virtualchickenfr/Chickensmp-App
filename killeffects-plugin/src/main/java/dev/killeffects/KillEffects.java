/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.milkbowl.vault.economy.Economy
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.RegisteredServiceProvider
 *  org.bukkit.plugin.java.JavaPlugin
 */
package dev.killeffects;

import dev.killeffects.CollectionMenu;
import dev.killeffects.Crate;
import dev.killeffects.CratesMenu;
import dev.killeffects.Fx;
import dev.killeffects.Gui;
import dev.killeffects.KillEffect;
import dev.killeffects.KillEffectsExpansion;
import dev.killeffects.Rarity;
import dev.killeffects.RollMenu;
import dev.killeffects.Store;
import dev.killeffects.Text;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class KillEffects
extends JavaPlugin
implements Listener,
TabExecutor {
    static final String VERSION = "4.4.0";
    static final UUID PACK_ID = UUID.nameUUIDFromBytes("ChickenSMPKillEffects".getBytes(StandardCharsets.UTF_8));
    final SecureRandom random = new SecureRandom();
    private final Map<UUID, Store.Profile> profiles = new ConcurrentHashMap<UUID, Store.Profile>();
    private final Map<UUID, Long> previewCooldown = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> killCooldown = new ConcurrentHashMap<UUID, Long>();
    private Map<String, KillEffect> effects = new LinkedHashMap<String, KillEffect>();
    private final Map<Rarity, List<KillEffect>> byRarity = new EnumMap<Rarity, List<KillEffect>>(Rarity.class);
    private final Map<String, Crate> crates = new LinkedHashMap<String, Crate>();
    private Economy economy;
    private Store store;

    public void onEnable() {
        RegisteredServiceProvider registeredServiceProvider;
        this.saveDefaultConfig();
        if (Bukkit.getPluginManager().isPluginEnabled("Vault") && (registeredServiceProvider = Bukkit.getServicesManager().getRegistration(Economy.class)) != null) {
            this.economy = (Economy)registeredServiceProvider.getProvider();
        }
        this.store = new Store(this.getDataFolder(), this.getLogger());
        try {
            this.store.open();
            this.profiles.putAll(this.store.loadAll());
        }
        catch (Exception exception) {
            this.getLogger().severe("Could not open the database: " + exception.getMessage());
            Bukkit.getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        this.load();
        this.migrate();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        if (this.getCommand("killeffects") != null) {
            this.getCommand("killeffects").setExecutor((CommandExecutor)this);
            this.getCommand("killeffects").setTabCompleter((TabCompleter)this);
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                new KillEffectsExpansion(this).register();
            }
            catch (Throwable throwable) {
                this.getLogger().warning("PlaceholderAPI hook failed: " + throwable.getMessage());
            }
        }
        this.getLogger().info("ChickenSMPKillEffects " + VERSION + " | " + this.effects.size() + " effects | " + this.crates.size() + " crates | economy: " + (this.economy == null ? "none (crates only with keys)" : this.economy.getName()));
    }

    public void onDisable() {
        if (this.store != null) {
            this.store.close();
        }
    }

    void load() {
        this.reloadConfig();
        Map<String, KillEffect> map = new LinkedHashMap<String, KillEffect>(Effects3.all());
        this.effects = new LinkedHashMap<String, KillEffect>();
        for (KillEffect killEffect : map.values()) {
            String string = "effects." + killEffect.id() + ".";
            if (!this.getConfig().getBoolean(string + "enabled", true)) continue;
            String name = this.getConfig().getString(string + "name");
            Rarity rar = Rarity.parse(this.getConfig().getString(string + "rarity"));
            String desc = this.getConfig().getString(string + "description");
            this.effects.put(killEffect.id(), new KillEffect(killEffect.id(), name == null ? killEffect.name() : name, rar == null ? killEffect.rarity() : rar, desc == null ? killEffect.description() : desc, killEffect.fallback(), killEffect.play()));
        }
        this.byRarity.clear();
        for (Rarity r : Rarity.values()) {
            this.byRarity.put(r, new ArrayList<KillEffect>());
        }
        for (KillEffect killEffect : this.effects.values()) {
            if (Effects3.EXCLUSIVE.contains(killEffect.id())) continue;
            this.byRarity.get(killEffect.rarity()).add(killEffect);
        }
        this.crates.clear();
        ConfigurationSection configurationSection = this.getConfig().getConfigurationSection("crates");
        if (configurationSection != null) {
            for (String string : configurationSection.getKeys(false)) {
                String pre = "crates." + string + ".";
                EnumMap<Rarity, Double> chances = new EnumMap<Rarity, Double>(Rarity.class);
                for (Rarity rarity : Rarity.values()) {
                    double d = this.getConfig().getDouble(pre + "chances." + rarity.key(), 0.0);
                    if (!(d > 0.0)) continue;
                    chances.put(rarity, d);
                }
                this.crates.put(string, new Crate(string, this.getConfig().getString(pre + "name", string), this.getConfig().getString(pre + "texture", string), this.getConfig().getDouble(pre + "price", 0.0), chances, this.getConfig().getStringList(pre + "lore")));
            }
        }
    }

    Map<String, KillEffect> effects() {
        return this.effects;
    }

    Map<Rarity, List<KillEffect>> byRarity() {
        return this.byRarity;
    }

    Map<String, Crate> crates() {
        return this.crates;
    }

    Economy economy() {
        return this.economy;
    }

    boolean customModels() {
        return this.getConfig().getBoolean("custom-models", true);
    }

    Store.Profile profile(UUID uUID) {
        return this.profiles.get(uUID);
    }

    Store.Profile profileOrCreate(UUID uUID2, String string) {
        Store.Profile profile = this.profiles.computeIfAbsent(uUID2, uUID -> new Store.Profile());
        if (string != null) {
            profile.name = string;
        }
        return profile;
    }

    /** Follows the rename chain (v1 -> v2 -> v4) to an effect that still exists, or null. */
    private String migrated(String id) {
        String cur = id;
        for (int i = 0; i < 5 && cur != null && !this.effects.containsKey(cur); i++) {
            String next = Effects3.MIGRATE.get(cur);
            if (next == null) next = Effects3.LEGACY.get(cur);
            cur = next;
        }
        return cur != null && this.effects.containsKey(cur) ? cur : null;
    }

    /** Old effects were removed or replaced: give the new effect of the same rarity instead. */
    private void migrate() {
        int changed = 0;
        for (Map.Entry<UUID, Store.Profile> e : this.profiles.entrySet()) {
            Store.Profile p = e.getValue();
            boolean dirty = false;
            for (String old : new ArrayList<String>(p.owned)) {
                String neu = migrated(old);
                if (neu == null || neu.equals(old)) continue;
                p.owned.remove(old);
                p.owned.add(neu);
                dirty = true;
            }
            if (p.selected != null) {
                String neu = migrated(p.selected);
                if (neu != null && !neu.equals(p.selected)) {
                    p.selected = neu;
                    dirty = true;
                }
            }
            if (dirty) {
                ++changed;
                this.save(e.getKey());
            }
        }
        if (changed > 0) this.getLogger().info("Moved " + changed + " player(s) to the new kill effects.");
    }

    void save(UUID uUID) {
        Store.Profile profile = this.profiles.get(uUID);
        if (profile != null) {
            this.store.save(uUID, profile);
        }
    }

    boolean owns(Player player, KillEffect killEffect) {
        Store.Profile profile = this.profiles.get(player.getUniqueId());
        if (profile != null && profile.owned.contains(killEffect.id())) {
            return true;
        }
        if (Effects3.EXCLUSIVE.contains(killEffect.id())) return false;
        return player.hasPermission(killEffect.permission()) || player.hasPermission("killeffects.rarity." + killEffect.rarity().key()) || player.hasPermission("killeffects.effect.*");
    }

    int ownedCount(Player player) {
        int n = 0;
        for (KillEffect killEffect : this.effects.values()) {
            if (!this.owns(player, killEffect)) continue;
            ++n;
        }
        return n;
    }

    KillEffect selected(Player player) {
        Store.Profile profile = this.profiles.get(player.getUniqueId());
        if (profile == null || profile.selected == null) {
            return null;
        }
        KillEffect killEffect = this.effects.get(profile.selected);
        return killEffect != null && this.owns(player, killEffect) ? killEffect : null;
    }

    String raw(String string) {
        String string2 = this.getConfig().getString(string);
        return string2 == null ? string : string2;
    }

    String msg(String string, Object ... objectArray) {
        return Text.color(KillEffects.fill(this.raw("messages." + string).replace("{prefix}", this.raw("messages.prefix")), objectArray));
    }

    String gui(String string, Object ... objectArray) {
        return Text.color(KillEffects.fill(this.raw("gui." + string), objectArray));
    }

    List<String> guiList(String string, Object ... objectArray) {
        ArrayList<String> arrayList = new ArrayList<String>();
        for (String string2 : this.getConfig().getStringList("gui." + string)) {
            arrayList.add(Text.color(KillEffects.fill(string2, objectArray)));
        }
        return arrayList;
    }

    static String fill(String string, Object ... objectArray) {
        int n = 0;
        while (n + 1 < objectArray.length) {
            string = string.replace("{" + String.valueOf(objectArray[n]) + "}", String.valueOf(objectArray[n + 1]));
            n += 2;
        }
        return string;
    }

    void send(CommandSender commandSender, String string, Object ... objectArray) {
        String string2 = this.msg(string, objectArray);
        if (!string2.isEmpty()) {
            commandSender.sendMessage(string2);
        }
        if (string.startsWith("error.") && commandSender instanceof Player) {
            Player player = (Player)commandSender;
            this.sound(player, "error");
        }
    }

    void broadcast(String string) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(string);
        }
        Bukkit.getConsoleSender().sendMessage(string);
    }

    String money(double d) {
        if (this.economy != null && this.getConfig().getBoolean("use-economy-format", false)) {
            try {
                return this.economy.format(d);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return this.raw("money-format").replace("{amount}", new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(d));
    }

    static String num(long l) {
        return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(l);
    }

    void sound(Player player, String string) {
        this.playSound(player, this.getConfig().getString("sounds." + string), 0.0f);
    }

    void playSound(Player player, String string, float f) {
        if (string == null || string.isBlank() || player == null) {
            return;
        }
        String[] stringArray = string.split("\\|");
        try {
            player.playSound(player.getLocation(), stringArray[0].trim(), stringArray.length > 1 ? Float.parseFloat(stringArray[1].trim()) : 1.0f, (stringArray.length > 2 ? Float.parseFloat(stringArray[2].trim()) : 1.0f) + f);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    ItemStack icon(KillEffect killEffect, boolean bl, String string, List<String> list) {
        return Gui.item(bl ? killEffect.fallback() : Material.GRAY_DYE, string, list);
    }

    ItemStack textured(String string, Material material, String string2, List<String> list) {
        ItemStack itemStack = Gui.item(this.customModels() ? Material.PAPER : material, string2, list);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null && this.customModels()) {
            itemMeta.setItemModel(new NamespacedKey("killfx", string));
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    /** EconomyCore style item name: rarity colour, bold, upper case. */
    static String upper(KillEffect e) {
        return e.rarity().color + "&l" + e.name().toUpperCase(Locale.ROOT);
    }

    List<String> effectLore(KillEffect killEffect) {
        return this.guiList("effect-lore", "rarity", killEffect.rarity().label(), "description", killEffect.description());
    }

    void play(KillEffect killEffect, Location location) {
        this.play(killEffect, location, null, null);
    }

    void play(KillEffect killEffect, Location location, Player victim, Location faceTarget) {
        try {
            if (Trace.ON) Trace.name = killEffect.id();
            Fx fx = new Fx(this, location, null, this.customModels());
            fx.victim = victim;
            fx.faceTarget = faceTarget;
            killEffect.play().accept(fx);
        }
        catch (Exception exception) {
            this.getLogger().warning("Kill effect " + killEffect.id() + " failed: " + String.valueOf(exception));
        }
    }

    /** The Fx of the last preview (to know how long it runs). */
    Fx lastPreview;

    /** How long the last preview runs, in ticks (it keeps growing while the effect schedules more steps). */
    int lastPreviewTicks() {
        return this.lastPreview == null ? 60 : Math.max(20, this.lastPreview.endTick);
    }

    boolean preview(Player player, KillEffect killEffect) {
        long l = System.currentTimeMillis();
        Long l2 = this.previewCooldown.get(player.getUniqueId());
        long l3 = this.getConfig().getLong("preview-cooldown-seconds", 4L) * 1000L;
        if (l2 != null && l - l2 < l3) {
            this.send((CommandSender)player, "error.preview-cooldown", "seconds", (l3 - (l - l2) + 999L) / 1000L);
            return false;
        }
        this.previewCooldown.put(player.getUniqueId(), l);
        Location location = player.getLocation();
        double d = Math.toRadians(location.getYaw());
        Location location2 = location.clone().add(-Math.sin(d) * 3.5, 0.0, Math.cos(d) * 3.5);
        location2.setY(Math.floor(location.getY()));
        try {
            Fx fx = new Fx(this, location2, player, this.customModels());
            killEffect.play().accept(fx);
            this.lastPreview = fx;
        }
        catch (Exception exception) {
            this.getLogger().warning("Kill effect preview " + killEffect.id() + " failed: " + String.valueOf(exception));
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFireworkDamage(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof org.bukkit.entity.Firework fw && fw.getScoreboardTags().contains("killeffects")) e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent playerDeathEvent) {
        Player player = playerDeathEvent.getEntity();
        Player player2 = player.getKiller();
        if (player2 == null || player2 == player) {
            return;
        }
        if (!player2.hasPermission("killeffects.use")) {
            return;
        }
        Store.Profile profile = this.profileOrCreate(player2.getUniqueId(), player2.getName());
        ++profile.kills;
        KillEffect killEffect = this.selected(player2);
        if (killEffect != null) {
            long l = System.currentTimeMillis();
            Long l2 = this.killCooldown.get(player2.getUniqueId());
            if (l2 == null || l - l2 >= this.getConfig().getLong("kill-cooldown-millis", 500L)) {
                this.killCooldown.put(player2.getUniqueId(), l);
                this.play(killEffect, player.getLocation(), player, player2.getLocation());
            }
        }
        this.save(player2.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();
        this.profileOrCreate(player.getUniqueId(), player.getName());
        Skin.load(this, player);
        String string = this.getConfig().getString("resource-pack.url");
        if (string != null && !string.isBlank()) {
            byte[] byArray = null;
            String string2 = this.getConfig().getString("resource-pack.sha1");
            if (string2 != null && string2.length() == 40) {
                try {
                    byArray = HexFormat.of().parseHex(string2);
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    // empty catch block
                }
            }
            byte[] byArray2 = byArray;
            String string3 = Text.color(this.raw("resource-pack.prompt"));
            boolean bl = this.getConfig().getBoolean("resource-pack.required", false);
            Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
                if (player.isOnline()) {
                    player.addResourcePack(PACK_ID, string, byArray2, string3, bl);
                }
            }, 20L);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        InventoryHolder inventoryHolder = inventoryClickEvent.getInventory().getHolder();
        if (!(inventoryHolder instanceof Gui)) {
            return;
        }
        Gui gui = (Gui)inventoryHolder;
        inventoryClickEvent.setCancelled(true);
        HumanEntity humanEntity = inventoryClickEvent.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        inventoryHolder = (Player)humanEntity;
        int n = inventoryClickEvent.getRawSlot();
        if (n < 0 || n >= inventoryClickEvent.getInventory().getSize()) {
            return;
        }
        gui.click((Player)inventoryHolder, n, inventoryClickEvent.isRightClick(), inventoryClickEvent.isShiftClick());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent inventoryDragEvent) {
        if (inventoryDragEvent.getInventory().getHolder() instanceof Gui) {
            inventoryDragEvent.setCancelled(true);
        }
    }

    void select(Player player, KillEffect killEffect) {
        Store.Profile profile = this.profileOrCreate(player.getUniqueId(), player.getName());
        if (killEffect == null) {
            profile.selected = null;
            this.send((CommandSender)player, "deselected", new Object[0]);
        } else {
            if (!this.owns(player, killEffect)) {
                this.send((CommandSender)player, "error.locked", "effect", Text.color(killEffect.colored()));
                return;
            }
            profile.selected = killEffect.id();
            this.send((CommandSender)player, "selected", "effect", Text.color(killEffect.colored()));
        }
        this.sound(player, "select");
        this.save(player.getUniqueId());
    }

    Prize openCrate(Player player, Crate crate, boolean bl) {
        KillEffect killEffect;
        Store.Profile profile = this.profileOrCreate(player.getUniqueId(), player.getName());
        if (!bl) {
            if (profile.keys(crate.id()) > 0) {
                profile.keys.put(crate.id(), profile.keys(crate.id()) - 1);
            } else if (crate.price() > 0.0 && this.economy != null) {
                if (!this.economy.has((OfflinePlayer)player, crate.price())) {
                    this.send((CommandSender)player, "error.not-enough-money", "amount", this.money(crate.price()));
                    return null;
                }
                if (!this.economy.withdrawPlayer((OfflinePlayer)player, crate.price()).transactionSuccess()) {
                    this.send((CommandSender)player, "error.economy", new Object[0]);
                    return null;
                }
            } else {
                this.send((CommandSender)player, "error.no-key", "crate", Text.color(crate.name()));
                return null;
            }
        }
        if ((killEffect = crate.roll(this.random, this.byRarity)) == null) {
            this.send((CommandSender)player, "error.crate-empty", new Object[0]);
            return null;
        }
        ++profile.opened;
        boolean bl2 = this.owns(player, killEffect);
        double d = 0.0;
        if (bl2) {
            double d2 = Math.max(0.0, Math.min(100.0, this.getConfig().getDouble("duplicate-refund-percent", 25.0)));
            d = Math.floor(crate.price() * d2 / 100.0);
            if (d > 0.0 && this.economy != null) {
                this.economy.depositPlayer((OfflinePlayer)player, d);
            } else {
                d = 0.0;
            }
        } else {
            profile.owned.add(killEffect.id());
        }
        this.save(player.getUniqueId());
        return new Prize(killEffect, bl2, d);
    }

    void announce(Player player, Crate crate, Prize prize) {
        Rarity rarity;
        KillEffect killEffect = prize.effect();
        if (player.isOnline()) {
            if (prize.duplicate()) {
                this.send((CommandSender)player, prize.refund() > 0.0 ? "won-duplicate" : "won-duplicate-nothing", "effect", Text.color(killEffect.colored()), "money", this.money(prize.refund()));
            } else {
                this.send((CommandSender)player, "won", "effect", Text.color(killEffect.colored()), "rarity", Text.color(killEffect.rarity().label()));
            }
        }
        if ((rarity = Rarity.parse(this.getConfig().getString("broadcast-from", "LEGENDARY"))) != null && killEffect.rarity().ordinal() >= rarity.ordinal()) {
            this.broadcast(this.msg("broadcast-win", "player", player.getName(), "effect", Text.color(killEffect.colored()), "rarity", Text.color(killEffect.rarity().label()), "crate", Text.color(crate.name())));
        }
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        Player player2 = commandSender instanceof Player ? (Player)commandSender : null;
        String object = stringArray.length == 0 ? "" : stringArray[0].toLowerCase(Locale.ROOT);
        if (List.of("give", "take", "keys", "key", "open", "play", "reload", "info").contains(object)) {
            if (!commandSender.hasPermission("killeffects.admin")) {
                this.send(commandSender, "error.no-permission", new Object[0]);
                return true;
            }
            this.admin(commandSender, (String)object, stringArray);
            return true;
        }
        if (player2 == null) {
            this.send(commandSender, "error.players-only", new Object[0]);
            return true;
        }
        if (!player2.hasPermission("killeffects.use")) {
            this.send((CommandSender)player2, "error.no-permission", new Object[0]);
            return true;
        }
        switch (object) {
            case "": 
            case "menu": 
            case "collection": {
                CollectionMenu.open(this, player2, 0, null);
                break;
            }
            case "crates": 
            case "crate": 
            case "mystery": 
            case "gamble": {
                CratesMenu.open(this, player2);
                break;
            }
            case "select": {
                if (stringArray.length < 2) {
                    this.send((CommandSender)player2, "usage", "usage", "/" + string + " select <effect|none>");
                    return true;
                }
                if (stringArray[1].equalsIgnoreCase("none")) {
                    this.select(player2, null);
                    return true;
                }
                KillEffect killEffect = this.effects.get(stringArray[1].toLowerCase(Locale.ROOT));
                if (killEffect == null) {
                    this.send((CommandSender)player2, "error.unknown-effect", "input", stringArray[1]);
                    return true;
                }
                this.select(player2, killEffect);
                break;
            }
            case "preview": {
                if (stringArray.length < 2) {
                    this.send((CommandSender)player2, "usage", "usage", "/" + string + " preview <effect>");
                    return true;
                }
                KillEffect killEffect = this.effects.get(stringArray[1].toLowerCase(Locale.ROOT));
                if (killEffect == null) {
                    this.send((CommandSender)player2, "error.unknown-effect", "input", stringArray[1]);
                    return true;
                }
                if (!player2.hasPermission("killeffects.preview")) {
                    this.send((CommandSender)player2, "error.no-permission", new Object[0]);
                    return true;
                }
                if (!this.preview(player2, killEffect)) break;
                this.send((CommandSender)player2, "preview", "effect", Text.color(killEffect.colored()));
                break;
            }
            case "help": {
                this.help((CommandSender)player2, string);
                break;
            }
            default: {
                this.send((CommandSender)player2, "error.unknown-command", "input", stringArray[0]);
            }
        }
        return true;
    }

    private void help(CommandSender commandSender, String string) {
        for (String string2 : this.getConfig().getStringList("messages.help")) {
            commandSender.sendMessage(Text.color(string2.replace("{label}", string)));
        }
        if (commandSender.hasPermission("killeffects.admin")) {
            for (String string2 : this.getConfig().getStringList("messages.help-admin")) {
                commandSender.sendMessage(Text.color(string2.replace("{label}", string)));
            }
        }
    }

    private List<UUID> targets(CommandSender commandSender, String string) {
        ArrayList<UUID> arrayList = new ArrayList<UUID>();
        if (string.equals("*") || string.equalsIgnoreCase("@a")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.profileOrCreate(player.getUniqueId(), player.getName());
                arrayList.add(player.getUniqueId());
            }
            return arrayList;
        }
        Player player = Bukkit.getPlayerExact((String)string);
        if (player != null) {
            this.profileOrCreate(player.getUniqueId(), player.getName());
            arrayList.add(player.getUniqueId());
            return arrayList;
        }
        for (Map.Entry<UUID, Store.Profile> entry : this.profiles.entrySet()) {
            if (!string.equalsIgnoreCase(entry.getValue().name)) continue;
            arrayList.add(entry.getKey());
            return arrayList;
        }
        this.send(commandSender, "error.player-not-found", "player", string);
        return arrayList;
    }

    private List<KillEffect> resolve(String string) {
        if (string.equalsIgnoreCase("all") || string.equals("*")) {
            ArrayList<KillEffect> all = new ArrayList<KillEffect>(this.effects.values());
            all.removeIf(e -> Effects3.EXCLUSIVE.contains(e.id()));
            return all;
        }
        Rarity rarity = Rarity.parse(string);
        if (rarity != null) {
            return new ArrayList<KillEffect>((Collection)this.byRarity.get((Object)rarity));
        }
        KillEffect killEffect = this.effects.get(string.toLowerCase(Locale.ROOT));
        return killEffect == null ? List.of() : List.of(killEffect);
    }

    private String who(List<UUID> list) {
        if (list.size() != 1) {
            return list.size() + " players";
        }
        Store.Profile profile = this.profiles.get(list.get(0));
        return profile == null || profile.name == null ? "?" : profile.name;
    }

    private void admin(CommandSender commandSender, String string, String[] stringArray) {
        switch (string) {
            case "give": 
            case "take": {
                if (stringArray.length < 3) {
                    this.send(commandSender, "usage", "usage", "/ke " + string + " <player|*> <effect|rarity|all>");
                    return;
                }
                List<UUID> list = this.targets(commandSender, stringArray[1]);
                if (list.isEmpty()) {
                    return;
                }
                List<KillEffect> list2 = this.resolve(stringArray[2]);
                if (list2.isEmpty()) {
                    this.send(commandSender, "error.unknown-effect", "input", stringArray[2]);
                    return;
                }
                for (UUID uUID : list) {
                    Store.Profile profile = this.profiles.get(uUID);
                    Player player;
                    java.util.Iterator<KillEffect> it = list2.iterator();
                    while (it.hasNext()) {
                        KillEffect killEffect = it.next();
                        if (string.equals("give")) {
                            profile.owned.add(killEffect.id());
                            continue;
                        }
                        profile.owned.remove(killEffect.id());
                    }
                    if (!(!string.equals("take") || profile.selected == null || profile.owned.contains(profile.selected) || (player = Bukkit.getPlayer((UUID)uUID)) != null && this.owns(player, this.effects.getOrDefault(profile.selected, list2.get(0))))) {
                        profile.selected = null;
                    }
                    this.save(uUID);
                    player = Bukkit.getPlayer((UUID)uUID);
                    if (player == null || !string.equals("give")) continue;
                    this.send((CommandSender)player, "received", "amount", list2.size(), "what", list2.size() == 1 ? Text.color(list2.get(0).colored()) : stringArray[2]);
                    this.sound(player, "unlock");
                }
                this.send(commandSender, string.equals("give") ? "admin-given" : "admin-taken", "amount", list2.size(), "player", this.who(list));
                break;
            }
            case "keys": 
            case "key": {
                if (stringArray.length < 4) {
                    this.send(commandSender, "usage", "usage", "/ke keys <player|*> <crate|all> <amount>");
                    return;
                }
                List<UUID> list = this.targets(commandSender, stringArray[1]);
                if (list.isEmpty()) {
                    return;
                }
                ArrayList<Crate> arrayList = new ArrayList<Crate>();
                if (stringArray[2].equalsIgnoreCase("all")) {
                    arrayList.addAll(this.crates.values());
                } else if (this.crates.containsKey(stringArray[2].toLowerCase(Locale.ROOT))) {
                    arrayList.add(this.crates.get(stringArray[2].toLowerCase(Locale.ROOT)));
                }
                if (arrayList.isEmpty()) {
                    this.send(commandSender, "error.unknown-crate", "input", stringArray[2]);
                    return;
                }
                int n = KillEffects.parseInt(stringArray[3]);
                for (UUID uUID : list) {
                    Store.Profile profile = this.profiles.get(uUID);
                    for (Crate crate : arrayList) {
                        profile.keys.put(crate.id(), Math.max(0, profile.keys(crate.id()) + n));
                    }
                    this.save(uUID);
                    Player player = Bukkit.getPlayer((UUID)uUID);
                    if (player == null || n <= 0) continue;
                    this.send((CommandSender)player, "keys-received", "amount", n, "crate", arrayList.size() == 1 ? Text.color(((Crate)arrayList.get(0)).name()) : "all crates");
                    this.sound(player, "unlock");
                }
                this.send(commandSender, "admin-keys", "amount", n, "player", this.who(list));
                break;
            }
            case "open": {
                if (stringArray.length < 3) {
                    this.send(commandSender, "usage", "usage", "/ke open <player> <crate>");
                    return;
                }
                Player player = Bukkit.getPlayerExact((String)stringArray[1]);
                if (player == null) {
                    this.send(commandSender, "error.player-not-found", "player", stringArray[1]);
                    return;
                }
                Crate crate = this.crates.get(stringArray[2].toLowerCase(Locale.ROOT));
                if (crate == null) {
                    this.send(commandSender, "error.unknown-crate", "input", stringArray[2]);
                    return;
                }
                Prize prize = this.openCrate(player, crate, true);
                if (prize == null) break;
                RollMenu.open(this, player, crate, prize);
                break;
            }
            case "play": {
                if (!(commandSender instanceof Player)) {
                    this.send(commandSender, "error.players-only", new Object[0]);
                    return;
                }
                Player player = (Player)commandSender;
                if (stringArray.length < 2) {
                    this.send(commandSender, "usage", "usage", "/ke play <effect>");
                    return;
                }
                KillEffect killEffect = this.effects.get(stringArray[1].toLowerCase(Locale.ROOT));
                if (killEffect == null) {
                    this.send(commandSender, "error.unknown-effect", "input", stringArray[1]);
                    return;
                }
                Location location = player.getLocation();
                double d = Math.toRadians(location.getYaw());
                Location at = location.clone().add(-Math.sin(d) * 3.5, 0.0, Math.cos(d) * 3.5);
                at.setY(Math.floor(location.getY()));
                this.play(killEffect, at, player, location);
                break;
            }
            case "info": {
                if (stringArray.length < 2) {
                    this.send(commandSender, "usage", "usage", "/ke info <player>");
                    return;
                }
                List<UUID> list = this.targets(commandSender, stringArray[1]);
                if (list.size() != 1) {
                    return;
                }
                Store.Profile profile = this.profiles.get(list.get(0));
                this.send(commandSender, "admin-info", "player", profile.name, "owned", profile.owned.size(), "total", this.effects.size(), "selected", profile.selected == null ? "-" : profile.selected, "kills", KillEffects.num(profile.kills), "opened", KillEffects.num(profile.opened), "keys", Store.encodeKeys(profile.keys).isEmpty() ? "-" : Store.encodeKeys(profile.keys));
                break;
            }
            case "reload": {
                this.load();
                this.send(commandSender, "reloaded", "effects", this.effects.size(), "crates", this.crates.size());
                break;
            }
        }
    }

    private static int parseInt(String string) {
        try {
            return Integer.parseInt(string);
        }
        catch (NumberFormatException numberFormatException) {
            return 0;
        }
    }

    public List<String> onTabComplete(CommandSender commandSender, Command command, String string, String[] stringArray) {
        ArrayList<String> arrayList = new ArrayList<String>();
        boolean bl = commandSender.hasPermission("killeffects.admin");
        String string3 = stringArray[0].toLowerCase(Locale.ROOT);
        if (stringArray.length == 1) {
            arrayList.addAll(List.of("crates", "mystery", "select", "preview", "help"));
            if (bl) {
                arrayList.addAll(List.of("give", "take", "keys", "open", "play", "info", "reload"));
            }
        } else if (stringArray.length == 2) {
            switch (string3) {
                case "select": 
                case "preview": 
                case "play": {
                    arrayList.addAll(this.effects.keySet());
                    if (!string3.equals("select")) break;
                    arrayList.add("none");
                    break;
                }
                case "give": 
                case "take": 
                case "keys": 
                case "open": 
                case "info": {
                    if (!bl) break;
                    arrayList.add("*");
                    Bukkit.getOnlinePlayers().forEach(player -> arrayList.add(player.getName()));
                    break;
                }
            }
        } else if (stringArray.length == 3 && bl) {
            switch (string3) {
                case "give": 
                case "take": {
                    arrayList.add("all");
                    for (Rarity rarity : Rarity.values()) {
                        arrayList.add(rarity.key());
                    }
                    arrayList.addAll(this.effects.keySet());
                    break;
                }
                case "keys": 
                case "open": {
                    arrayList.addAll(this.crates.keySet());
                    if (!string3.equals("keys")) break;
                    arrayList.add("all");
                    break;
                }
            }
        } else if (stringArray.length == 4 && bl && string3.equals("keys")) {
            arrayList.addAll(List.of("1", "5", "10"));
        }
        String string4 = stringArray[stringArray.length - 1].toLowerCase(Locale.ROOT);
        return arrayList.stream().filter(string2 -> string2.toLowerCase(Locale.ROOT).startsWith(string4)).limit(60L).toList();
    }

    record Prize(KillEffect effect, boolean duplicate, double refund) {
    }
}

