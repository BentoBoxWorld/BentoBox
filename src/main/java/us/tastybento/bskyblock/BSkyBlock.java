package us.tastybento.bskyblock;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import us.tastybento.bskyblock.commands.AdminCommand;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.PlayersManager;
import us.tastybento.bskyblock.database.managers.island.IslandsManager;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.listeners.JoinLeaveListener;
import us.tastybento.bskyblock.listeners.PanelListenerManager;
import us.tastybento.bskyblock.managers.AddonsManager;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.FlagsManager;
import us.tastybento.bskyblock.managers.LocalesManager;

/**
 * Main BSkyBlock class - provides an island minigame in the sky
 * @author Tastybento
 * @author Poslovitch
 */
public class BSkyBlock extends JavaPlugin {

    private static BSkyBlock plugin;

    // Databases
    private PlayersManager playersManager;
    private IslandsManager islandsManager;

    // Metrics
    private Metrics metrics;

    // Managers
    private CommandsManager commandsManager;
    private LocalesManager localesManager;
    private AddonsManager addonsManager;
    private FlagsManager flagsManager;
    private IslandWorld islandWorldManager;

    // Settings
    Settings settings;


    @Override
    public void onEnable(){
        plugin = this;

        // Load config - EXPERIMENTAL        
        settings = new Settings();
        try {
            //config.saveConfig(); // works, but will wipe out comments
            settings = settings.loadSettings();
            getLogger().info("DEBUG: island distance = " + settings.getIslandDistance());
        } catch (Exception e) {
            e.printStackTrace();
        }

        playersManager = new PlayersManager(this);
        islandsManager = new IslandsManager(this);

        // Load metrics
        metrics = new Metrics(plugin);
        registerCustomCharts();

        // Set up commands
        commandsManager = new CommandsManager();
        new IslandCommand();
        new AdminCommand();

        // These items have to be loaded when the server has done 1 tick.
        // Note Worlds are not loaded this early, so any Locations or World reference will be null
        // at this point. Therefore, the 1 tick scheduler is required.
        getServer().getScheduler().runTask(this, new Runnable() {

            @Override
            public void run() {
                // Create the world if it does not exist
                islandWorldManager = new IslandWorld(plugin);

                getServer().getScheduler().runTask(plugin, new Runnable() {

                    @Override
                    public void run() {
                        // Load islands from database
                        islandsManager.load();

                        // TODO: load these from config.yml
                        getSettings().setChestItems(new ItemStack[] {
                                new ItemStack(Material.LAVA_BUCKET,1),
                                new ItemStack(Material.ICE,2),
                                new ItemStack(Material.MELON_SEEDS,1),
                                new ItemStack(Material.BONE,2),
                                new ItemStack(Material.COBBLESTONE,5),
                                new ItemStack(Material.SAPLING,2)
                        });

                        //getSettings().setDefaultLanguage("en-US");
                        plugin.getLogger().info("DEBUG: ************************** Loading Locales **************************");
                        localesManager = new LocalesManager(plugin);
                        //TODO localesManager.registerLocales(plugin);

                        // Register Listeners
                        registerListeners();

                        // Load Flags
                        flagsManager = new FlagsManager();

                        // Load addons
                        addonsManager = new AddonsManager(plugin);
                        addonsManager.enableAddons();

                        /*
                         *DEBUG CODE
                            Island loadedIsland = islandsManager.getIsland(owner);
                            getLogger().info("Island name = " + loadedIsland.getName());
                            getLogger().info("Island locked = " + loadedIsland.getLocked());
                            //getLogger().info("Random set = " + randomSet);
                            getLogger().info("Island coops = " + loadedIsland.getCoops());
                            for (Entry<SettingsFlag, Boolean> flag: loadedIsland.getFlags().entrySet()) {
                                getLogger().info("Flag " + flag.getKey().name() + " = " + flag.getValue());
                            }
                         */
                        // Save islands & players data asynchronously every X minutes
                        getSettings().setDatabaseBackupPeriod(10 * 60 * 20);
                        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {

                            @Override
                            public void run() {
                                playersManager.save(true);
                                islandsManager.save(true);
                            }
                        }, getSettings().getDatabaseBackupPeriod(), getSettings().getDatabaseBackupPeriod());
                    }
                });
            } 
        });
    }



    private void registerListeners() {
        PluginManager manager = getServer().getPluginManager();
        // Player join events
        manager.registerEvents(new JoinLeaveListener(this), this);
        manager.registerEvents(new PanelListenerManager(), this);
    }

    @Override
    public void onDisable() {
        addonsManager.disableAddons();
        // Save data
        playersManager.shutdown();
        islandsManager.shutdown();
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
                return getSettings().getDefaultLanguage();
            }
        });

        metrics.addCustomChart(new Metrics.SimplePie("database") {

            @Override
            public String getValue() {
                return BSBDatabase.getDatabase(plugin).toString();
            }
        });
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

    public static BSkyBlock getInstance() {
        return plugin;
    }

    /**
     * @return the Commands manager
     */
    public CommandsManager getCommandsManager() {
        return commandsManager;
    }

    /**
     * @return the Locales manager
     */
    public LocalesManager getLocalesManager() {
        return localesManager;
    }

    /**
     * @return the Addons manager
     */
    public AddonsManager getAddonsManager() {
        return addonsManager;
    }

    /**
     * @return the Flags manager
     */
    public FlagsManager getFlagsManager() {
        return flagsManager;
    }

    /**
     * @return the settings
     */
    public Settings getSettings() {
        return settings;
    }


    /**
     * @return the Island World Manager
     */
    public IslandWorld getIslandWorldManager() {
        return islandWorldManager;
    }

}
