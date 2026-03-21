package world.bentobox.bentobox.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.math.NumberUtils;

import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Provides a standardized help system for BentoBox commands.
 * <p>
 * This command is automatically added to every {@link CompositeCommand} that doesn't
 * define its own help sub-command. It displays command usage, parameters, and descriptions
 * in a paginated format, respecting permissions and command visibility settings.
 * <p>
 * Features:
 * <ul>
 *   <li>Paginated help display with {@value #COMMANDS_PER_PAGE} commands per page</li>
 *   <li>Permission-based filtering of commands</li>
 *   <li>Support for console and player-specific commands</li>
 *   <li>Page selection via numeric argument (e.g., {@code /is help 2})</li>
 * </ul>
 * <p>
 * Usage: {@code /parentcommand help [page]}
 * 
 * @author tastybento
 * @since 1.0
 */
public class DefaultHelpCommand extends CompositeCommand {

    /** Number of commands displayed per help page */
    protected static final int COMMANDS_PER_PAGE = 10;

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
     * Executes the help command, displaying formatted and paginated help information.
     * <p>
     * The help display:
     * <ol>
     *   <li>Checks if parent command is hidden</li>
     *   <li>Processes optional page number argument (defaults to page 1)</li>
     *   <li>Collects all visible commands via {@link #getHelpEntries(User)}</li>
     *   <li>Shows the requested page with header, commands, page indicator, and footer</li>
     * </ol>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Hidden commands don't show help
        if (parent.isHidden()) return true;

        // Determine page number (1-indexed, defaults to 1)
        int page = 1;
        if (!args.isEmpty()) {
            if (args.size() == 1 && NumberUtils.isDigits(args.getFirst())) {
                page = Math.max(1, NumberUtils.toInt(args.getFirst(), 1));
            } else {
                // Show basic syntax help if argument isn't a valid page number
                String usage = parent.getUsage();
                String params = user.getTranslation("commands.help.parameters");
                String desc = user.getTranslation("commands.help.description");
                user.sendMessage(HELP_SYNTAX_REF, USAGE_PLACEHOLDER, usage,
                        PARAMS_PLACEHOLDER, params, DESC_PLACEHOLDER, desc);
                return true;
            }
        }

        // Collect all visible help entries for this command
        List<String[]> helpEntries = getHelpEntries(user);

        // Calculate total pages
        int totalPages = Math.max(1, (helpEntries.size() + COMMANDS_PER_PAGE - 1) / COMMANDS_PER_PAGE);
        page = Math.min(page, totalPages);

        // Show header
        String labelText = getWorld() != null ?
                getIWM().getFriendlyName(getWorld()) :
                user.getTranslation("commands.help.console");
        user.sendMessage("commands.help.header", TextVariables.LABEL, labelText);

        // Display commands for this page
        int start = (page - 1) * COMMANDS_PER_PAGE;
        int end = Math.min(start + COMMANDS_PER_PAGE, helpEntries.size());
        for (int i = start; i < end; i++) {
            String[] entry = helpEntries.get(i);
            // entry[0] = usage, entry[1] = params, entry[2] = description
            if (entry[1] == null || entry[1].isEmpty()) {
                user.sendMessage(HELP_SYNTAX_NO_PARAMETERS_REF, USAGE_PLACEHOLDER, entry[0], DESC_PLACEHOLDER, entry[2]);
            } else {
                user.sendMessage(HELP_SYNTAX_REF, USAGE_PLACEHOLDER, entry[0], PARAMS_PLACEHOLDER, entry[1], DESC_PLACEHOLDER, entry[2]);
            }
        }

        // Show page indicator when there are multiple pages
        if (totalPages > 1) {
            user.sendMessage("commands.help.page", "[page]", String.valueOf(page), "[total]", String.valueOf(totalPages));
        }

        // Show footer
        user.sendMessage("commands.help.end");
        return true;
    }

    /**
     * Collects all help entries for the parent command and its direct sub-commands
     * that the user has permission to see.
     * <p>
     * Override this method to customize the list of commands shown in the help output.
     *
     * @param user The user requesting help
     * @return List of help entries; each entry is a {@code String[]} of
     *         {@code {usage, parameters, description}}
     */
    protected List<String[]> getHelpEntries(User user) {
        List<String[]> entries = new ArrayList<>();
        // Add parent command's own entry if it's not the help command itself
        if (!parent.getLabel().equals(HELP)) {
            addHelpEntry(user, parent, entries);
        }
        // Add entries for all visible sub-commands
        for (CompositeCommand subCommand : parent.getSubCommands().values()) {
            if (!subCommand.getLabel().equals(HELP)) {
                addHelpEntry(user, subCommand, entries);
            }
        }
        return entries;
    }

    /**
     * Adds a formatted help entry for the given command to the list, if the user
     * has the required permission and the command is not hidden.
     *
     * @param user    The user requesting help
     * @param command The command whose entry should be added
     * @param entries The list to add the entry to
     */
    protected void addHelpEntry(User user, CompositeCommand command, List<String[]> entries) {
        if (command.isHidden()) return;
        // Permission check
        if (user.isPlayer()) {
            if (!user.hasPermission(command.getPermission())) return;
        } else if (command.isOnlyPlayer()) {
            return;
        }
        String usage = command.getUsage();
        String params = user.getTranslationOrNothing(command.getParameters());
        String desc = user.getTranslation(command.getDescription());
        entries.add(new String[]{usage, params != null ? params : "", desc});
    }

    /**
     * Provides tab-completion of page numbers for the help command.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 1) {
            List<String[]> entries = getHelpEntries(user);
            int totalPages = Math.max(1, (entries.size() + COMMANDS_PER_PAGE - 1) / COMMANDS_PER_PAGE);
            List<String> pageNumbers = new ArrayList<>();
            for (int i = 1; i <= totalPages; i++) {
                pageNumbers.add(String.valueOf(i));
            }
            return Optional.of(pageNumbers);
        }
        return Optional.empty();
    }

    /**
     * Displays formatted help for a single command, respecting permissions and command type.
     * <p>
     * This method is retained for backward compatibility with subclasses.
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
