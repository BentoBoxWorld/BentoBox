package world.bentobox.bentobox.managers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import world.bentobox.bentobox.api.commands.CompositeCommand;

public class CommandsManager {

    private Map<String, CompositeCommand> commands = new HashMap<>();

    public void registerCommand(CompositeCommand command) {
        commands.put(command.getLabel(), command);
        // Use reflection to obtain the commandMap method in Bukkit's server.
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

    /**
     * Try to get a registered command.
     * @param command - command string
     * @return CompositeCommand or null if it does not exist
     */
    public CompositeCommand getCommand(String command) {
        return commands.get(command);
    }

    /**
     * @return the commands
     */
    public Map<String, CompositeCommand> getCommands() {
        return commands;
    }

    /**
     * List all commands registered so far
     * @return set of commands
     */
    public Set<String> listCommands() {
        return commands.keySet();
    }

}
