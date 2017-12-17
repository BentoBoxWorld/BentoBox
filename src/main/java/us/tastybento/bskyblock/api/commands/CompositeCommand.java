package us.tastybento.bskyblock.api.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;

import us.tastybento.bskyblock.BSkyBlock;

public abstract class CompositeCommand extends Command implements PluginIdentifiableCommand {

    private BSkyBlock plugin = BSkyBlock.getPlugin();

    private String permission = null;
    private boolean onlyPlayer = false;
    private Map<String, CommandArgument> subCommands;

    public CompositeCommand(String label, String description){
        super(label);
        this.setDescription(description);
        this.subCommands = new LinkedHashMap<>();

        this.setup();
    }

    public CompositeCommand(String label, String description, String... aliases) {
        super(label);
        this.setDescription(description);
        this.setAliases(new ArrayList<>(Arrays.asList(aliases)));

        this.subCommands = new LinkedHashMap<>();

        this.setup();
    }

    public abstract void setup();
    public abstract boolean execute(User user, String[] args);

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

    @Override
    public void setPermission(String permission) {
        this.permission = permission;
    }

    @Override
    public String getPermission() {
        return this.permission;
    }

    public boolean isOnlyPlayer() {
        return onlyPlayer;
    }

    public void setOnlyPlayer(boolean onlyPlayer) {
        this.onlyPlayer = onlyPlayer;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        User user = User.getInstance(sender);
        if (permission != null && user.hasPermission(permission)) {
            if (onlyPlayer && user.isPlayer()) {
                if (args.length >= 1) {
                    // Store the latest subCommand found
                    CommandArgument subCommand = null;

                    for (int i = 0; i < args.length; i++) {
                        // get the subcommand corresponding to the label
                        if (subCommand == null) subCommand = getSubCommand(args[i]);
                        else subCommand = subCommand.getSubCommand(args[i]);

                        if (subCommand != null) { // check if this subcommand exists
                            if (!subCommand.hasSubCommmands()) { // if it has not any subcommands
                                if (subCommand.getPermission() != null && user.hasPermission(subCommand.getPermission())) {
                                    if (onlyPlayer && user.isPlayer()) {
                                        subCommand.execute(user, args); //TODO: "cut" the args to only send the needed ones
                                    } else {
                                        user.sendMessage("general.errors.use-in-game");
                                    }
                                } else {
                                    user.sendMessage("general.errors.no-permission");
                                }
                            }
                            // else continue the loop
                            // TODO: adapt this part to make it works with arguments that are not subcommands
                        }
                        // TODO: get the help
                        else {
                            //TODO: say "unknown command"
                        }
                    }
                } else {
                    // No args : execute the default behaviour
                    this.execute(user, args);
                }
            } else {
                user.sendMessage("general.errors.use-in-game");
            }
        } else {
            user.sendMessage("general.errors.no-permission");
        }

        return true;
    }

    @Override
    public BSkyBlock getPlugin() {
        return plugin;
    }
}
