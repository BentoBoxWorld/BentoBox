package us.tastybento.bskyblock;

import java.io.File;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import us.tastybento.bskyblock.api.BSBModule;
import us.tastybento.bskyblock.commands.AdminCommand;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.config.PluginConfig;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.PlayersManager;
import us.tastybento.bskyblock.database.managers.island.IslandsManager;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.listeners.JoinLeaveListener;
import us.tastybento.bskyblock.listeners.PanelListener;
import us.tastybento.bskyblock.managers.AddonsManager;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.LocalesManager;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.nms.NMSAbstraction;

/**
 * Main BSkyBlock class - provides an island minigame in the sky
 * @author Tastybento
 * @author Poslovitch
 */
public class BSkyBlock extends JavaPlugin implements BSBModule {

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

    @Override
    public void onEnable(){
        plugin = this;

        // Load configuration and locales. If there are no errors, load the plugin.
        if(PluginConfig.loadPluginConfig(this)){
            
            playersManager = new PlayersManager(this);
            islandsManager = new IslandsManager(this);

            // Only load metrics if set to true in config

            if(Settings.metrics) {
                metrics = new Metrics(plugin);

                registerCustomCharts();

            }

            // Set up commands
            commandsManager = new CommandsManager();
            commandsManager.registerCommand(this, new IslandCommand());
            commandsManager.registerCommand(this, new AdminCommand());

            // These items have to be loaded when the server has done 1 tick.
            // Note Worlds are not loaded this early, so any Locations or World reference will be null
            // at this point. Therefore, the 1 tick scheduler is required.
            getServer().getScheduler().runTask(this, new Runnable() {

                @Override
                public void run() {
                    // Create the world if it does not exist
                    new IslandWorld(plugin);
                    
                    getServer().getScheduler().runTask(plugin, new Runnable() {

                        @Override
                        public void run() {
                            // Load islands from database
                            islandsManager.load();

                            // TODO: load these from config.yml
                            Settings.chestItems = new ItemStack[] {
                                    new ItemStack(Material.LAVA_BUCKET,1),
                                    new ItemStack(Material.ICE,2),
                                    new ItemStack(Material.MELON_SEEDS,1),
                                    new ItemStack(Material.BONE,2),
                                    new ItemStack(Material.COBBLESTONE,5),
                                    new ItemStack(Material.SAPLING,2)
                            };

                            Settings.defaultLanguage = "en-US";
                            localesManager = new LocalesManager(plugin);
                            localesManager.registerLocales(plugin);

                            // Register Listeners
                            registerListeners();
                            
                            // Load addons
                            addonsManager = new AddonsManager();
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
                            Settings.databaseBackupPeriod = 10 * 60 * 20;
                            plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {

                                @Override
                                public void run() {
                                    playersManager.save(true);
                                    islandsManager.save(true);
                                }
                            }, Settings.databaseBackupPeriod, Settings.databaseBackupPeriod);
                        }
                    });
                } 
            });
        }
    }

    private void registerListeners() {
        PluginManager manager = getServer().getPluginManager();
        // Player join events
        manager.registerEvents(new JoinLeaveListener(this), this);
        manager.registerEvents(new PanelListener(this), this);
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

    public NMSAbstraction getNMSHandler() {
        NMSAbstraction nmsHandler = null;
        try {
            nmsHandler = Util.getNMSHandler();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return nmsHandler;
    }

    public CommandsManager getCommandsManager() {
        return commandsManager;
    }

    public LocalesManager getLocalesManager() {
        return localesManager;
    }

    @Override
    public final String getIdentifier() {
        return getDescription().getName();
    }

    @Override
    public final boolean isAddon() {
        return false;
    }

    @Override
    public final File getFolder() {
        return getDataFolder();
    }
}
