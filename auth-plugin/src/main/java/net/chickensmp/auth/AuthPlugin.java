package net.chickensmp.auth;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.retrooper.packetevents.PacketEvents;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthPlugin extends JavaPlugin implements Listener {
    private final Mojang mojang = new Mojang();
    private final Msg msg = new Msg();
    private PremiumHandshake handshake;
    private UserStore store;
    private boolean floodgate;

    /** Players who don't need a password this session (verified premium or Bedrock). */
    private final Set<UUID> trusted = ConcurrentHashMap.newKeySet();
    /** Cracked players who have logged in. */
    private final Set<UUID> authed = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> waitingSince = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> attempts = new ConcurrentHashMap<>();
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();
    private final Map<String, Deque<Long>> ipFails = new ConcurrentHashMap<>();
    private final Map<String, Long> ipBlocked = new ConcurrentHashMap<>();

    Msg msg() { return msg; }
    boolean failClosed() { return getConfig().getBoolean("fail-closed", true); }
    String floodgatePrefix() { return floodgate ? FloodgateHook.prefix() : ""; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try { LogFilter.install(); } catch (Throwable t) { getLogger().warning("Could not install log filter: " + t); }
        store = new UserStore(getDataFolder());
        floodgate = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        if (Bukkit.getOnlineMode()) {
            getLogger().warning("online-mode=true: cracked players can't join anyway. ChickenSMPAuth stays passive until you set online-mode=false.");
        }
        try {
            handshake = new PremiumHandshake(this, mojang);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        if (!Bukkit.getOnlineMode()) PacketEvents.getAPI().getEventManager().registerListener(handshake);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> handshake.cleanup(), 600L, 600L);
        for (Player p : Bukkit.getOnlinePlayers()) trusted.add(p.getUniqueId()); // after /reload: don't lock out
        getLogger().info("ChickenSMPAuth ready. Floodgate: " + floodgate + ", cracked accounts: " + store.count());
    }

    @Override
    public void onDisable() {
        if (store != null) store.saveNow();
    }

    boolean needsLogin(Player p) {
        if (Bukkit.getOnlineMode()) return false;
        UUID u = p.getUniqueId();
        return !trusted.contains(u) && !authed.contains(u);
    }

    // ---------------------------------------------------------------- connection

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (Bukkit.getOnlineMode()) return;
        String name = e.getName();
        String ip = e.getAddress().getHostAddress();
        Long until = ipBlocked.get(ip);
        if (until != null && until > System.currentTimeMillis()) { deny(e, "ipBlocked"); return; }

        Mojang.Verified v = handshake.consume(name);
        if (v != null) {
            PlayerProfile prof = Bukkit.createProfile(v.uuid(), v.name());
            for (Mojang.Prop pr : v.props()) prof.setProperty(new ProfileProperty(pr.name(), pr.value(), pr.signature()));
            e.setPlayerProfile(prof);
            trusted.add(v.uuid());
            store.premiumSeen(v.name(), v.uuid());
            return;
        }
        if (floodgate && FloodgateHook.isBedrock(e.getUniqueId())) { trusted.add(e.getUniqueId()); return; }

        if (!name.matches(getConfig().getString("allowed-names", "^[a-zA-Z0-9_]{3,16}$"))) { deny(e, "invalidName"); return; }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) { deny(e, "alreadyOnline"); return; }
        Mojang.Lookup l = mojang.lookup(name);
        if (l.kind() == Mojang.Kind.PREMIUM) { deny(e, "premiumName"); return; }
        if (l.kind() == Mojang.Kind.UNKNOWN && failClosed() && !store.registered(name)) { deny(e, "mojangDown"); return; }
        // cracked: allowed in, must log in
    }

    private void deny(AsyncPlayerPreLoginEvent e, String key) {
        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, msg.kick(key));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!needsLogin(p)) return;
        String ip = ip(p);
        long session = getConfig().getLong("session-minutes", 30) * 60_000L;
        if (session > 0 && store.registered(p.getName()) && ip.equals(store.lastIp(p.getName()))
                && System.currentTimeMillis() - store.lastLogout(p.getName()) < session) {
            authed.add(p.getUniqueId());
            store.loggedIn(p.getName(), ip);
            Bukkit.getScheduler().runTaskLater(this, () -> { if (p.isOnline()) msg.send(p, "sessionResumed"); }, 20L);
            return;
        }
        waitingSince.put(p.getUniqueId(), System.currentTimeMillis());
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 0, false, false, false));
        Bukkit.getScheduler().runTaskLater(this, () -> prompt(p), 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID u = p.getUniqueId();
        if (authed.remove(u)) { store.loggedOut(p.getName(), ip(p)); store.saveAsync(this); }
        trusted.remove(u);
        waitingSince.remove(u);
        attempts.remove(u);
        busy.remove(u);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long timeout = getConfig().getLong("login-timeout", 120) * 1000L;
        for (Map.Entry<UUID, Long> en : waitingSince.entrySet()) {
            Player p = Bukkit.getPlayer(en.getKey());
            if (p == null) { waitingSince.remove(en.getKey()); continue; }
            long age = now - en.getValue();
            if (age > timeout) { p.kick(msg.kick("timeout")); continue; }
            if ((age / 1000) % 10 == 0 && age > 1500) prompt(p);
        }
    }

    private void prompt(Player p) {
        if (!p.isOnline() || !needsLogin(p)) return;
        boolean reg = store.registered(p.getName());
        msg.send(p, reg ? "login" : "register");
        p.showTitle(Title.title(msg.c(p, reg ? "titleLogin" : "titleRegister"), msg.c(p, reg ? "subLogin" : "subRegister"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(11), Duration.ofMillis(300))));
    }

    private void unlock(Player p, String key) {
        UUID u = p.getUniqueId();
        authed.add(u);
        waitingSince.remove(u);
        attempts.remove(u);
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        p.clearTitle();
        store.loggedIn(p.getName(), ip(p));
        store.saveAsync(this);
        msg.send(p, key);
    }

    private static String ip(Player p) {
        return p.getAddress() != null && p.getAddress().getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "?";
    }

    private void ipFail(String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> d = ipFails.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (d) {
            d.addLast(now);
            while (!d.isEmpty() && now - d.peekFirst() > 600_000) d.pollFirst();
            if (d.size() >= getConfig().getInt("ip-fail-limit", 12)) { ipBlocked.put(ip, now + 600_000); d.clear(); }
        }
    }

    // ---------------------------------------------------------------- commands

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        String c = cmd.getName().toLowerCase();
        if (c.equals("chickenauth")) return admin(s, a);
        if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
        if (Bukkit.getOnlineMode() || trusted.contains(p.getUniqueId())) { msg.send(p, "noPremiumPw"); return true; }
        if (busy.contains(p.getUniqueId())) return true;
        switch (c) {
            case "register" -> register(p, a);
            case "login" -> login(p, a);
            case "changepassword" -> change(p, a);
        }
        return true;
    }

    private boolean pwOk(Player p, String pw) {
        int min = getConfig().getInt("password-min-length", 6);
        if (pw.length() < min) { msg.send(p, "pwShort", min); return false; }
        if (pw.equalsIgnoreCase(p.getName())) { msg.send(p, "pwName"); return false; }
        return true;
    }

    private void register(Player p, String[] a) {
        if (authed.contains(p.getUniqueId())) { msg.send(p, "alreadyIn"); return; }
        if (store.registered(p.getName())) { msg.send(p, "alreadyRegistered"); return; }
        if (a.length != 2) { msg.send(p, "usage.register"); return; }
        if (!a[0].equals(a[1])) { msg.send(p, "pwMismatch"); return; }
        if (!pwOk(p, a[0]) || a[0].length() > 64) return;
        String ip = ip(p);
        if (store.accountsForIp(ip) >= getConfig().getInt("max-accounts-per-ip", 3)) { msg.send(p, "tooManyAccounts"); return; }
        busy.add(p.getUniqueId());
        String pw = a[0];
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String h = Passwords.hash(pw);
            Bukkit.getScheduler().runTask(this, () -> {
                busy.remove(p.getUniqueId());
                if (!p.isOnline() || store.registered(p.getName())) return;
                store.register(p.getName(), h, ip);
                getLogger().info("Registered cracked account " + p.getName() + " from " + ip);
                unlock(p, "registered");
            });
        });
    }

    private void login(Player p, String[] a) {
        if (authed.contains(p.getUniqueId())) { msg.send(p, "alreadyIn"); return; }
        if (!store.registered(p.getName())) { msg.send(p, "notRegistered"); return; }
        if (a.length != 1) { msg.send(p, "usage.login"); return; }
        busy.add(p.getUniqueId());
        String stored = store.hash(p.getName());
        String pw = a[0];
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            boolean ok = Passwords.verify(pw, stored);
            Bukkit.getScheduler().runTask(this, () -> {
                busy.remove(p.getUniqueId());
                if (!p.isOnline()) return;
                if (ok) { unlock(p, "loggedIn"); return; }
                ipFail(ip(p));
                int n = attempts.merge(p.getUniqueId(), 1, Integer::sum);
                int left = getConfig().getInt("max-attempts", 5) - n;
                if (left <= 0) p.kick(msg.kick("tooMany"));
                else msg.send(p, "wrongPw", left);
            });
        });
    }

    private void change(Player p, String[] a) {
        if (!authed.contains(p.getUniqueId())) { msg.send(p, "mustLogin"); return; }
        if (a.length != 2) { msg.send(p, "usage.change"); return; }
        if (!pwOk(p, a[1]) || a[1].length() > 64) return;
        busy.add(p.getUniqueId());
        String stored = store.hash(p.getName());
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            boolean ok = Passwords.verify(a[0], stored);
            String h = ok ? Passwords.hash(a[1]) : null;
            Bukkit.getScheduler().runTask(this, () -> {
                busy.remove(p.getUniqueId());
                if (!p.isOnline()) return;
                if (!ok) { msg.send(p, "wrongPw", "–"); return; }
                store.setHash(p.getName(), h);
                store.saveAsync(this);
                msg.send(p, "pwChanged");
            });
        });
    }

    private boolean admin(CommandSender s, String[] a) {
        if (a.length == 0) {
            s.sendMessage("§b/chickenauth info <name> §7| §bunregister <name> §7| §bsetpassword <name> <pw> §7| §bforgetcache <name> §7| §breload");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "info" -> { if (a.length > 1) s.sendMessage("§b" + a[1] + "§7: " + store.info(a[1])); }
            case "unregister" -> {
                if (a.length < 2) return true;
                boolean had = store.unregister(a[1]);
                store.saveAsync(this);
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t != null && had && !trusted.contains(t.getUniqueId())) t.kick(msg.kick("timeout"));
                s.sendMessage(had ? "§aAccount " + a[1] + " removed." : "§c" + a[1] + " is not registered.");
            }
            case "setpassword" -> {
                if (a.length < 3) return true;
                String name = a[1], pw = a[2];
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    String h = Passwords.hash(pw);
                    Bukkit.getScheduler().runTask(this, () -> {
                        if (store.registered(name)) store.setHash(name, h); else store.register(name, h, "admin");
                        store.saveAsync(this);
                        s.sendMessage("§aPassword for " + name + " set.");
                    });
                });
            }
            case "forgetcache" -> { if (a.length > 1) { mojang.forget(a[1]); s.sendMessage("§aCache cleared for " + a[1]); } }
            case "reload" -> { reloadConfig(); s.sendMessage("§aConfig reloaded."); }
            default -> s.sendMessage("§cUnknown subcommand.");
        }
        return true;
    }

    // ---------------------------------------------------------------- restrictions while not logged in

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!needsLogin(e.getPlayer())) return;
        if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ() || e.getTo().getY() > e.getFrom().getY()) {
            e.setTo(e.getFrom().setDirection(e.getTo().getDirection()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) { if (needsLogin(e.getPlayer())) { e.setCancelled(true); msg.send(e.getPlayer(), "mustLogin"); } }

    private static final Set<String> ALLOWED = Set.of("/login", "/l", "/register", "/reg",
            "/chickensmpauth:login", "/chickensmpauth:l", "/chickensmpauth:register", "/chickensmpauth:reg");

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!needsLogin(e.getPlayer())) return;
        String first = e.getMessage().split(" ", 2)[0].toLowerCase();
        if (!ALLOWED.contains(first)) { e.setCancelled(true); msg.send(e.getPlayer(), "mustLogin"); }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTab(org.bukkit.event.server.TabCompleteEvent e) {
        if (e.getSender() instanceof Player p && needsLogin(p)) e.setCompletions(List.of());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandSend(PlayerCommandSendEvent e) {
        if (needsLogin(e.getPlayer())) e.getCommands().removeIf(c -> !ALLOWED.contains("/" + c));
    }

    @EventHandler(priority = EventPriority.LOWEST) public void onInteract(PlayerInteractEvent e) { if (needsLogin(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onInteractEntity(PlayerInteractEntityEvent e) { if (needsLogin(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onBreak(BlockBreakEvent e) { if (needsLogin(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onPlace(BlockPlaceEvent e) { if (needsLogin(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onDrop(PlayerDropItemEvent e) { if (needsLogin(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onSwap(PlayerSwapHandItemsEvent e) { if (needsLogin(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onConsume(PlayerItemConsumeEvent e) { if (needsLogin(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onPickup(EntityPickupItemEvent e) { if (e.getEntity() instanceof Player p && needsLogin(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onClick(InventoryClickEvent e) { if (e.getWhoClicked() instanceof Player p && needsLogin(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onOpen(InventoryOpenEvent e) { if (e.getPlayer() instanceof Player p && needsLogin(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onFood(FoodLevelChangeEvent e) { if (e.getEntity() instanceof Player p && needsLogin(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onTarget(EntityTargetLivingEntityEvent e) { if (e.getTarget() instanceof Player p && needsLogin(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onDamage(EntityDamageEvent e) { if (e.getEntity() instanceof Player p && needsLogin(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void onHit(EntityDamageByEntityEvent e) { if (e.getDamager() instanceof Player p && needsLogin(p)) e.setCancelled(true); }
}
