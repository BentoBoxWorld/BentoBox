package world.bentobox.bentobox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.commands.BentoBoxCommand;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.hooks.FancyNpcsHook;
import world.bentobox.bentobox.hooks.ItemsAdderHook;
import world.bentobox.bentobox.hooks.MultipaperHook;
import world.bentobox.bentobox.hooks.MultiverseCore4Hook;
import world.bentobox.bentobox.hooks.MultiverseCore5Hook;
import world.bentobox.bentobox.hooks.MyWorldsHook;
import world.bentobox.bentobox.hooks.MythicMobsHook;
import world.bentobox.bentobox.hooks.OraxenHook;
import world.bentobox.bentobox.hooks.SlimefunHook;
import world.bentobox.bentobox.hooks.VaultHook;
import world.bentobox.bentobox.hooks.ZNPCsPlusHook;
import world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook;
import world.bentobox.bentobox.listeners.BannedCommands;
import world.bentobox.bentobox.listeners.BlockEndDragon;
import world.bentobox.bentobox.listeners.DeathListener;
import world.bentobox.bentobox.listeners.JoinLeaveListener;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.listeners.PrimaryIslandListener;
import world.bentobox.bentobox.listeners.StandardSpawnProtectionListener;
import world.bentobox.bentobox.listeners.teleports.EntityTeleportListener;
import world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.HooksManager;
import world.bentobox.bentobox.managers.IslandDeletionManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.managers.WebManager;
import world.bentobox.bentobox.util.ExpiringMap;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.heads.HeadGetter;
import world.bentobox.bentobox.versions.ServerCompatibility;

/**
 * Main BentoBox class
 * @author tastybento, Poslovitch
 */
public class BentoBox extends JavaPlugin implements Listener {

    private static final String PANELS = "panels";

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
    private BlueprintsManager blueprintsManager;
    private HooksManager hooksManager;
    private PlaceholdersManager placeholdersManager;
    private IslandDeletionManager islandDeletionManager;
    private WebManager webManager;

    // Settings
    private Settings settings;

    // Notifier
    private Notifier notifier;
    
    // Click limiter
    private ExpiringMap<Pair<UUID, String>, Boolean> lastClick ;

    private HeadGetter headGetter;

    private boolean isLoaded;

    // Metrics
    @Nullable
    private BStats metrics;

    private Config<Settings> configObject;

    private BukkitTask blueprintLoadingTask;

    private boolean shutdown;

    @Override
    public void onEnable(){
        setInstance(this);

        if (!ServerCompatibility.getInstance().checkCompatibility().isCanLaunch()) {
            // The server's most likely incompatible.
            // Show a warning
            logWarning("************ Disclaimer **************");
            logWarning("BentoBox may not be compatible with this server!");
            logWarning("BentoBox is tested only on the following Paper versions:");

            List<String> versions = ServerCompatibility.ServerVersion.getVersions(ServerCompatibility.Compatibility.COMPATIBLE, ServerCompatibility.Compatibility.SUPPORTED)
                    .stream().map(ServerCompatibility.ServerVersion::toString).toList();

            logWarning(String.join(", ", versions));
            logWarning("**************************************");
        }

        // Not loaded
        isLoaded = false;
        // Store the current millis time so we can tell how many ms it took for BSB to fully load.
        final long loadStart = System.currentTimeMillis();

        // Save the default config from config.yml
        saveDefaultConfig();
        // Load Flags
        flagsManager = new FlagsManager(this);

        if (!loadSettings()) {
            // We're aborting the load.
            return;
        }
        // Saving the config now.
        saveConfig();
        
        // Set up click timeout
        lastClick = new ExpiringMap<>(getSettings().getClickCooldownMs(), TimeUnit.MILLISECONDS);

        // Start Database managers
        playersManager = new PlayersManager(this);
        // Check if this plugin is now disabled (due to bad database handling)
        if (!this.isEnabled()) {
            return;
        }
        islandsManager = new IslandsManager(this);

        // Start head getter
        headGetter = new HeadGetter(this);

        // Load Notifier
        notifier = new Notifier();

        // Set up command manager
        commandsManager = new CommandsManager();

        // Load BentoBox commands
        new BentoBoxCommand();

        // Start Island Worlds Manager
        islandWorldManager = new IslandWorldManager(this);

        // Load blueprints manager
        blueprintsManager = new BlueprintsManager(this);

        // Locales manager must be loaded before addons
        localesManager = new LocalesManager(this);

        // Load hooks
        hooksManager = new HooksManager(this);

        // Load addons. Addons may load worlds, so they must go before islands are loaded.
        addonsManager = new AddonsManager(this);
        addonsManager.loadAddons();

        final long loadTime = System.currentTimeMillis() - loadStart;

        Bukkit.getScheduler().runTask(instance, () -> {
            try {
                completeSetup(loadTime);
            } catch (Exception e) {
                fireCriticalError(e.getMessage(), "");
                logStacktrace(e);
            }
        });
    }

    private void completeSetup(long loadTime) {
        final long enableStart = System.currentTimeMillis();

        hooksManager.registerHook(new MultipaperHook());

        hooksManager.registerHook(new VaultHook());

        // FancyNpcs
        hooksManager.registerHook(new FancyNpcsHook());
        // ZNPCsPlus
        hooksManager.registerHook(new ZNPCsPlusHook());

        // MythicMobs
        hooksManager.registerHook(new MythicMobsHook());

        hooksManager.registerHook(new PlaceholderAPIHook());
        // Setup the Placeholders manager
        placeholdersManager = new PlaceholdersManager(this);

        // Enable addons
        addonsManager.enableAddons();

        // Register default gamemode placeholders
        addonsManager.getGameModeAddons().forEach(placeholdersManager::registerDefaultPlaceholders);

        // Register Listeners
        registerListeners();

        // Load islands from database - need to wait until all the worlds are loaded
        log("Loading islands from database...");
        try {
            islandsManager.load();
        } catch (Exception e) {
            fireCriticalError(e.getMessage(), "Could not load islands!");
            return;
        }

        // Make sure all flag listeners are registered.
        flagsManager.registerListeners();

        // Load metrics
        metrics = new BStats(this);
        metrics.registerMetrics();

        // Register Multiverse hook - MV loads AFTER BentoBox
        // Make sure all worlds are already registered to Multiverse.
        if (hasClass("org.mvplugins.multiverse.core.MultiverseCore")) {
            hooksManager.registerHook(new MultiverseCore5Hook());
        } else if (hasClass("com.onarandombox.MultiverseCore.MultiverseCore")) {
            hooksManager.registerHook(new MultiverseCore4Hook());
        }
        hooksManager.registerHook(new MyWorldsHook());
        islandWorldManager.registerWorldsToMultiverse(true);

        // Register Slimefun
        hooksManager.registerHook(new SlimefunHook());

        // Register ItemsAdder
        hooksManager.registerHook(new ItemsAdderHook(this));
        
        // Register Oraxen
        hooksManager.registerHook(new OraxenHook(this));

        // TODO: re-enable after implementation
        //hooksManager.registerHook(new DynmapHook());
        // TODO: re-enable after rework
        //hooksManager.registerHook(new LangUtilsHook());

        webManager = new WebManager(this);

        final long enableTime = System.currentTimeMillis() - enableStart;

        // Show banner
        User.getInstance(Bukkit.getConsoleSender()).sendMessage("successfully-loaded",
                TextVariables.VERSION, instance.getDescription().getVersion(),
                "[time]", String.valueOf(loadTime + enableTime));

        // Poll for blueprints loading to be finished - async so could be a completely variable time
        blueprintLoadingTask = Bukkit.getScheduler().runTaskTimer(instance, () -> {
            if (getBlueprintsManager().isBlueprintsLoaded()) {
                blueprintLoadingTask.cancel();
                // Tell all addons that everything is loaded
                isLoaded = true;
                this.addonsManager.allLoaded();
                // Run ready commands
                settings.getReadyCommands().forEach(cmd -> Bukkit.getServer().dispatchCommand(getServer().getConsoleSender(), cmd));
                // Fire plugin ready event - this should go last after everything else
                Bukkit.getPluginManager().callEvent(new BentoBoxReadyEvent());
                instance.log("All blueprints loaded.");
            }
        }, 0L, 1L);

        if (getSettings().getDatabaseType().equals(DatabaseSetup.DatabaseType.YAML)) {
            logWarning("*** You're still using YAML database ! ***");
            logWarning("This database type is being deprecated from BentoBox as some official addons encountered difficulties supporting it correctly.");
            logWarning("You should switch ASAP to an alternative database type. Please refer to the comments in BentoBox's config.yml.");
            logWarning("There is NO guarantee YAML database will remain properly supported in the following updates, and its usage should as such be considered a non-viable situation.");
            logWarning("*** *** *** *** *** *** *** *** *** *** ***");
        }
    }

    private boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void fireCriticalError(String message, String error) {
        logError("*****************CRITICAL ERROR!******************");
        logError(message);
        logError(error + " Disabling BentoBox...");
        logError("*************************************************");
        // Stop all addons
        if (addonsManager != null) {
            addonsManager.disableAddons();
        }
        // Do not save players or islands, just shutdown
        shutdown = true;
        instance.setEnabled(false);
    }

    /**
     * Registers listeners.
     */
    private void registerListeners() {
        PluginManager manager = getServer().getPluginManager();
        // Player join events
        manager.registerEvents(new JoinLeaveListener(this), this);
        // Panel listener manager
        manager.registerEvents(new PanelListenerManager(), this);
        // Standard Nether/End spawns protection
        manager.registerEvents(new StandardSpawnProtectionListener(this), this);
        // Player portals
        manager.registerEvents(new PlayerTeleportListener(this), this);
        // Entity portals
        manager.registerEvents(new EntityTeleportListener(this), this);
        // End dragon blocking
        manager.registerEvents(new BlockEndDragon(this), this);
        // Banned visitor commands
        manager.registerEvents(new BannedCommands(this), this);
        // Death counter
        manager.registerEvents(new DeathListener(this), this);
        // MV unregister
        manager.registerEvents(this, this);
        // Island Delete Manager
        islandDeletionManager = new IslandDeletionManager(this);
        manager.registerEvents(islandDeletionManager, this);
        // Primary Island Listener
        manager.registerEvents(new PrimaryIslandListener(this), this);
    }

    @Override
    public void onDisable() {
        // Stop all async database tasks
        shutdown = true;

        HeadGetter.shutdown();

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

    /**
     * Returns the player manager
     * @return the player manager
     * @see #getPlayersManager()
     */
    public PlayersManager getPlayers() {
        return playersManager;
    }

    /**
     * Returns the player manager
     * @return the player manager
     * @see #getPlayers()
     * @since 1.16.0
     */
    public PlayersManager getPlayersManager() {
        return getPlayers();
    }

    /**
     * Returns the island manager
     * @return the island manager
     * @see #getIslandsManager()
     */
    public IslandsManager getIslands() {
        return islandsManager;
    }

    /**
     * Returns the island manager
     * @return the island manager
     * @see #getIslands()
     * @since 1.16.0
     */
    public IslandsManager getIslandsManager() {
        return getIslands();
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
     * @deprecated Just use {@code RanksManager.getInstance()}
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public RanksManager getRanksManager() {
        return RanksManager.getInstance();
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
     * Loads the settings from the config file.
     * If it fails, it can shut the plugin down.
     * @return {@code true} if it loaded successfully.
     * @since 1.3.0
     */
    public boolean loadSettings() {
        log("Loading Settings from config.yml...");
        // Load settings from config.yml. This will check if there are any issues with it too.
        if (configObject == null) configObject = new Config<>(this, Settings.class);
        settings = configObject.loadConfigObject();
        if (settings == null) {
            // Settings did not load correctly. Disable plugin.
            logError("Settings did not load correctly - disabling plugin - please check config.yml");
            getPluginLoader().disablePlugin(this);
            return false;
        }
        log("Saving default panels...");

        if (!Files.exists(Path.of(this.getDataFolder().getPath(), PANELS, "island_creation_panel.yml"))) {
            log("Saving default island_creation_panel...");
            this.saveResource("panels/island_creation_panel.yml", false);
        }

        if (!Files.exists(Path.of(this.getDataFolder().getPath(), PANELS, "language_panel.yml"))) {
            log("Saving default language_panel...");
            this.saveResource("panels/language_panel.yml", false);
        }

        if (!Files.exists(Path.of(this.getDataFolder().getPath(), PANELS, "island_homes_panel.yml"))) {
            log("Saving default island_homes_panel...");
            this.saveResource("panels/island_homes_panel.yml", false);
        }

        if (!Files.exists(Path.of(this.getDataFolder().getPath(), PANELS, "team_invite_panel.yml"))) {
            log("Saving default team_invite_panel...");
            this.saveResource("panels/team_invite_panel.yml", false);
        }

        if (!Files.exists(Path.of(this.getDataFolder().getPath(), PANELS, "team_panel.yml"))) {
            log("Saving default team_panel...");
            this.saveResource("panels/team_panel.yml", false);
        }

        return true;
    }

    @Override
    public void saveConfig() {
        if (settings != null) configObject.saveConfigObject(settings);
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

    /**
     * Logs the stacktrace of a Throwable that was thrown by an error.
     * It should be used preferably instead of {@link Throwable#printStackTrace()} as it does not risk exposing sensitive information.
     * @param throwable the Throwable that was thrown by an error.
     * @since 1.3.0
     */
    public void logStacktrace(@NonNull Throwable throwable) {
        logError(ExceptionUtils.getStackTrace(throwable));
    }

    public void logWarning(String warning) {
        getLogger().warning(() -> warning);
    }

    /**
     * Returns the instance of the {@link BlueprintsManager}.
     * @return the {@link BlueprintsManager}.
     * @since 1.5.0
     */
    public BlueprintsManager getBlueprintsManager() {
        return blueprintsManager;
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

    /**
     * @return the islandDeletionManager
     * @since 1.1
     */
    public IslandDeletionManager getIslandDeletionManager() {
        return islandDeletionManager;
    }

    /**
     * @return an optional of the Bstats instance
     * @since 1.1
     */
    @NonNull
    public Optional<BStats> getMetrics() {
        return Optional.ofNullable(metrics);
    }

    /**
     * @return the {@link WebManager}.
     * @since 1.5.0
     */
    public WebManager getWebManager() {
        return webManager;
    }

    // Overriding default JavaPlugin methods

    /* (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#getDefaultWorldGenerator(java.lang.String, java.lang.String)
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NonNull String worldName, String id) {
        return addonsManager.getDefaultWorldGenerator(worldName, id);
    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#reloadConfig()
     */
    @Override
    public void reloadConfig() {
        loadSettings();
    }

    /**
     * Check if plug has shutdown. Used to close databases that are running async.
     * @return true if plugin has shutdown
     * @since 1.13.0
     */
    public boolean isShutdown() {
        return shutdown;
    }
    
    /**
     * Checks if a user can click a GUI or needs to slow down
     * @param user user
     * @return false if they can click and the timeout is started, otherwise true.
     */
    public boolean onTimeout(User user, Panel panel) {
        if (lastClick.containsKey(new Pair<>(user.getUniqueId(), panel.getName()))) {
            user.notify("general.errors.slow-down");
            return true;
        }
        lastClick.put(new Pair<>(user.getUniqueId(), panel.getName()), true);
        return false;
    }
}
