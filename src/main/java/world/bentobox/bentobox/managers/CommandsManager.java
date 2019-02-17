package world.bentobox.bentobox.managers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;

public class CommandsManager {

    @NonNull
    private Map<@NonNull String, @NonNull CompositeCommand> commands = new HashMap<>();

    public void registerCommand(@NonNull CompositeCommand command) {
        commands.put(command.getLabel(), command);
        // Use reflection to obtain the commandMap method in Bukkit's server.
        try{
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            String commandPrefix = "bentobox";
            if (command.getAddon() != null) {
                commandPrefix = command.getAddon().getDescription().getName().toLowerCase(Locale.ENGLISH);
            }

            commandMap.register(commandPrefix, command);
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
    @Nullable
    public CompositeCommand getCommand(@NonNull String command) {
        return commands.get(command);
    }

    /**
     * @return the commands
     */
    @NonNull
    public Map<String, CompositeCommand> getCommands() {
        return commands;
    }

    /**
     * List all commands registered so far
     * @return set of commands
     */
    @NonNull
    public Set<String> listCommands() {
        return commands.keySet();
    }
}
