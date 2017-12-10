package us.tastybento.bskyblock.api.commands;

import org.bukkit.command.CommandSender;

import java.util.*;

public abstract class CommandArgument {

    private String label;
    private List<String> aliases;
    private Map<String, CommandArgument> subCommands;

    public CommandArgument(String label, String... aliases) {
        this.label = label;
        this.aliases = new ArrayList<>(Arrays.asList(aliases));
        this.subCommands = new LinkedHashMap<>();
    }

    public abstract boolean execute(CommandSender sender, String[] args);
    public abstract Set<String> tabComplete(CommandSender sender, String[] args);

    public String getLabel() {
        return label;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public boolean hasSubCommmands() {
        return !subCommands.isEmpty();
    }

    public Map<String, CommandArgument> getSubCommands() {
        return subCommands;
    }

    public CommandArgument getSubCommand(String label) {
        for (Map.Entry<String, CommandArgument> entry : subCommands.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(label)) return subCommands.get(label);
            else if (entry.getValue().getAliases().contains(label)) return subCommands.get(entry.getValue().getLabel());
        }
        return null;
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
