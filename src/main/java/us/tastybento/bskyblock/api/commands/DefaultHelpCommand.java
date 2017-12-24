package us.tastybento.bskyblock.api.commands;

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
                if (user.hasPermission(parent.getPermission()) && user.hasPermission(parent.getPermission())) {
                    user.sendMessage(parent.getUsage());
                }
            } else if (!parent.isOnlyPlayer() && !parent.isOnlyPlayer()) {
                // Console. Only show if it is a console command
                user.sendMessage(parent.getUsage()); 
            }
            // Run through any subcommands
            for (CompositeCommand subCommand : parent.getSubCommands().values()) {
                // Ignore the help command
                if (!subCommand.getLabel().equals("help")) {
                    if (user.isPlayer()) {
                        // Player. Check perms
                        if (user.hasPermission(parent.getPermission()) && user.hasPermission(subCommand.getPermission())) {
                            user.sendMessage(subCommand.getUsage());
                        }
                    } else if (!subCommand.isOnlyPlayer()) {
                        // Console
                        user.sendMessage(subCommand.getUsage()); 
                    }
                }
            }
        }
        return true;
    }

}
