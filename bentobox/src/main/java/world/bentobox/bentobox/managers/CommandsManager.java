package world.bentobox.bentobox.managers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;

public class CommandsManager {

    @NonNull
    private Map<@NonNull String, @NonNull CompositeCommand> commands = new HashMap<>();
    private SimpleCommandMap commandMap;

    public void registerCommand(@NonNull CompositeCommand command) {
        commands.put(command.getLabel(), command);
        // Use reflection to obtain the commandMap method in Bukkit's server.
        try{
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (SimpleCommandMap) commandMapField.get(Bukkit.getServer());

            String commandPrefix = "bentobox";
            if (command.getAddon() != null) {
                commandPrefix = command.getAddon().getDescription().getName().toLowerCase(Locale.ENGLISH);
            }
            if (!commandMap.register(commandPrefix, command)) {
                BentoBox.getInstance().logError("Failed to register command " + commandPrefix + " " + command.getLabel());
            }
        }
        catch(Exception exception){
            Bukkit.getLogger().severe("Bukkit server commandMap method is not there! This means no commands can be registered!");
        }
    }

    /**
     * Unregisters all BentoBox registered commands with Bukkit
     */
    public void unregisterCommands() {
        // Use reflection to obtain the knownCommands in the commandMap
        try {
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) commandMap.getClass().getMethod("getKnownCommands").invoke(commandMap);
            //noinspection SuspiciousMethodCalls
            knownCommands.values().removeIf(commands.values()::contains);
            // Not sure if this is needed, but it clears out all references
            commands.values().forEach(c -> c.unregister(commandMap));
            // Zap everything
            commands.clear();
        } catch(Exception e){
            Bukkit.getLogger().severe("Known commands reflection was not possible, BentoBox is now unstable, so restart server!");
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
