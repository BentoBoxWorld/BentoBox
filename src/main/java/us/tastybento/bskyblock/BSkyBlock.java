package us.tastybento.bskyblock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import us.tastybento.bskyblock.config.BSBLocale;
import us.tastybento.bskyblock.config.PluginConfig;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.BSBDatabase.DatabaseType;
import us.tastybento.bskyblock.database.managers.IslandsManager;
import us.tastybento.bskyblock.database.managers.OfflineHistoryMessages;
import us.tastybento.bskyblock.database.managers.PlayersManager;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Island.SettingsFlag;
import us.tastybento.bskyblock.util.VaultHelper;

/**
 * Main BSkyBlock class - provides an island minigame in the sky
 * @author Tastybento
 * @author Poslovitch
 */
public class BSkyBlock extends JavaPlugin{
    private static BSkyBlock plugin;

    private HashMap<String, BSBLocale> locales = new HashMap<String, BSBLocale>();

    // Databases
    private PlayersManager playersManager;
    private IslandsManager islandsManager;
    private OfflineHistoryMessages offlineHistoryMessages;

    // Metrics
    private Metrics metrics;

    @Override
    public void onEnable(){
        plugin = this;

        // Load configuration and locales. If there are no errors, load the plugin.
        if(PluginConfig.loadPluginConfig(this)){
            // TEMP DEBUG DATABASE
            Settings.databaseType = DatabaseType.MYSQL;
            Settings.dbHost = "localhost";
            Settings.dbPort = 3306;
            Settings.dbName = "ASkyBlock";
            Settings.dbUsername = "username";
            Settings.dbPassword = "password";

            playersManager = new PlayersManager(this);
            islandsManager = new IslandsManager(this);
            // Only load metrics if set to true in config
            if(Settings.metrics) metrics = new Metrics(plugin);

            // If metrics are loaded, register the custom data charts
            if(metrics != null){
                registerCustomCharts();
            }

            offlineHistoryMessages = new OfflineHistoryMessages(this);
            offlineHistoryMessages.load();

            if (Settings.useEconomy && !VaultHelper.setupEconomy()) {
                getLogger().warning("Could not set up economy! - Running without an economy.");
                Settings.useEconomy = false;
            }

            // These items have to be loaded when the server has done 1 tick.
            // Note Worlds are not loaded this early, so any Locations or World reference will be null
            // at this point. Therefore, the 1 tick scheduler is required.
            getServer().getScheduler().runTask(this, new Runnable() {

                @Override
                public void run() {

                    // Test: Create a random island and save it
                    // TODO: ideally this should be in a test class!
                    /*
                    Island island = islandsManager.createIsland(new Location(getServer().getWorld("world"),0,0,0,0,0), UUID.randomUUID());
                    // Add members
                    Set<UUID> randomSet = new HashSet<UUID>();
                    for (int i = 0; i < 10; i++) {
                        randomSet.add(UUID.randomUUID());
                        island.addMember(UUID.randomUUID());
                        island.addToBanList(UUID.randomUUID());
                    }
                    island.setBanned(randomSet);
                    island.setCoops(randomSet);
                    island.setTrustees(randomSet);
                    island.setMembers(randomSet);
                    for (SettingsFlag flag: SettingsFlag.values()) {
                        island.setFlag(flag, true);
                    }
                    island.setLocked(true);
                    island.setName("new name");
                    island.setPurgeProtected(true);
                    islandsManager.save(false);
    
                    
                    getLogger().info("DEBUG: ************ Finished saving, now loading *************");
                    // TODO: Write loading code for MySQL
                     * 
                     */
                    playersManager.load();
                    islandsManager.load();


                    // Save islands & players data asynchronously every X minutes
                    Settings.databaseBackupPeriod = 10 * 60 * 20;
                    plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {

                        @Override
                        public void run() {
                            playersManager.save(true);
                            islandsManager.save(true);
                            offlineHistoryMessages.save(true);
                        }
                    }, Settings.databaseBackupPeriod, Settings.databaseBackupPeriod);
                }
                // TODO Auto-generated method stub

            });
        }
    }

    @Override
    public void onDisable(){
        // Save data
        playersManager.shutdown();
        islandsManager.shutdown();
        //offlineHistoryMessages.shutdown();
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
                return BSBDatabase.getDatabase().toString();
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
    public HashMap<String, BSBLocale> getLocales(){
        return locales;
    }

    /**
     * Set the available locales
     * @param locales - the locales to set
     */
    public void setLocales(HashMap<String, BSBLocale> locales){
        this.locales = locales;
    }

    /**
     * Returns the default locale
     * @return the default locale
     */
    public BSBLocale getLocale(){
        return locales.get(Settings.defaultLanguage);
    }

    /**
     * Returns the locale for the specified CommandSender
     * @param sender - CommandSender to get the locale
     * @return if sender is a player, the player's locale, otherwise the default locale
     */
    public BSBLocale getLocale(CommandSender sender){
        if(sender instanceof Player) return getLocale(((Player) sender).getUniqueId());
        else return getLocale();
    }

    /**
     * Returns the locale for the specified player
     * @param player - Player to get the locale
     * @return the locale for this player
     */
    public BSBLocale getLocale(UUID player){
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
