package world.bentobox.bentobox;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.commands.BentoBoxCommand;
import world.bentobox.bentobox.listeners.BannedVisitorCommands;
import world.bentobox.bentobox.listeners.BlockEndDragon;
import world.bentobox.bentobox.listeners.DeathListener;
import world.bentobox.bentobox.listeners.JoinLeaveListener;
import world.bentobox.bentobox.listeners.NetherPortals;
import world.bentobox.bentobox.listeners.ObsidianToLava;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.managers.SchemsManager;
import world.bentobox.bentobox.util.HeadGetter;

/**
 * Main BentoBox class
 * @author tastybento, Poslovitch
 */
public class BentoBox extends JavaPlugin {
    private static BentoBox instance;

    // Databases
    private PlayersManager playersManager;
    private IslandsManager islandsManager;

    // Managers
    private CommandsManager commandsManager;
    private LocalesManager localesManager;
    private AddonsManager addonsManager;
    private FlagsManager flagsManager;
    private IslandWorldManager islandWorldManager;
    private RanksManager ranksManager;
    private SchemsManager schemsManager;

    // Settings
    private Settings settings;

    // Notifier
    private Notifier notifier;

    private HeadGetter headGetter;

    private boolean isLoaded;

    @Override
    public void onEnable(){
        // Not loaded
        isLoaded = false;
        // Store the current millis time so we can tell how many ms it took for BSB to fully load.
        final long startMillis = System.currentTimeMillis();

        // Save the default config from config.yml
        saveDefaultConfig();
        setInstance(this);
        // Load Flags
        flagsManager = new FlagsManager(this);

        // Load settings from config.yml. This will check if there are any issues with it too.
        settings = new Config<>(this, Settings.class).loadConfigObject("");
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

        // Load Notifier
        notifier = new Notifier();

        // Set up command manager
        commandsManager = new CommandsManager();

        // Load BentoBox commands
        new BentoBoxCommand();

        // Start Island Worlds Manager
        islandWorldManager = new IslandWorldManager(instance);
        // Load schems manager
        schemsManager = new SchemsManager(instance);

        // Locales manager must be loaded before addons
        localesManager = new LocalesManager(instance);

        // Load addons. Addons may load worlds, so they must go before islands are loaded.
        addonsManager = new AddonsManager(instance);
        addonsManager.loadAddons();
        // Enable addons
        addonsManager.enableAddons();

        getServer().getScheduler().runTask(instance, () -> {
            // Register Listeners
            registerListeners();

            // Load islands from database - need to wait until all the worlds are loaded
            islandsManager.load();

            // Save islands & players data asynchronously every X minutes
            instance.getServer().getScheduler().runTaskTimer(instance, () -> {
                playersManager.save(true);
                islandsManager.save(true);
            }, getSettings().getDatabaseBackupPeriod() * 20 * 60L, getSettings().getDatabaseBackupPeriod() * 20 * 60L);
            isLoaded = true;
            flagsManager.registerListeners();
            instance.log("#############################################");
            instance.log(instance.getDescription().getFullName() + " has been fully enabled.");
            instance.log("It took: " + (System.currentTimeMillis() - startMillis + "ms"));
            instance.log("Thanks for using our plugin !");
            instance.log("- Tastybento and Poslovitch, 2017-2018");
            instance.log("#############################################");

            // Load metrics

            if (settings.isMetrics()) {
                BStats bStats = new BStats(this);
                bStats.registerMetrics();
            }

            // Fire plugin ready event
            Bukkit.getServer().getPluginManager().callEvent(new BentoBoxReadyEvent());
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
        // End dragon blocking
        manager.registerEvents(new BlockEndDragon(this), this);
        // Banned visitor commands
        manager.registerEvents(new BannedVisitorCommands(this), this);
        // Death counter
        manager.registerEvents(new DeathListener(this), this);
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
        // Save settings
        if (settings != null) {
            new Config<>(this, Settings.class).saveConfigObject(settings);
        }
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

    private static void setInstance(BentoBox plugin) {
        BentoBox.instance = plugin;
    }

    public static BentoBox getInstance() {
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

    public void logDebug(Object object) {
        getLogger().info(() -> "DEBUG: " + object);
    }

    public void logError(String error) {
        getLogger().severe(() -> error);
    }

    public void logWarning(String warning) {
        getLogger().warning(warning);
    }

    /**
     * Registers a world as a world to be covered by this plugin
     * @param world - Bukkit overworld
     * @param worldSettings - settings for this world
     */
    public void registerWorld(World world, WorldSettings worldSettings) {
        islandWorldManager.addWorld(world, worldSettings);
    }

    /**
     * @return the schemsManager
     */
    public SchemsManager getSchemsManager() {
        return schemsManager;
    }

    /**
     * True if the plugin is loaded and ready
     * @return the isLoaded
     */
    public boolean isLoaded() {
        return isLoaded;
    }
}
