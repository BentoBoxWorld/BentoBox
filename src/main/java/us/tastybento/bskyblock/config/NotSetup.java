package us.tastybento.bskyblock.config;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * This class runs when the config file is not set up enough, or is unsafe.
 * It provides useful information to the admin on what is wrong.
 * 
 * @author Tastybento
 * @author Poslovitch
 */
public class NotSetup implements CommandExecutor{

    public enum ConfigError {
        DIFFERENT_WORLDNAME(0, 001),
        DIFFERENT_ISLAND_DISTANCE(0, 002),
        PROTECTION_RANGE_HIGHER_THAN_ISLAND_DISTANCE(1, 101),
        UNKNOWN_LANGUAGE(2, 201),
        NOT_CHUNK_ISLAND_DISTANCE(2, 202),
        NOT_EVEN_PROTECTION_RANGE(2, 203),
        PURGE_ISLAND_LEVEL_TOO_LOW(3, 301),
        ISLAND_DISTANCE_TOO_LOW(3, 302),
        PROTECTION_RANGE_TOO_LOW(3, 303),
        ISLAND_HEIGHT_TOO_LOW(3, 304),
        NETHER_SPAWN_RADIUS_TOO_LOW(3, 305),
        NETHER_SPAWN_RADIUS_TOO_HIGH(3, 306);
        
        /*
         * Priority:
         * 0 - CRITICAL
         * 1 - HIGH
         * 2 - MEDIUM
         * 3 - LOW
         */
        private int priority;
        private int id;

        ConfigError(int priority, int id){
            this.priority = priority;
            this.id = id;
        }
        
        public static ConfigError getById(int id){
            for(ConfigError e : ConfigError.values()){
                if(e.id == id) return e;
            }
            return null;
        }
    }
    
    private BSkyBlock plugin;
    private List<Error> errors;
    
    /**
     * Handles plugin operation if a critical config-related issue happened
     * 
     * @param plugin
     * @param errors
     */
    public NotSetup(BSkyBlock plugin, List<Error> errors){
        this.plugin = plugin;
        this.errors = errors;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        return true;
    }
}
