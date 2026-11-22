/*
 * Decompiled with CFR 0.152.
 */
package dev.killeffects;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

final class Store {
    private final File folder;
    private final Logger log;
    private final ExecutorService exec = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "KillEffects-Storage");
        thread.setDaemon(true);
        return thread;
    });
    private Connection conn;

    Store(File file, Logger logger) {
        this.folder = file;
        this.log = logger;
    }

    void open() throws Exception {
        if (!this.folder.exists()) {
            this.folder.mkdirs();
        }
        this.exec.submit(() -> {
            try {
                Class.forName("org.sqlite.JDBC");
            }
            catch (ClassNotFoundException classNotFoundException) {
                // empty catch block
            }
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + new File(this.folder, "killeffects.db").getAbsolutePath());
            try (Statement statement = this.conn.createStatement();){
                statement.execute("PRAGMA journal_mode=WAL");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, name TEXT, selected TEXT, shards INTEGER, kills INTEGER, opened INTEGER, owned TEXT, keys TEXT)");
            }
            return null;
        }).get(15L, TimeUnit.SECONDS);
    }

    Map<UUID, Profile> loadAll() throws Exception {
        return this.exec.submit(() -> {
            HashMap<UUID, Profile> hashMap = new HashMap<UUID, Profile>();
            try (Statement statement = this.conn.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT * FROM players");){
                while (resultSet.next()) {
                    Profile profile = new Profile();
                    profile.name = resultSet.getString("name");
                    profile.selected = resultSet.getString("selected");
                    profile.shards = resultSet.getLong("shards");
                    profile.kills = resultSet.getLong("kills");
                    profile.opened = resultSet.getLong("opened");
                    String string = resultSet.getString("owned");
                    if (string != null && !string.isBlank()) {
                        for (String string2 : string.split(",")) {
                            profile.owned.add(string2.trim());
                        }
                    }
                    Store.decodeKeys(resultSet.getString("keys"), profile.keys);
                    hashMap.put(UUID.fromString(resultSet.getString("uuid")), profile);
                }
            }
            return hashMap;
        }).get(30L, TimeUnit.SECONDS);
    }

    static String encodeKeys(Map<String, Integer> map) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() <= 0) continue;
            if (stringBuilder.length() > 0) {
                stringBuilder.append(',');
            }
            stringBuilder.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return stringBuilder.toString();
    }

    static void decodeKeys(String string, Map<String, Integer> map) {
        if (string == null || string.isBlank()) {
            return;
        }
        for (String string2 : string.split(",")) {
            int n = string2.indexOf(58);
            if (n <= 0) continue;
            try {
                map.put(string2.substring(0, n), Integer.parseInt(string2.substring(n + 1)));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
    }

    void save(UUID uUID, Profile profile) {
        Profile profile2 = profile.copy();
        this.exec.execute(() -> {
            try (PreparedStatement preparedStatement = this.conn.prepareStatement("INSERT OR REPLACE INTO players VALUES (?,?,?,?,?,?,?,?)");){
                preparedStatement.setString(1, uUID.toString());
                preparedStatement.setString(2, profile.name);
                preparedStatement.setString(3, profile.selected);
                preparedStatement.setLong(4, profile.shards);
                preparedStatement.setLong(5, profile.kills);
                preparedStatement.setLong(6, profile.opened);
                preparedStatement.setString(7, String.join((CharSequence)",", new LinkedHashSet<String>(profile.owned)));
                preparedStatement.setString(8, Store.encodeKeys(profile.keys));
                preparedStatement.executeUpdate();
            }
            catch (Exception exception) {
                this.log.log(Level.SEVERE, "KillEffects database error", exception);
            }
        });
    }

    void close() {
        this.exec.shutdown();
        try {
            this.exec.awaitTermination(10L, TimeUnit.SECONDS);
        }
        catch (InterruptedException interruptedException) {
            // empty catch block
        }
        try {
            if (this.conn != null) {
                this.conn.close();
            }
        }
        catch (SQLException sQLException) {
            // empty catch block
        }
    }

    static final class Profile {
        String name;
        String selected;
        long shards;
        long kills;
        long opened;
        final Set<String> owned = ConcurrentHashMap.newKeySet();
        final Map<String, Integer> keys = new ConcurrentHashMap<String, Integer>();

        Profile() {
        }

        int keys(String string) {
            return this.keys.getOrDefault(string, 0);
        }

        Profile copy() {
            Profile profile = new Profile();
            profile.name = this.name;
            profile.selected = this.selected;
            profile.shards = this.shards;
            profile.kills = this.kills;
            profile.opened = this.opened;
            profile.owned.addAll(this.owned);
            profile.keys.putAll(this.keys);
            return profile;
        }
    }
}

