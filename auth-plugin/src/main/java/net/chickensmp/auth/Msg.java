package net.chickensmp.auth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

final class Msg {
    private static final String P = "<gradient:#3b7bff:#7cc4ff><bold>ChickenSMP</bold></gradient> <dark_gray>»</dark_gray> ";
    private final Map<String, String> de = new HashMap<>(), en = new HashMap<>();

    Msg() {
        put("register", "<gray>Willkommen! Registriere dich mit <aqua>/register <Passwort> <Passwort></aqua>",
                "<gray>Welcome! Register with <aqua>/register <password> <password></aqua>");
        put("login", "<gray>Willkommen zurück! Melde dich an mit <aqua>/login <Passwort></aqua>",
                "<gray>Welcome back! Log in with <aqua>/login <password></aqua>");
        put("titleRegister", "<aqua><bold>REGISTRIEREN", "<aqua><bold>REGISTER");
        put("titleLogin", "<aqua><bold>ANMELDEN", "<aqua><bold>LOG IN");
        put("subRegister", "<gray>/register <Passwort> <Passwort>", "<gray>/register <password> <password>");
        put("subLogin", "<gray>/login <Passwort>", "<gray>/login <password>");
        put("registered", "<green>Account erstellt – viel Spaß auf ChickenSMP!", "<green>Account created – have fun on ChickenSMP!");
        put("loggedIn", "<green>Erfolgreich angemeldet. Viel Spaß!", "<green>Logged in. Have fun!");
        put("sessionResumed", "<green>Automatisch angemeldet (gleiche IP).", "<green>Logged in automatically (same IP).");
        put("wrongPw", "<red>Falsches Passwort. Noch <yellow>{n}</yellow> Versuche.", "<red>Wrong password. <yellow>{n}</yellow> tries left.");
        put("notRegistered", "<red>Du bist noch nicht registriert. Nutze <aqua>/register <Passwort> <Passwort>", "<red>You are not registered yet. Use <aqua>/register <password> <password>");
        put("alreadyRegistered", "<red>Du bist schon registriert. Nutze <aqua>/login <Passwort>", "<red>You are already registered. Use <aqua>/login <password>");
        put("alreadyIn", "<gray>Du bist bereits angemeldet.", "<gray>You are already logged in.");
        put("pwShort", "<red>Das Passwort muss mindestens <yellow>{n}</yellow> Zeichen lang sein.", "<red>Your password needs at least <yellow>{n}</yellow> characters.");
        put("pwMismatch", "<red>Die Passwörter stimmen nicht überein.", "<red>The passwords don't match.");
        put("pwName", "<red>Dein Passwort darf nicht dein Name sein.", "<red>Your password can't be your name.");
        put("pwChanged", "<green>Passwort geändert.", "<green>Password changed.");
        put("noPremiumPw", "<gray>Du spielst mit einem gekauften Account oder über Bedrock – du brauchst kein Passwort.", "<gray>You play with a paid account or on Bedrock – you don't need a password.");
        put("mustLogin", "<red>Bitte melde dich zuerst an.", "<red>Please log in first.");
        put("tooManyAccounts", "<red>Von deiner Verbindung wurden schon zu viele Accounts registriert.", "<red>Too many accounts were registered from your connection.");
        put("usage.register", "<red>Nutze: /register <Passwort> <Passwort>", "<red>Usage: /register <password> <password>");
        put("usage.login", "<red>Nutze: /login <Passwort>", "<red>Usage: /login <password>");
        put("usage.change", "<red>Nutze: /changepassword <altes> <neues>", "<red>Usage: /changepassword <old> <new>");
        // Kick / disconnect messages (shown in both languages, client language unknown at that point)
        put("timeout", "Zeit abgelaufen – bitte erneut verbinden und anmelden.", "Time ran out – please reconnect and log in.");
        put("tooMany", "Zu viele falsche Passwörter.", "Too many wrong passwords.");
        put("premiumName", "Dieser Name gehört zu einem gekauften Minecraft-Account.\nJoine mit diesem Account oder nimm einen anderen Namen.",
                "This name belongs to a paid Minecraft account.\nJoin with that account or pick another name.");
        put("invalidName", "Ungültiger Name. Erlaubt: 3–16 Zeichen, A–Z, 0–9 und _", "Invalid name. Allowed: 3–16 characters, A–Z, 0–9 and _");
        put("alreadyOnline", "Ein Spieler mit diesem Namen ist bereits online.", "A player with this name is already online.");
        put("mojangDown", "Die Mojang-Server sind gerade nicht erreichbar. Bitte versuch es gleich nochmal.", "Mojang's servers can't be reached right now. Please try again shortly.");
        put("oldClient", "Bitte nutze Minecraft 1.19.3 oder neuer.", "Please use Minecraft 1.19.3 or newer.");
        put("verifyFailed", "Dein Minecraft-Account konnte nicht bestätigt werden.\nStarte Minecraft neu und versuch es nochmal.", "Your Minecraft account could not be verified.\nRestart Minecraft and try again.");
        put("ipBlocked", "Zu viele fehlgeschlagene Anmeldungen. Bitte warte 10 Minuten.", "Too many failed logins. Please wait 10 minutes.");
    }

    private void put(String k, String d, String e) { de.put(k, d); en.put(k, e); }

    /** Server is English-only. */
    static String lang(Player p) { return "en"; }

    String raw(String lang, String key) { return (lang.equals("de") ? de : en).getOrDefault(key, key); }

    /** Plain text in both languages, for disconnect screens. */
    String both(String key) { return en.get(key); }

    Component c(Player p, String key, Object... n) {
        String s = raw(lang(p), key);
        if (n.length > 0) s = s.replace("{n}", String.valueOf(n[0]));
        return MiniMessage.miniMessage().deserialize(s);
    }

    void send(Player p, String key, Object... n) { p.sendMessage(MiniMessage.miniMessage().deserialize(P).append(c(p, key, n))); }

    Component kick(String key) {
        return MiniMessage.miniMessage().deserialize("<gradient:#3b7bff:#7cc4ff><bold>ChickenSMP</bold></gradient>\n\n<red>" + en.get(key));
    }
}
