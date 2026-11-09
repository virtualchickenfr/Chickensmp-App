package net.chickensmp.auth;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientEncryptionResponse;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerDisconnect;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerEncryptionRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Runs Mojang's online-mode authentication on an offline-mode server, but only for names
 * that belong to a paid account. Cracked names are passed straight through.
 */
final class PremiumHandshake extends PacketListenerAbstract {
    record Pending(String name, UUID clientUuid, ClientVersion version, byte[] token, long at) {}
    record Done(Mojang.Verified v, long at) {}

    private final AuthPlugin plugin;
    private final Mojang mojang;
    private final KeyPair keys;
    private final SecureRandom rng = new SecureRandom();
    private final Map<Object, Pending> pending = new ConcurrentHashMap<>();
    private final Map<String, Done> verified = new ConcurrentHashMap<>();

    PremiumHandshake(AuthPlugin plugin, Mojang mojang) throws Exception {
        super(PacketListenerPriority.LOWEST);
        this.plugin = plugin;
        this.mojang = mojang;
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(1024);
        this.keys = g.generateKeyPair();
    }

    /** Called from AsyncPlayerPreLoginEvent. Returns the Mojang-verified profile for this name, once. */
    Mojang.Verified consume(String name) {
        Done d = verified.remove(name.toLowerCase());
        if (d == null || System.currentTimeMillis() - d.at > 60_000) return null;
        return d.v;
    }

    void cleanup() {
        long now = System.currentTimeMillis();
        pending.values().removeIf(p -> now - p.at > 60_000);
        verified.values().removeIf(d -> now - d.at > 60_000);
    }

    static boolean isLocal(SocketAddress a) {
        if (!(a instanceof InetSocketAddress isa)) return true; // Geyser direct connection (LocalChannel)
        return isa.getAddress() != null && isa.getAddress().isLoopbackAddress();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) onLoginStart(event);
        else if (event.getPacketType() == PacketType.Login.Client.ENCRYPTION_RESPONSE) onEncryption(event);
    }

    private void onLoginStart(PacketReceiveEvent event) {
        WrapperLoginClientLoginStart w = new WrapperLoginClientLoginStart(event);
        String name = w.getUsername();
        Object channel = event.getChannel();
        if (isLocal(ChannelHelper.remoteAddress(channel))) return;          // Bedrock via Geyser
        String fp = plugin.floodgatePrefix();
        if (!fp.isEmpty() && name.startsWith(fp)) return;                   // Bedrock name
        if (!name.matches("^[a-zA-Z0-9_]{3,16}$")) return;                  // can't be premium; pre-login decides
        User user = event.getUser();
        UUID clientUuid = w.getPlayerUUID().orElse(null);
        ClientVersion version = event.getClientVersion();
        event.setCancelled(true);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Mojang.Lookup l = mojang.lookup(name);
            ChannelHelper.runInEventLoop(channel, () -> {
                if (!ChannelHelper.isOpen(channel)) return;
                if (l.kind() == Mojang.Kind.PREMIUM) {
                    byte[] token = new byte[4];
                    rng.nextBytes(token);
                    pending.put(channel, new Pending(name, clientUuid, version, token, System.currentTimeMillis()));
                    user.sendPacket(new WrapperLoginServerEncryptionRequest("", keys.getPublic(), token, true));
                } else if (l.kind() == Mojang.Kind.CRACKED || !plugin.failClosed()) {
                    reinject(user, version, name, clientUuid);
                } else {
                    disconnect(user, plugin.msg().both("mojangDown"));
                }
            });
        });
    }

    private void onEncryption(PacketReceiveEvent event) {
        Object channel = event.getChannel();
        Pending p = pending.remove(channel);
        if (p == null) return;
        event.setCancelled(true);
        User user = event.getUser();
        try {
            WrapperLoginClientEncryptionResponse w = new WrapperLoginClientEncryptionResponse(event);
            if (w.getEncryptedVerifyToken().isEmpty()) { disconnect(user, plugin.msg().both("oldClient")); return; }
            Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.DECRYPT_MODE, keys.getPrivate());
            byte[] token = rsa.doFinal(w.getEncryptedVerifyToken().get());
            if (!MessageDigest.isEqual(token, p.token())) { disconnect(user, plugin.msg().both("verifyFailed")); return; }
            SecretKey secret = w.getSecretKey(keys.getPrivate());
            // Turn on encryption now: the client encrypts everything after this packet.
            ChannelPipeline pl = ((Channel) channel).pipeline();
            pl.addBefore(pl.get("splitter") != null ? "splitter" : pl.names().get(0), "decrypt",
                    new CipherCodec.Decoder(CipherCodec.cipher(Cipher.DECRYPT_MODE, secret)));
            pl.addBefore(pl.get("prepender") != null ? "prepender" : pl.names().get(0), "encrypt",
                    new CipherCodec.Encoder(CipherCodec.cipher(Cipher.ENCRYPT_MODE, secret)));
            String hash = Mojang.serverHash("", secret.getEncoded(), keys.getPublic());
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Mojang.Verified v = null;
                try { v = mojang.hasJoined(p.name(), hash); } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "hasJoined failed for " + p.name(), e);
                }
                Mojang.Verified fv = v;
                ChannelHelper.runInEventLoop(channel, () -> {
                    if (!ChannelHelper.isOpen(channel)) return;
                    if (fv == null || !fv.name().equalsIgnoreCase(p.name())) {
                        disconnect(user, plugin.msg().both("verifyFailed"));
                        return;
                    }
                    verified.put(p.name().toLowerCase(), new Done(fv, System.currentTimeMillis()));
                    plugin.getLogger().info("Premium account verified: " + fv.name() + " (" + fv.uuid() + ")");
                    reinject(user, p.version(), p.name(), p.clientUuid());
                });
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Premium handshake failed for " + p.name(), e);
            disconnect(user, plugin.msg().both("verifyFailed"));
        }
    }

    private void reinject(User user, ClientVersion v, String name, UUID uuid) {
        user.receivePacketSilently(new WrapperLoginClientLoginStart(v, name, null, uuid));
    }

    private void disconnect(User user, String text) {
        try { user.sendPacket(new WrapperLoginServerDisconnect(Component.text(text, NamedTextColor.RED))); } catch (Exception ignored) { }
        user.closeConnection();
    }
}
