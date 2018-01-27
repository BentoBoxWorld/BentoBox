package us.tastybento.bskyblock.managers;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

public final class CommandsManager {

    private static final boolean DEBUG = false;
    private HashMap<String, Command> commands = new HashMap<>();

    public void registerCommand(Command command) {
        if (DEBUG)
            Bukkit.getLogger().info("DEBUG: registering command - " + command.getLabel());
       commands.put(command.getLabel(), command);
        // Use reflection to obtain the commandMap method in Bukkit's server. It used to be visible, but isn't anymore.
        try{
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
            commandMap.register(command.getLabel(), command);
        }
        catch(Exception exception){
            Bukkit.getLogger().severe("Bukkit server commandMap method is not there! This means no commands can be registered!");
        }     
    }

    public Command getCommand(String command) {
        return commands.get(command);
    }

}
