package us.tastybento.bskyblock.api.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.math.NumberUtils;

/**
 * Adds a default help to every command that will show the usage of the command
 * and the usage of any subcommands that the command has.
 * @author tastybento
 *
 */
public class DefaultHelpCommand extends CompositeCommand {

    // TODO: make this a setting
    private static final int MAX_DEPTH = 2;
    private static final String USAGE_PLACEHOLDER = "[usage]";
    private static final String PARAMS_PLACEHOLDER = "[parameters]";
    private static final String DESC_PLACEHOLDER = "[description]";
    private static final String HELP_SYNTAX_REF = "commands.help.syntax";

    public DefaultHelpCommand(CompositeCommand parent) {
        super(parent, "help");
    }

    @Override
    public void setup() {
        // Set the usage to what the parent's command is
        setParameters(parent.getParameters());
        setDescription(parent.getDescription());
    }

    @Override
    public boolean execute(User user, List<String> args) {
        int depth = 0;
        if (args.size() == 1) {
            if (NumberUtils.isDigits(args.get(0))) {
                // Converts first argument into an int, or returns -1 if it cannot. Avoids exceptions.
                depth = Optional.ofNullable(args.get(0)).map(NumberUtils::toInt).orElse(-1);
            } else {
                String usage = user.getTranslation(parent.getUsage());
                String params = user.getTranslation("commands.help.parameters");
                String desc = user.getTranslation("commands.help.description");
                user.sendMessage(HELP_SYNTAX_REF, USAGE_PLACEHOLDER, usage, PARAMS_PLACEHOLDER, params, DESC_PLACEHOLDER, desc);
                return true;
            }
        }
        if (depth == 0) {
            user.sendMessage("commands.help.header");
        }
        //if (args.isEmpty()) {
        if (depth < MAX_DEPTH) {
            if (!parent.getLabel().equals("help")) {
                // Get elements
                String usage = parent.getUsage().isEmpty() ? "" : user.getTranslation(parent.getUsage());
                String params = getParameters().isEmpty() ? "" : user.getTranslation(getParameters());
                String desc = getDescription().isEmpty() ? "" : user.getTranslation(getDescription());
                // Show the help
                if (user.isPlayer()) {
                    // Player. Check perms
                    if (user.hasPermission(parent.getPermission())) {
                        user.sendMessage(HELP_SYNTAX_REF, USAGE_PLACEHOLDER, usage, PARAMS_PLACEHOLDER, params, DESC_PLACEHOLDER, desc);
                    } else {
                        // No permission, nothing to see here. If you don't have permission, you cannot see any sub commands
                        return true;
                    }
                } else if (!parent.isOnlyPlayer()) {
                    // Console. Only show if it is a console command
                    user.sendMessage(HELP_SYNTAX_REF, USAGE_PLACEHOLDER, usage, PARAMS_PLACEHOLDER, params, DESC_PLACEHOLDER, desc);
                }
            }
            // Increment the depth
            int newDepth = depth + 1;
            // Run through any subcommands and get their help
            for (CompositeCommand subCommand : parent.getSubCommands().values()) {
                // Ignore the help command
                if (!subCommand.getLabel().equals("help")) {
                    // Every command should have help because every command has a default help
                    Optional<CompositeCommand> sub = subCommand.getSubCommand("help");
                    if (sub.isPresent()) {
                        sub.get().execute(user, Arrays.asList(String.valueOf(newDepth)));
                    }
                }
            }
        }

        if (depth == 0) {
            user.sendMessage("commands.help.end");
        }
        return true;
    }

}
