package world.bentobox.bentobox.api.commands;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.math.NumberUtils;

import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Provides a standardized help system for BentoBox commands.
 * <p>
 * This command is automatically added to every {@link CompositeCommand} that doesn't
 * define its own help sub-command. It displays command usage, parameters, and descriptions
 * in a hierarchical format, respecting permissions and command visibility settings.
 * <p>
 * Features:
 * <ul>
 *   <li>Hierarchical help display up to {@value #MAX_DEPTH} levels deep</li>
 *   <li>Permission-based filtering of commands</li>
 *   <li>Support for console and player-specific commands</li>
 *   <li>Pagination support via depth parameter</li>
 * </ul>
 * <p>
 * Usage: {@code /parentcommand help [depth]}
 * 
 * @author tastybento
 * @since 1.0
 */
public class DefaultHelpCommand extends CompositeCommand {

    /** Maximum depth of sub-commands to display in help */
    protected static final int MAX_DEPTH = 2;
    
    /** Placeholders used in help message formatting */
    protected static final String USAGE_PLACEHOLDER = "[usage]";
    protected static final String PARAMS_PLACEHOLDER = "[parameters]";
    protected static final String DESC_PLACEHOLDER = "[description]";
    
    /** Localization keys for help message templates */
    protected static final String HELP_SYNTAX_REF = "commands.help.syntax";
    protected static final String HELP_SYNTAX_NO_PARAMETERS_REF = "commands.help.syntax-no-parameters";
    
    /** Standard label for help commands */
    protected static final String HELP = "help";

    /**
     * Creates a help command for the specified parent command.
     * Inherits permissions and parameters from the parent.
     *
     * @param parent The command that this help command belongs to
     */
    public DefaultHelpCommand(CompositeCommand parent) {
        super(parent, HELP);
    }

    @Override
    public void setup() {
        // Inherit parameters and description from parent command
        setParametersHelp(parent.getParameters());
        setDescription(parent.getDescription());
        inheritPermission();
    }

    /**
     * Executes the help command, displaying formatted help information.
     * <p>
     * The help display:
     * <ol>
     *   <li>Checks if parent command is hidden</li>
     *   <li>Processes optional depth parameter</li>
     *   <li>Shows help header (if depth = 0)</li>
     *   <li>Displays command help up to MAX_DEPTH</li>
     *   <li>Shows help footer (if depth = 0)</li>
     * </ol>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Hidden commands don't show help
        if (parent.isHidden()) return true;

        // Process depth parameter (0 = top level, increases for sub-commands)
        int depth = 0;
        if (args.size() == 1) {
            if (NumberUtils.isDigits(args.get(0))) {
                depth = Optional.ofNullable(args.get(0)).map(NumberUtils::toInt).orElse(-1);
            } else {
                // Show basic syntax help if argument isn't a valid depth
                String usage = parent.getUsage();
                String params = user.getTranslation("commands.help.parameters");
                String desc = user.getTranslation("commands.help.description");
                user.sendMessage(HELP_SYNTAX_REF, USAGE_PLACEHOLDER, usage, 
                    PARAMS_PLACEHOLDER, params, DESC_PLACEHOLDER, desc);
                return true;
            }
        }

        // Show header for top-level help
        if (depth == 0) {
            String labelText = getWorld() != null ? 
                getIWM().getFriendlyName(getWorld()) : 
                user.getTranslation("commands.help.console");
            user.sendMessage("commands.help.header", TextVariables.LABEL, labelText);
        }

        // Display help content if within depth limit
        if (depth < MAX_DEPTH) {
            if (!parent.getLabel().equals(HELP)) {
                String usage = parent.getUsage();
                String params = user.getTranslationOrNothing(getParameters());
                String desc = user.getTranslation(getDescription());

                if (showPrettyHelp(user, usage, params, desc)) {
                    return true; // Exit if no permission
                }
            }
            // Show help for sub-commands at next depth level
            runSubCommandHelp(user, depth + 1);
        }

        // Show footer for top-level help
        if (depth == 0) {
            user.sendMessage("commands.help.end");
        }
        return true;
    }

    /**
     * Recursively displays help for all sub-commands at the specified depth.
     * Skips the help command itself to avoid infinite recursion.
     *
     * @param user     The user to show help to
     * @param newDepth The depth level for sub-commands
     */
    protected void runSubCommandHelp(User user, int newDepth) {
        for (CompositeCommand subCommand : parent.getSubCommands().values()) {
            // Ignore the help command
            if (!subCommand.getLabel().equals(HELP)) {
                // Every command should have help because every command has a default help
                Optional<CompositeCommand> sub = subCommand.getSubCommand(HELP);
                sub.ifPresent(compositeCommand -> compositeCommand.execute(user, HELP, Collections.singletonList(String.valueOf(newDepth))));
            }
        }
    }

    /**
     * Displays formatted help for a single command, respecting permissions and command type.
     *
     * @param user   The user to show help to
     * @param usage  The command usage string
     * @param params The command parameters description
     * @param desc   The command description
     * @return true if help display should stop (e.g., no permission), false to continue
     */
    protected boolean showPrettyHelp(User user, String usage, String params, String desc) {
        // Show the help
        if (user.isPlayer()) {
            // Player. Check perms
            if (user.hasPermission(parent.getPermission())) {
                if (params == null || params.isEmpty()) {
                    user.sendMessage(HELP_SYNTAX_NO_PARAMETERS_REF, USAGE_PLACEHOLDER, usage, DESC_PLACEHOLDER, desc);
                } else {
                    user.sendMessage(HELP_SYNTAX_REF, USAGE_PLACEHOLDER, usage, PARAMS_PLACEHOLDER, params, DESC_PLACEHOLDER, desc);
                }
            } else {
                // No permission, nothing to see here. If you don't have permission, you cannot see any sub commands
                return true;
            }
        } else if (!parent.isOnlyPlayer()) {
            // Console. Only show if it is a console command
            if (params == null || params.isEmpty()) {
                user.sendMessage(HELP_SYNTAX_NO_PARAMETERS_REF, USAGE_PLACEHOLDER, usage, DESC_PLACEHOLDER, desc);
            } else {
                user.sendMessage(HELP_SYNTAX_REF, USAGE_PLACEHOLDER, usage, PARAMS_PLACEHOLDER, params, DESC_PLACEHOLDER, desc);
            }
        }
        return false;
    }

}
