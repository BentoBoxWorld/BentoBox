package world.bentobox.bentobox;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.commands.BentoBoxCommand;
import world.bentobox.bentobox.hooks.MultiverseCoreHook;
import world.bentobox.bentobox.hooks.PlaceholderAPIHook;
import world.bentobox.bentobox.hooks.VaultHook;
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
import world.bentobox.bentobox.managers.HooksManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.managers.WorldsManager;
import world.bentobox.bentobox.util.heads.HeadGetter;

/**
 * Main BentoBox class
 * @author tastybento, Poslovitch
 */
public class BentoBox extends JavaPlugin {
    private static BentoBox instance;

    // Databases
    private PlayersManager playersManager;

    // Managers
    private WorldsManager worldsManager;
    private CommandsManager commandsManager;
    private LocalesManager localesManager;
    private AddonsManager addonsManager;
    private FlagsManager flagsManager;
    private RanksManager ranksManager;
    private HooksManager hooksManager;
    private PlaceholdersManager placeholdersManager;

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
        if (settings == null) {
            // Settings did no load correctly. Disable plugin.
            logError("Settings did not load correctly - disabling plugin - please check config.yml");
            getPluginLoader().disablePlugin(this);
            return;
        }
        // Start Database managers
        playersManager = new PlayersManager(this);
        // Check if this plugin is now disabled (due to bad database handling)
        if (!this.isEnabled()) {
            return;
        }
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
        worldsManager = new WorldsManager(this);

        // Locales manager must be loaded before addons
        localesManager = new LocalesManager(this);

        // Load addons. Addons may load worlds, so they must go before islands are loaded.
        addonsManager = new AddonsManager(this);
        addonsManager.loadAddons();
        // Enable addons
        addonsManager.enableAddons();

        getServer().getScheduler().runTask(instance, () -> {
            // Register Listeners
            registerListeners();

            // Load islands from database - need to wait until all the worlds are loaded
            worldsManager.getGameWorldsList().forEach(gameWorld -> gameWorld.getIslands().load());

            // Save islands & players data asynchronously every X minutes
            instance.getServer().getScheduler().runTaskTimer(instance, () -> {
                playersManager.save(true);
                worldsManager.getGameWorldsList().forEach(gameWorld -> gameWorld.getIslands().save(true));
            }, getSettings().getDatabaseBackupPeriod() * 20 * 60L, getSettings().getDatabaseBackupPeriod() * 20 * 60L);

            // Make sure all flag listeners are registered.
            flagsManager.registerListeners();

            // Load metrics
            if (settings.isMetrics()) {
                BStats bStats = new BStats(this);
                bStats.registerMetrics();
            }

            // Load hooks
            hooksManager = new HooksManager(this);
            hooksManager.registerHook(new VaultHook());
            hooksManager.registerHook(new PlaceholderAPIHook());
            hooksManager.registerHook(new MultiverseCoreHook());

            // Make sure all worlds are already registered to Multiverse.
            worldsManager.registerWorldsToMultiverse();

            // Setup the Placeholders manager
            placeholdersManager = new PlaceholdersManager(this);

            // Fire plugin ready event
            isLoaded = true;
            Bukkit.getServer().getPluginManager().callEvent(new BentoBoxReadyEvent());

            instance.log("#############################################");
            instance.log(instance.getDescription().getFullName() + " has been fully enabled.");
            instance.log("It took: " + (System.currentTimeMillis() - startMillis + "ms"));
            instance.log("Thanks for using our plugin !");
            instance.log("- Tastybento and Poslovitch, 2017-2018");
            instance.log("#############################################");
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
        worldsManager.getGameWorldsList().forEach(gameWorld -> gameWorld.getIslands().shutdown());
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
     * @return the WorldsManager
     */
    public WorldsManager getWorlds() {
        return worldsManager;
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
     * Returns whether BentoBox is fully loaded or not.
     * This basically means that all managers are instantiated and can therefore be safely accessed.
     * @return whether BentoBox is fully loaded or not.
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * @return the HooksManager
     */
    public HooksManager getHooks() {
        return hooksManager;
    }

    /**
     * Convenience method to get the VaultHook.
     * @return the Vault hook
     */
    public Optional<VaultHook> getVault() {
        return Optional.ofNullable((VaultHook) hooksManager.getHook("Vault").orElse(null));
    }

    /**
     * @return the PlaceholdersManager.
     */
    public PlaceholdersManager getPlaceholdersManager() {
        return placeholdersManager;
    }
}
