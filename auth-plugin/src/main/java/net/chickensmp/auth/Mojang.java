package net.chickensmp.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class Mojang {
    enum Kind { PREMIUM, CRACKED, UNKNOWN }
    record Lookup(Kind kind, UUID uuid) {}
    record Prop(String name, String value, String signature) {}
    record Verified(UUID uuid, String name, List<Prop> props) {}

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private record Cached(Lookup l, long until) {}
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    /** Is this name owned by a paid Minecraft account? */
    Lookup lookup(String name) {
        String key = name.toLowerCase();
        Cached c = cache.get(key);
        if (c != null && c.until > System.currentTimeMillis()) return c.l;
        Lookup l = query("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + enc(name));
        if (l.kind == Kind.UNKNOWN) l = query("https://api.mojang.com/users/profiles/minecraft/" + enc(name));
        if (l.kind != Kind.UNKNOWN) {
            long ttl = l.kind == Kind.PREMIUM ? 6 * 3600_000L : 20 * 60_000L;
            cache.put(key, new Cached(l, System.currentTimeMillis() + ttl));
        }
        return l;
    }

    void forget(String name) { cache.remove(name.toLowerCase()); }

    private Lookup query(String url) {
        try {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "ChickenSMPAuth/1.0").GET().build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 204 || r.statusCode() == 404) return new Lookup(Kind.CRACKED, null);
            if (r.statusCode() == 200) {
                JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
                if (o.has("id")) return new Lookup(Kind.PREMIUM, parse(o.get("id").getAsString()));
                return new Lookup(Kind.CRACKED, null);
            }
        } catch (Exception ignored) { }
        return new Lookup(Kind.UNKNOWN, null);
    }

    /** Ask Mojang's session server whether this client really authenticated for our server hash. */
    Verified hasJoined(String name, String serverHash) throws Exception {
        String url = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + enc(name) + "&serverId=" + enc(serverHash);
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                .header("User-Agent", "ChickenSMPAuth/1.0").GET().build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200 || r.body().isBlank()) return null;
        JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
        List<Prop> props = new ArrayList<>();
        if (o.has("properties")) {
            JsonArray arr = o.getAsJsonArray("properties");
            for (JsonElement e : arr) {
                JsonObject p = e.getAsJsonObject();
                props.add(new Prop(p.get("name").getAsString(), p.get("value").getAsString(),
                        p.has("signature") ? p.get("signature").getAsString() : null));
            }
        }
        return new Verified(parse(o.get("id").getAsString()), o.get("name").getAsString(), props);
    }

    static String serverHash(String serverId, byte[] secret, PublicKey key) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        sha.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
        sha.update(secret);
        sha.update(key.getEncoded());
        return new BigInteger(sha.digest()).toString(16);
    }

    static UUID parse(String id) {
        String s = id.replace("-", "");
        return new UUID(Long.parseUnsignedLong(s.substring(0, 16), 16), Long.parseUnsignedLong(s.substring(16), 16));
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
