package us.tastybento.bskyblock.api.commands;

import java.util.List;
import java.util.Map;

public class CommandArgument {

    private String label;
    private List<String> aliases;
    private Map<String, CommandArgument> subCommands;

    public String getLabel() {
        return label;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public Map<String, CommandArgument> getSubCommands() {
        return subCommands;
    }

    public CommandArgument getSubCommand(String label) {
        return subCommands.getOrDefault(label, null);
    }

    public void addSubCommand(CommandArgument subCommand) {
        subCommands.putIfAbsent(subCommand.getLabel(), subCommand);
    }

    public void removeSubCommand(String label) {
        subCommands.remove(label);
    }

    public void replaceSubCommand(CommandArgument subCommand) {
        subCommands.put(subCommand.getLabel(), subCommand);
    }
}
