package fe;
public class Eco implements net.milkbowl.vault.economy.Economy {
  public boolean isEnabled() { return true; }
  public java.lang.String getName() { return "x"; }
  public boolean hasBankSupport() { return true; }
  public int fractionalDigits() { return 2; }
  public java.lang.String format(double a0) { return "x"; }
  public java.lang.String currencyNamePlural() { return "x"; }
  public java.lang.String currencyNameSingular() { return "x"; }
  public boolean hasAccount(java.lang.String a0) { return true; }
  public boolean hasAccount(org.bukkit.OfflinePlayer a0) { return true; }
  public boolean hasAccount(java.lang.String a0, java.lang.String a1) { return true; }
  public boolean hasAccount(org.bukkit.OfflinePlayer a0, java.lang.String a1) { return true; }
  public double getBalance(java.lang.String a0) { return 1e12; }
  public double getBalance(org.bukkit.OfflinePlayer a0) { return 1e12; }
  public double getBalance(java.lang.String a0, java.lang.String a1) { return 1e12; }
  public double getBalance(org.bukkit.OfflinePlayer a0, java.lang.String a1) { return 1e12; }
  public boolean has(java.lang.String a0, double a1) { return true; }
  public boolean has(org.bukkit.OfflinePlayer a0, double a1) { return true; }
  public boolean has(java.lang.String a0, java.lang.String a1, double a2) { return true; }
  public boolean has(org.bukkit.OfflinePlayer a0, java.lang.String a1, double a2) { return true; }
  public net.milkbowl.vault.economy.EconomyResponse withdrawPlayer(java.lang.String a0, double a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse withdrawPlayer(org.bukkit.OfflinePlayer a0, double a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse withdrawPlayer(java.lang.String a0, java.lang.String a1, double a2) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse withdrawPlayer(org.bukkit.OfflinePlayer a0, java.lang.String a1, double a2) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse depositPlayer(java.lang.String a0, double a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse depositPlayer(org.bukkit.OfflinePlayer a0, double a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse depositPlayer(java.lang.String a0, java.lang.String a1, double a2) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse depositPlayer(org.bukkit.OfflinePlayer a0, java.lang.String a1, double a2) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse createBank(java.lang.String a0, java.lang.String a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse createBank(java.lang.String a0, org.bukkit.OfflinePlayer a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse deleteBank(java.lang.String a0) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse bankBalance(java.lang.String a0) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse bankHas(java.lang.String a0, double a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse bankWithdraw(java.lang.String a0, double a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse bankDeposit(java.lang.String a0, double a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse isBankOwner(java.lang.String a0, java.lang.String a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse isBankOwner(java.lang.String a0, org.bukkit.OfflinePlayer a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse isBankMember(java.lang.String a0, java.lang.String a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public net.milkbowl.vault.economy.EconomyResponse isBankMember(java.lang.String a0, org.bukkit.OfflinePlayer a1) { return new net.milkbowl.vault.economy.EconomyResponse(0,1e12,net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,""); }
  public java.util.List<java.lang.String> getBanks() { return new java.util.ArrayList<>(); }
  public boolean createPlayerAccount(java.lang.String a0) { return true; }
  public boolean createPlayerAccount(org.bukkit.OfflinePlayer a0) { return true; }
  public boolean createPlayerAccount(java.lang.String a0, java.lang.String a1) { return true; }
  public boolean createPlayerAccount(org.bukkit.OfflinePlayer a0, java.lang.String a1) { return true; }
}
