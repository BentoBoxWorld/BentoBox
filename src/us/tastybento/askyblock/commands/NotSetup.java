package us.tastybento.askyblock.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import us.tastybento.askyblock.ASkyBlock;
import us.tastybento.askyblock.util.Util;

/**
 * This class runs when the config file is not set up enough, or is unsafe.
 * It provides useful information to the admin on what is wrong.
 * 
 * @author Tastybento
 */
public class NotSetup implements CommandExecutor{

    public enum Reason {
        DISTANCE, GENERATOR, WORLD_NAME, OUTDATED;
    }
    
    private ASkyBlock plugin;
    private Reason reason;
    
    /**
     * Handles plugin operation if a critical config-related issue happened
     * 
     * @param reason
     */
    public NotSetup(ASkyBlock plugin, Reason reason){
        this.reason = reason;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Util.sendMessage(sender, ChatColor.RED + plugin.getLocale(sender).notSetupHeader);
        switch (reason) {
        case DISTANCE:
            Util.sendMessage(sender, ChatColor.RED + plugin.getLocale(sender).notSetupDistance);
            break;
        case GENERATOR:
            Util.sendMessage(sender, ChatColor.RED + plugin.getLocale(sender).notSetupGenerator);
            if(plugin.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) Util.sendMessage(sender, ChatColor.RED + plugin.getLocale(sender).notSetupGeneratorMultiverse);
            break;
        case WORLD_NAME:
            Util.sendMessage(sender, ChatColor.RED + plugin.getLocale(sender).notSetupWorldname);
            break;
        case OUTDATED:
            Util.sendMessage(sender, ChatColor.RED + plugin.getLocale(sender).notSetupOutdated);
            break;
        default:
            break;
        }
        return true;
    }
}
