package us.tastybento.bskyblock;

import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import us.tastybento.bskyblock.api.configuration.WorldSettings;
import us.tastybento.bskyblock.api.placeholders.PlaceholderHandler;
import us.tastybento.bskyblock.api.user.Notifier;
import us.tastybento.bskyblock.commands.AdminCommand;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.database.BSBDbSetup;
import us.tastybento.bskyblock.listeners.BlockEndDragon;
import us.tastybento.bskyblock.listeners.JoinLeaveListener;
import us.tastybento.bskyblock.listeners.NetherPortals;
import us.tastybento.bskyblock.listeners.ObsidianToLava;
import us.tastybento.bskyblock.listeners.PanelListenerManager;
import us.tastybento.bskyblock.listeners.protection.FlyingMobEvents;
import us.tastybento.bskyblock.managers.AddonsManager;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.FlagsManager;
import us.tastybento.bskyblock.managers.IslandWorldManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.LocalesManager;
import us.tastybento.bskyblock.managers.PlayersManager;
import us.tastybento.bskyblock.managers.RanksManager;
import us.tastybento.bskyblock.util.HeadGetter;

/**
 * Main BSkyBlock class - provides an island minigame in the sky
 * @author tastybento
 * @author Poslovitch
 */
public class BSkyBlock extends JavaPlugin {

    private static BSkyBlock instance;

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
    private IslandWorldManager islandWorldManager;
    private RanksManager ranksManager;

    // Settings
    private Settings settings;

    // Notifier
    private Notifier notifier;

    private HeadGetter headGetter;

    @Override
    public void onEnable(){
        // Save the default config from config.yml
        saveDefaultConfig();
        setInstance(this);

        settings = new Settings();
        // Load settings from config.yml. This will check if there are any issues with it too.
        try {
            settings = settings.loadSettings();
        } catch (Exception e) {
            logError("Settings could not be loaded " + e.getMessage());
        }

        // Save a backup of settings to the database so it can be checked next time
        try {
            settings.saveBackup();
        } catch (Exception e) {
            logError("Settings backup could not be saved" + e.getMessage());
        }

        // Start Database managers
        playersManager = new PlayersManager(this);
        // Check if this plugin is now disabled (due to bad database handling)
        if (!this.isEnabled()) {
            return;
        }
        islandsManager = new IslandsManager(this);
        ranksManager = new RanksManager(this);

        // Start head getter
        headGetter = new HeadGetter(this);
        
        // Load metrics
        metrics = new Metrics(instance);
        registerCustomCharts();

        // Load Notifier
        notifier = new Notifier();

        // Set up commands
        commandsManager = new CommandsManager();
        new IslandCommand();
        new AdminCommand();

        // These items have to be loaded when the server has done 1 tick.
        // Note Worlds are not loaded this early, so any Locations or World reference will be null
        // at this point. Therefore, the 1 tick scheduler is required.
        getServer().getScheduler().runTask(this, () -> {
            // Create the world if it does not exist
            islandWorldManager = new IslandWorldManager(instance);
            
            getServer().getScheduler().runTask(instance, () -> {

                // Load Flags
                flagsManager = new FlagsManager(instance);

                // Load islands from database
                islandsManager.load();

                localesManager = new LocalesManager(instance);
                PlaceholderHandler.register(instance);

                // Register Listeners
                registerListeners();

                // Load addons
                addonsManager = new AddonsManager(instance);
                addonsManager.loadAddons();
                // Enable addons
                addonsManager.enableAddons();

                // Save islands & players data asynchronously every X minutes
                getSettings().setDatabaseBackupPeriod(10 * 60 * 20);
                instance.getServer().getScheduler().runTaskTimer(instance, () -> {
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
        // Obsidian to lava helper
        manager.registerEvents(new ObsidianToLava(this), this);
        // Flying mobs protection
        manager.registerEvents(new FlyingMobEvents(this), this);
        // End dragon blocking
        manager.registerEvents(new BlockEndDragon(this), this);
    }

    @Override
    public void onDisable() {
        if (addonsManager != null) {
            addonsManager.disableAddons();
        }
        // Save data
        if (playersManager != null) {
            playersManager.shutdown();
        }
        if (islandsManager != null) {
            islandsManager.shutdown();
        }
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
                return BSBDbSetup.getDatabase().toString();
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
        BSkyBlock.instance = plugin;
    }

    public static BSkyBlock getInstance() {
        return instance;
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
     * @return the ranksManager
     */
    public RanksManager getRanksManager() {
        return ranksManager;
    }

    /**
     * @return the Island World Manager
     */
    public IslandWorldManager getIWM() {
        return islandWorldManager;
    }

    /**
     * @return the settings
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * @return the notifier
     */
    public Notifier getNotifier() {
        return notifier;
    }

    /**
     * @return the headGetter
     */
    public HeadGetter getHeadGetter() {
        return headGetter;
    }
    
    public void log(String string) {
        getLogger().info(() -> string);
    }
    
    public void logError(String error) {
        getLogger().severe(() -> error);
    }
    
    public void logWarning(String warning) {
        getLogger().warning(warning);
    }



    /**
     * Registers a world as a world to be covered by this plugin
     * @param world - Bukkit over world
     * @param worldSettings - settings for this world
     */
    public void registerWorld(World world, WorldSettings worldSettings) {
        islandWorldManager.addWorld(world, worldSettings);
    }
    
}
