package fe;
public class P extends org.bukkit.plugin.java.JavaPlugin { public void onLoad(){ getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class,new Eco(),this,org.bukkit.plugin.ServicePriority.Highest);} }
