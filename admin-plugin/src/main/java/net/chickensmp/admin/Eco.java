package net.chickensmp.admin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Vault access kept in its own class, so the plugin still loads when Vault is missing. */
final class Eco {
    private Eco() {}
    private static Economy economy;

    private static Economy get() {
        if (economy == null && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        }
        return economy;
    }

    static boolean available() {
        return Bukkit.getPluginManager().getPlugin("Vault") != null && get() != null;
    }

    static double balance(OfflinePlayer p) { return available() ? get().getBalance(p) : 0; }

    static boolean deposit(OfflinePlayer p, double amount) {
        return available() && get().depositPlayer(p, amount).transactionSuccess();
    }
}
