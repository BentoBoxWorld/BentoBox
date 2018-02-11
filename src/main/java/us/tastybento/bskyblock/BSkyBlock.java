package us.tastybento.bskyblock;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import us.tastybento.bskyblock.api.placeholders.PlaceholderHandler;
import us.tastybento.bskyblock.commands.AdminCommand;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.PlayersManager;
import us.tastybento.bskyblock.database.managers.island.IslandsManager;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.listeners.JoinLeaveListener;
import us.tastybento.bskyblock.listeners.NetherPortals;
import us.tastybento.bskyblock.listeners.PanelListenerManager;
import us.tastybento.bskyblock.managers.AddonsManager;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.FlagsManager;
import us.tastybento.bskyblock.managers.LocalesManager;
import us.tastybento.bskyblock.managers.RanksManager;

/**
 * Main BSkyBlock class - provides an island minigame in the sky
 * @author tastybento
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
    private RanksManager ranksManager;

    // Settings
    Settings settings;


    @Override
    public void onEnable(){
        // Save the default config from config.yml
        saveDefaultConfig();
        setInstance(this);

        settings = new Settings();
        // Load settings from config.yml. This will check if there are any issues with it too.
        try {
            //settings.saveSettings();
            settings = settings.loadSettings();
        } catch (Exception e) {
            getLogger().severe("Settings could not be loaded" + e.getMessage());
        }

        // Save a backup of settings to the database so it can be checked next time
        try {
            settings.saveBackup();
        } catch (Exception e) {
            getLogger().severe("Settings backup could not be saved" + e.getMessage());
        }

        playersManager = new PlayersManager(this);
        islandsManager = new IslandsManager(this);
        ranksManager = new RanksManager(this);

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
        getServer().getScheduler().runTask(this, () -> {
            // Create the world if it does not exist
            islandWorldManager = new IslandWorld(plugin);

            getServer().getScheduler().runTask(plugin, () -> {

                // Load Flags
                flagsManager = new FlagsManager(plugin);

                // Load islands from database
                islandsManager.load();

                localesManager = new LocalesManager(plugin);
                PlaceholderHandler.register(plugin);

                // Register Listeners
                registerListeners();

                // Load addons
                addonsManager = new AddonsManager(plugin);
                addonsManager.enableAddons();

                // Save islands & players data asynchronously every X minutes
                getSettings().setDatabaseBackupPeriod(10 * 60 * 20);
                plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                    playersManager.save(true);
                    islandsManager.save(true);
                }, getSettings().getDatabaseBackupPeriod(), getSettings().getDatabaseBackupPeriod());
            });
        });
    }



    /**
     * Register listeners
     */
    private void registerListeners() {
        PluginManager manager = getServer().getPluginManager();
        // Player join events
        manager.registerEvents(new JoinLeaveListener(this), this);
        // Panel listener manager
        manager.registerEvents(new PanelListenerManager(), this);
        // Nether portals
        manager.registerEvents(new NetherPortals(this), this);
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
                return BSBDatabase.getDatabase().toString();
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

    private static void setInstance(BSkyBlock plugin) {
        BSkyBlock.plugin = plugin;
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



    /**
     * @return the ranksManager
     */
    public RanksManager getRanksManager() {
        return ranksManager;
    }

}
