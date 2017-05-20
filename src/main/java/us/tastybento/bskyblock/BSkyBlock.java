package us.tastybento.bskyblock;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import us.tastybento.bskyblock.config.ASBLocale;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.ASBDatabase;
import us.tastybento.bskyblock.database.IslandsManager;
import us.tastybento.bskyblock.database.OfflineHistoryMessages;
import us.tastybento.bskyblock.database.PlayersManager;
import us.tastybento.bskyblock.util.VaultHelper;

/**
 * Main BSkyBlock class - provides an island minigame in the sky
 * @author Tastybento
 * @author Poslovitch
 */
public class BSkyBlock extends JavaPlugin{
    private static BSkyBlock plugin;
    
    private HashMap<String, ASBLocale> locales = new HashMap<String, ASBLocale>();
    
    // Databases
    private PlayersManager playersManager;
    private IslandsManager islandsManager;
    private OfflineHistoryMessages offlineHistoryMessages;
    
    // Metrics
    private Metrics metrics;
    
    @Override
    public void onEnable(){
        plugin = this;
        playersManager = new PlayersManager(this);
        islandsManager = new IslandsManager(this);
        
        playersManager.load();
        islandsManager.load();
        
        offlineHistoryMessages = new OfflineHistoryMessages(this);
        offlineHistoryMessages.load();
        
        if (Settings.useEconomy && !VaultHelper.setupEconomy(this)) {
            getLogger().warning("Could not set up economy! - Running without an economy.");
            Settings.useEconomy = false;
        }
        if (!VaultHelper.setupPermissions(this)) {
            getLogger().severe("Cannot link with Vault for permissions! Disabling plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Only load metrics if set to true in config
        if(Settings.metrics) metrics = new Metrics(this);
        
        // If metrics are loaded, register the custom data charts
        if(metrics != null){
            registerCustomCharts();
        }
        
        // Save islands & players data asynchronously every X minutes
        plugin.getServer().getScheduler().runTaskTimer(this, new Runnable() {
            
            @Override
            public void run() {
                playersManager.save(true);
                islandsManager.save(true);
                offlineHistoryMessages.save(true);
            }
        }, Settings.backupPeriod, Settings.backupPeriod);
    }
    
    @Override
    public void onDisable(){
        // Save data
        playersManager.shutdown();
        islandsManager.shutdown();
        offlineHistoryMessages.shutdown();
        
        plugin = null;
    }
    
    private void registerCustomCharts(){   
        metrics.addCustomChart(new Metrics.SingleLineChart("islands_count") {
            
            @Override
            public int getValue() {
                return islandsManager.getCount();
            }
        });
        
        metrics.addCustomChart(new Metrics.SingleLineChart("created_islands") {
            
            @Override
            public int getValue() {
                int created = islandsManager.metrics_getCreatedCount();
                islandsManager.metrics_setCreatedCount(0);
                return created;
            }
        });
        
        metrics.addCustomChart(new Metrics.SimplePie("default_locale") {
            
            @Override
            public String getValue() {
                return Settings.defaultLanguage;
            }
        });
        
        metrics.addCustomChart(new Metrics.SimplePie("database") {
            
            @Override
            public String getValue() {
                return ASBDatabase.getDatabase().toString();
            }
        });
    }
    
    /**
     * Returns BSkyBlock object instance
     * @return BSkyBlock instance
     */
    public static BSkyBlock getInstance(){
        return plugin;
    }
    
    /**
     * Returns an HashMap of locale identifier and the related object
     * @return the locales
     */
    public HashMap<String, ASBLocale> getLocales(){
        return locales;
    }
    
    /**
     * Returns the default locale
     * @return the default locale
     */
    public ASBLocale getLocale(){
        return locales.get(Settings.defaultLanguage);
    }
    
    /**
     * Returns the locale for the specified CommandSender
     * @param sender - CommandSender to get the locale
     * @return if sender is a player, the player's locale, otherwise the default locale
     */
    public ASBLocale getLocale(CommandSender sender){
        if(sender instanceof Player) return getLocale(((Player) sender).getUniqueId());
        else return getLocale();
    }
    
    /**
     * Returns the locale for the specified player
     * @param player - Player to get the locale
     * @return the locale for this player
     */
    public ASBLocale getLocale(UUID player){
        String locale = getPlayers().getPlayer(player).getLocale();
        if(locale.isEmpty() || !locales.containsKey(locale)) return locales.get(Settings.defaultLanguage);
        
        return locales.get(locale);
    }
    
    /**
     * Returns the player database
     * @return the player database
     */
    public PlayersManager getPlayers(){
        return playersManager;
    }
    
    /**
     * Returns the island database
     * @return the island database
     */
    public IslandsManager getIslands(){
        return islandsManager;
    }
}
