package net.chickensmp.auth;

import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

/** Only loaded when Floodgate is installed. */
final class FloodgateHook {
    static boolean isBedrock(UUID uuid) {
        try { return FloodgateApi.getInstance().isFloodgatePlayer(uuid); } catch (Throwable t) { return false; }
    }
    static String prefix() {
        try { return FloodgateApi.getInstance().getPlayerPrefix(); } catch (Throwable t) { return ""; }
    }
}
