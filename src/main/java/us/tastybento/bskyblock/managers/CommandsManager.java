package us.tastybento.bskyblock.managers;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import us.tastybento.bskyblock.api.commands.CompositeCommand;

public class CommandsManager {

    private HashMap<String, CompositeCommand> commands = new HashMap<>();

    public void registerCommand(CompositeCommand command) {
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

    public CompositeCommand getCommand(String command) {
        return commands.get(command);
    }

}
