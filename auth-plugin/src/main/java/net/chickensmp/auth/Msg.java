package net.chickensmp.auth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

final class Msg {
    private static final String P = "<color:#00A4FC><bold>AUTH</bold></color> <gray>»</gray> <white>";
    private final Map<String, String> de = new HashMap<>(), en = new HashMap<>();

    Msg() {
        put("register", "<white>Willkommen! Registriere dich mit <color:#FCE300>/register <Passwort> <Passwort></color>",
                "<white>Welcome! Register with <color:#FCE300>/register <password> <password></color>");
        put("login", "<white>Willkommen zurück! Melde dich an mit <color:#FCE300>/login <Passwort></color>",
                "<white>Welcome back! Log in with <color:#FCE300>/login <password></color>");
        put("titleRegister", "<color:#FCE300><bold>REGISTRIEREN", "<color:#FCE300><bold>REGISTER");
        put("titleLogin", "<color:#FCE300><bold>ANMELDEN", "<color:#FCE300><bold>LOG IN");
        put("subRegister", "<white>/register <Passwort> <Passwort>", "<white>/register <password> <password>");
        put("subLogin", "<white>/login <Passwort>", "<white>/login <password>");
        put("registered", "<color:#00FC00>Account erstellt – viel Spaß auf ChickenSMP!", "<color:#00FC00>Account created – have fun on ChickenSMP!");
        put("loggedIn", "<color:#00FC00>Erfolgreich angemeldet. Viel Spaß!", "<color:#00FC00>Logged in. Have fun!");
        put("sessionResumed", "<color:#00FC00>Automatisch angemeldet (gleiche IP).", "<color:#00FC00>Logged in automatically (same IP).");
        put("wrongPw", "<color:#FC0000>Falsches Passwort. Noch <color:#FCE300>{n}</color> Versuche.", "<color:#FC0000>Wrong password. <color:#FCE300>{n}</color> tries left.");
        put("notRegistered", "<color:#FC0000>Du bist noch nicht registriert. Nutze <color:#FCE300>/register <Passwort> <Passwort>", "<color:#FC0000>You are not registered yet. Use <color:#FCE300>/register <password> <password>");
        put("alreadyRegistered", "<color:#FC0000>Du bist schon registriert. Nutze <color:#FCE300>/login <Passwort>", "<color:#FC0000>You are already registered. Use <color:#FCE300>/login <password>");
        put("alreadyIn", "<white>Du bist bereits angemeldet.", "<white>You are already logged in.");
        put("pwShort", "<color:#FC0000>Das Passwort muss mindestens <color:#FCE300>{n}</color> Zeichen lang sein.", "<color:#FC0000>Your password needs at least <color:#FCE300>{n}</color> characters.");
        put("pwMismatch", "<color:#FC0000>Die Passwörter stimmen nicht überein.", "<color:#FC0000>The passwords don't match.");
        put("pwName", "<color:#FC0000>Dein Passwort darf nicht dein Name sein.", "<color:#FC0000>Your password can't be your name.");
        put("pwWeak", "<color:#FC0000>Dieses Passwort ist zu leicht zu erraten. Nimm ein anderes.", "<color:#FC0000>That password is too easy to guess. Pick another one.");
        put("failsWarning", "<color:#FCE300>Achtung: <color:#FCE300>{n}</color> falsche Anmeldeversuche seit deinem letzten Login. Ändere dein Passwort mit /changepassword, wenn du das nicht warst.",
                "<color:#FCE300>Heads up: <color:#FCE300>{n}</color> wrong login attempts on your account since your last login. If that wasn't you, use <color:#FCE300>/changepassword</color>.");
        put("pwChanged", "<color:#00FC00>Passwort geändert.", "<color:#00FC00>Password changed.");
        put("noPremiumPw", "<white>Du spielst mit einem gekauften Account oder über Bedrock – du brauchst kein Passwort.", "<white>You play with a paid account or on Bedrock – you don't need a password.");
        put("mustLogin", "<color:#FC0000>Bitte melde dich zuerst an.", "<color:#FC0000>Please log in first.");
        put("tooManyAccounts", "<color:#FC0000>Von deiner Verbindung wurden schon zu viele Accounts registriert.", "<color:#FC0000>Too many accounts were registered from your connection.");
        put("usage.register", "<color:#FC0000>Nutze: /register <Passwort> <Passwort>", "<color:#FC0000>Usage: /register <password> <password>");
        put("usage.login", "<color:#FC0000>Nutze: /login <Passwort>", "<color:#FC0000>Usage: /login <password>");
        put("usage.change", "<color:#FC0000>Nutze: /changepassword <altes> <neues>", "<color:#FC0000>Usage: /changepassword <old> <new>");
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
        put("accountLocked", "Dieser Account ist wegen zu vieler falscher Passwörter 15 Minuten gesperrt.", "This account is locked for 15 minutes after too many wrong passwords.");
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
        return MiniMessage.miniMessage().deserialize("<color:#00A4FC><bold>ChickenSMP</bold></color>\n\n<color:#FC0000>" + en.get(key));
    }
}
