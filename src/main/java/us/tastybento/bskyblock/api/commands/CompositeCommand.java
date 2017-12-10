package us.tastybento.bskyblock.api.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import us.tastybento.bskyblock.BSkyBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class CompositeCommand extends Command implements PluginIdentifiableCommand {

    private BSkyBlock plugin = BSkyBlock.getPlugin();

    private Map<String, CommandArgument> subCommands;

    public CompositeCommand(String label, String description){
        super(label);
        this.setDescription(description);

        this.subCommands = new LinkedHashMap<>();

        this.setup();

        plugin.getNMSHandler().getServerCommandMap().register(label, this);
    }

    public CompositeCommand(String label, String description, String... aliases) {
        super(label);
        this.setDescription(description);
        this.setAliases(new ArrayList<String>(Arrays.asList(aliases)));

        this.subCommands = new LinkedHashMap<>();

        this.setup();

        plugin.getNMSHandler().getServerCommandMap().register(label, this);
    }

    public abstract void setup();

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

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        commandSender.sendMessage("Hi! I'm working!");
        return false;
    }

    @Override
    public BSkyBlock getPlugin() {
        return plugin;
    }
}
