package us.tastybento.bskyblock.managers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.BSModule;

public final class CommandsManager {

    private Map<BSModule, List<Command>> commands = new LinkedHashMap<>();

    public void registerCommand(BSModule module, Command command) {
        List<Command> cmds = new ArrayList<>();
        if (commands.containsKey(module)) {
            cmds = commands.get(module);
        }

        cmds.add(command);
        commands.put(module, cmds);
        BSkyBlock.getPlugin().getNMSHandler().getServerCommandMap().register(command.getLabel(), command);
    }

    public void unregisterCommand(Command command) {

    }

    public List<Command> getCommands(BSModule module) {
        return commands.get(module);
    }

    public Command getCommand(String label) {
        for (List<Command> cmds : commands.values()) {
            for (Command cmd : cmds) {
                if (cmd.getLabel().equals(label) || cmd.getAliases().contains(label)) return cmd;
            }
        }
        return null;
    }

}
