package net.chickensmp.auth;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

final class UserStore {
    private final File file;
    private final YamlConfiguration y;

    UserStore(File dir) {
        this.file = new File(dir, "users.yml");
        this.y = YamlConfiguration.loadConfiguration(file);
    }

    private static String k(String name) { return name.toLowerCase(); }

    synchronized boolean registered(String name) { return y.isString("users." + k(name) + ".hash"); }
    synchronized String hash(String name) { return y.getString("users." + k(name) + ".hash"); }
    synchronized String lastIp(String name) { return y.getString("users." + k(name) + ".lastIp"); }
    synchronized long lastLogout(String name) { return y.getLong("users." + k(name) + ".lastLogout", 0); }

    synchronized void register(String name, String hash, String ip) {
        String b = "users." + k(name);
        y.set(b + ".name", name);
        y.set(b + ".hash", hash);
        y.set(b + ".regIp", ip);
        y.set(b + ".regDate", System.currentTimeMillis());
        y.set(b + ".lastIp", ip);
    }

    synchronized void setHash(String name, String hash) { y.set("users." + k(name) + ".hash", hash); }
    synchronized void loggedIn(String name, String ip) {
        y.set("users." + k(name) + ".lastIp", ip);
        y.set("users." + k(name) + ".lastLogin", System.currentTimeMillis());
    }
    synchronized void loggedOut(String name, String ip) {
        if (!registered(name)) return;
        y.set("users." + k(name) + ".lastIp", ip);
        y.set("users." + k(name) + ".lastLogout", System.currentTimeMillis());
    }
    synchronized boolean unregister(String name) {
        boolean had = registered(name);
        y.set("users." + k(name), null);
        return had;
    }
    synchronized int accountsForIp(String ip) {
        ConfigurationSection s = y.getConfigurationSection("users");
        if (s == null) return 0;
        int n = 0;
        for (String key : s.getKeys(false)) if (ip.equals(s.getString(key + ".regIp"))) n++;
        return n;
    }
    synchronized int count() {
        ConfigurationSection s = y.getConfigurationSection("users");
        return s == null ? 0 : s.getKeys(false).size();
    }
    synchronized void premiumSeen(String name, UUID uuid) {
        y.set("premium." + k(name), uuid.toString());
    }
    synchronized String info(String name) {
        String b = "users." + k(name);
        if (registered(name)) return "cracked, registered " + new java.util.Date(y.getLong(b + ".regDate")) + ", last IP " + y.getString(b + ".lastIp");
        String pu = y.getString("premium." + k(name));
        return pu != null ? "premium (" + pu + ")" : "unknown / never registered";
    }

    void saveAsync(org.bukkit.plugin.Plugin pl) {
        String data;
        synchronized (this) { data = y.saveToString(); }
        pl.getServer().getScheduler().runTaskAsynchronously(pl, () -> write(data));
    }
    void saveNow() {
        String data;
        synchronized (this) { data = y.saveToString(); }
        write(data);
    }
    private synchronized void write(String data) {
        try {
            File tmp = new File(file.getParentFile(), "users.yml.tmp");
            java.nio.file.Files.writeString(tmp.toPath(), data);
            java.nio.file.Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
