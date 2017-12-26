package us.tastybento.bskyblock.api.commands;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a default help to every command that will show the usage of the command
 * and the usage of any subcommands that the command has.
 * @author ben
 *
 */
public class DefaultHelpCommand extends CompositeCommand {

    private CompositeCommand parent;

    public DefaultHelpCommand(CompositeCommand parent) {
        super(parent, "help");
        this.parent = parent;
    }

    @Override
    public boolean execute(User user, List<String> args) {

        if (args.isEmpty()) { 
            // Show the top level help
            if (user.isPlayer()) {
                // Player. Check perms
                if (user.hasPermission(parent.getPermission())) {
                    user.sendMessage((parent.getUsage().isEmpty() ? "" : parent.getUsage() + " ") + parent.getDescription());
                } else {
                    user.sendMessage("errors.no-permission");
                    return true;
                }

            } else if (!parent.isOnlyPlayer()) {
                // Console. Only show if it is a console command
                user.sendMessage((parent.getUsage().isEmpty() ? "" : parent.getUsage() + " ") + parent.getDescription());
            }
            // Run through any subcommands
            for (CompositeCommand subCommand : parent.getSubCommands().values()) {
                // Ignore the help command
                if (!subCommand.getLabel().equals("help")) {
                    String usage = subCommand.getUsage();
                    String desc = subCommand.getDescription();
                    if (user.isPlayer()) {
                        // Player. Check perms
                        if (user.hasPermission(parent.getPermission()) && user.hasPermission(subCommand.getPermission())) {
                            user.sendMessage((usage.isEmpty() ? "" : usage + " ") + desc);
                        }
                    } else if (!subCommand.isOnlyPlayer()) {
                        user.sendMessage((usage.isEmpty() ? "" : usage + " ") + desc);
                    }
                }
            }
        }
        return true;
    }

}
