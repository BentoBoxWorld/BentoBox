package us.tastybento.bskyblock.config;

import java.util.HashMap;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.NotSetup.ConfigError;

/**
 * Loads the plugin configuration and the locales.
 * Also provides 
 * 
 * @author Tastybento
 * @author Poslovitch
 */
public class PluginConfig {
    
    /**
     * Loads the plugin configuration and the locales.
     * If there were errors, it setups the commands as "NotSetup" and generates a debug for admins to fix their configuration.
     * @return true if there wasn't any error, otherwise false.
     */
    public static boolean loadPluginConfig(BSkyBlock plugin){
        // Check if the config exists. It shouldn't happen, but the stack trace helps to know why.
        try{
            plugin.getConfig();
        } catch (Exception exception){
            exception.printStackTrace();
        }
        
        // Initialize the errors list
        HashMap<ConfigError, Object> errors = new HashMap<ConfigError, Object>();
        
        //TODO config version
        
        // The order in this file should match the order in config.yml so that it's easy to check that everything is covered
        
        // ********************* General *********************
        Settings.metrics = plugin.getConfig().getBoolean("general.metrics", true);
        Settings.checkUpdates = plugin.getConfig().getBoolean("general.check-updates", true);
        
        loadLocales(plugin);
        Settings.defaultLanguage = plugin.getConfig().getString("general.default-language", "en-US");
        if(!plugin.getLocales().containsKey(Settings.defaultLanguage)) errors.put(ConfigError.UNKNOWN_LANGUAGE, Settings.defaultLanguage);
        
        Settings.useEconomy = plugin.getConfig().getBoolean("general.use-economy", true);
        Settings.startingMoney = plugin.getConfig().getDouble("general.starting-money", 10.0);
        Settings.useControlPanel = plugin.getConfig().getBoolean("general.use-control-panel", true);
        
        // Purge
        Settings.purgeMaxIslandLevel = plugin.getConfig().getInt("general.purge.max-island-level", 50);
        if(Settings.purgeMaxIslandLevel < 0) errors.put(ConfigError.PURGE_ISLAND_LEVEL_TOO_LOW, Settings.purgeMaxIslandLevel);
        Settings.purgeRemoveUserData = plugin.getConfig().getBoolean("general.purge.remove-user-data", false);
        
        // TODO Database
        
        Settings.recoverSuperFlat = plugin.getConfig().getBoolean("general.recover-super-flat", false);
        Settings.muteDeathMessages = plugin.getConfig().getBoolean("general.mute-death-messages", false);
        Settings.ftbAutoActivator = plugin.getConfig().getBoolean("general.FTB-auto-activator", false);
        Settings.allowObsidianScooping = plugin.getConfig().getBoolean("general.allow-obsidian-scooping", true);
        
        // Allow teleport
        Settings.fallingAllowTeleport = plugin.getConfig().getBoolean("general.allow-teleport.falling", true);
        Settings.fallingBlockedCommands = plugin.getConfig().getStringList("general.allow-teleport.falling-blocked-commands");
        Settings.acidAllowTeleport = plugin.getConfig().getBoolean("general.allow-teleport.acid", true);
        Settings.acidBlockedCommands = plugin.getConfig().getStringList("general.allow-teleport.acid-blocked-commands");
        
        // ********************* World *********************
        Settings.worldName = plugin.getConfig().getString("world.world-name", "BSkyBlock");
        //TODO check if it is the same than before
        
        Settings.islandDistance = plugin.getConfig().getInt("world.distance", 200);
        // TODO check if it is the same than before
        if(Settings.islandDistance % 2 != 0) errors.put(ConfigError.NOT_EVEN_ISLAND_DISTANCE, Settings.islandDistance);
        if(Settings.islandDistance < 50) errors.put(ConfigError.ISLAND_DISTANCE_TOO_LOW, Settings.islandDistance);
        
        Settings.islandProtectionRange = plugin.getConfig().getInt("world.protection-range", 100);
        if(Settings.islandProtectionRange % 2 != 0) errors.put(ConfigError.NOT_EVEN_PROTECTION_RANGE, Settings.islandProtectionRange);
        if(Settings.islandProtectionRange < 0) errors.put(ConfigError.PROTECTION_RANGE_TOO_LOW, Settings.islandProtectionRange);
        if(Settings.islandProtectionRange > Settings.islandDistance) errors.put(ConfigError.PROTECTION_RANGE_HIGHER_THAN_ISLAND_DISTANCE, Settings.islandProtectionRange);
        
        Settings.startX = plugin.getConfig().getInt("world.start-x", 0);
        Settings.startZ = plugin.getConfig().getInt("world.start-z", 0);
        Settings.islandHeight = plugin.getConfig().getInt("world.island-height", 120);
        if(Settings.islandHeight < 5) errors.put(ConfigError.ISLAND_HEIGHT_TOO_LOW, Settings.islandHeight);
        Settings.seaHeight = plugin.getConfig().getInt("world.sea-height", 0);
        
        Settings.maxIslands = plugin.getConfig().getInt("world.max-islands", 0);
        
        // Nether
        Settings.netherGenerate = plugin.getConfig().getBoolean("world.nether.generate", true);
        Settings.netherIslands = plugin.getConfig().getBoolean("world.nether.islands", true);
        Settings.netherTrees = plugin.getConfig().getBoolean("world.nether.trees", true);
        Settings.netherRoof = plugin.getConfig().getBoolean("world.nether.roof", true);
        Settings.netherSpawnRadius = plugin.getConfig().getInt("world.nether.spawn-radius", 25);
        if(!Settings.netherIslands){
            // If the nether is vanilla
            if(Settings.netherSpawnRadius < 0) errors.put(ConfigError.NETHER_SPAWN_RADIUS_TOO_LOW, Settings.netherSpawnRadius);
            if(Settings.netherSpawnRadius > 100) errors.put(ConfigError.NETHER_SPAWN_RADIUS_TOO_HIGH, Settings.netherSpawnRadius);
        }
        
        // Entities
        
        //TODO end loading
        
        //TODO not setup error report
        
        return true;
    }
    
    public static void loadLocales(BSkyBlock plugin){
        //TODO Imperatively load en-US locale
    }
}
