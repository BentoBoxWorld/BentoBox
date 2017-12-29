package us.tastybento.bskyblock.managers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import us.tastybento.bskyblock.api.BSBModule;

public final class CommandsManager {

    private static final boolean DEBUG = false;
    private Map<BSBModule, List<Command>> commands = new LinkedHashMap<>();

    public void registerCommand(BSBModule module, Command command) {
        if (DEBUG)
            Bukkit.getLogger().info("DEBUG: registering command for " + module.getIdentifier() + " - " + command.getLabel());
        List<Command> cmds = new ArrayList<>();
        if (commands.containsKey(module)) {
            cmds = commands.get(module);
        }

        cmds.add(command);
        commands.put(module, cmds);
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

    public void unregisterCommand(Command command) {
        // TODO - is this ever going to be used?
    }

    /**
     * Get all of the commands for this
     * @param module
     * @return list of commands
     */
    public List<Command> getCommands(BSBModule module) {
        return commands.get(module);
    }

    /**
     * Get the command with the label
     * @param label
     * @return the command or null if it is not there
     */
    public Command getCommand(String label) {
        for (List<Command> cmds : commands.values()) {
            for (Command cmd : cmds) {
                if (cmd.getLabel().equals(label) || cmd.getAliases().contains(label)) return cmd;
            }
        }
        return null;
    }

}
