package world.bentobox.bentobox.api.commands.island;

import java.util.Collections;
import java.util.List;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Provides the default implementation for the main player command in a game mode (e.g., /island, /bskyblock).
 * <p>
 * This class automatically sets up all standard island management commands and handles
 * the default action when a player types the base command with no arguments.
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable command aliases via game mode settings</li>
 *   <li>Automatic registration of standard sub-commands</li>
 *   <li>Configurable default actions for new and existing players</li>
 *   <li>Player-only command restriction</li>
 * </ul>
 * <p>
 * Command categories registered:
 * <ul>
 *   <li>Teleportation (go, spawn)</li>
 *   <li>Island Creation (create, reset)</li>
 *   <li>Information (info)</li>
 *   <li>Settings (settings, setname, resetname, language)</li>
 *   <li>Moderation (ban, unban, banlist, expel)</li>
 *   <li>Navigation (near)</li>
 *   <li>Team Management (team and sub-commands)</li>
 *   <li>Home Management (sethome, delhome, renamehome, homes)</li>
 * </ul>
 * 
 * @since 1.13.0
 * @author BONNe
 */
public abstract class DefaultPlayerCommand extends CompositeCommand {

    /**
     * Creates the default player command for a game mode.
     * Uses the first alias from config as the main label.
     *
     * @param addon The game mode addon creating this command
     */
    protected DefaultPlayerCommand(GameModeAddon addon) {
        // Register command with alias from config.
        // The first command listed is the "label" and the others are aliases.
        super(addon,
                addon.getWorldSettings().getPlayerCommandAliases().split(" ")[0],
                addon.getWorldSettings().getPlayerCommandAliases().split(" "));
    }

    /**
     * Setups anything that is necessary for default main user command.
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#setup()
     */
    @Override
    public void setup() {
        // Description
        this.setDescription("commands.island.help.description");
        // Limit to player
        this.setOnlyPlayer(true);
        // Permission
        this.setPermission("island");

        // Set up default subcommands

        // Teleport commands
        new IslandGoCommand(this);
        new IslandSpawnCommand(this);

        // Allows to create/reset island.
        new IslandCreateCommand(this);
        new IslandResetCommand(this);

        // Displays info about the island.
        new IslandInfoCommand(this);

        // Settings related commands
        new IslandSettingsCommand(this);
        new IslandSetnameCommand(this);
        new IslandResetnameCommand(this);
        new IslandLanguageCommand(this);

        // Ban related commands
        new IslandBanCommand(this);
        new IslandUnbanCommand(this);
        new IslandBanlistCommand(this);

        // Kicks visitors or coops/trusted from island
        new IslandExpelCommand(this);

        // Tells owner of adjacent islands
        new IslandNearCommand(this);

        // Team commands
        new IslandTeamCommand(this);

        // Home commands
        new IslandSethomeCommand(this);
        new IslandDeletehomeCommand(this);
        new IslandRenamehomeCommand(this);
        new IslandHomesCommand(this);
    }


    /**
     * Handles the execution of the base command with no arguments.
     * <p>
     * Behavior:
     * <ul>
     *   <li>If user has an island: Executes the configured default player action (default: "go")</li>
     *   <li>If user has no island: Executes the configured default new player action (default: "create")</li>
     * </ul>
     * The default actions can be configured in the game mode's world settings.
     *
     * @param user  The user executing the command
     * @param label The command label used
     * @param args  Command arguments (must be empty for default behavior)
     * @return true if the command was executed successfully
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (user == null) {
            return false;
        }

        if (!args.isEmpty()) {
            user.sendMessage("general.errors.unknown-command", TextVariables.LABEL, this.getTopLabel());
            return false;
        }

        // Check if user has an island.
        if (this.getIslands().getIsland(this.getWorld(), user.getUniqueId()) != null) {
            // Default command if user has an island.
            return runCommand(user, label, this.<GameModeAddon>getAddon().getWorldSettings().getDefaultPlayerAction(), "go");
        } else {
            // Default command if user does not have an island.
            return runCommand(user, label, this.<GameModeAddon>getAddon().getWorldSettings().getDefaultNewPlayerAction(), "create");
        }
    }

    /**
     * Executes a sub-command or direct command based on the configured action.
     * <p>
     * If the command is a registered sub-command, it will be executed through the command system.
     * Otherwise, it will be executed directly through the server's command system, allowing
     * integration with other plugins (e.g., DeluxeMenus).
     *
     * @param user            The user executing the command
     * @param label          The command label used
     * @param command        The configured action to execute
     * @param defaultSubCommand Fallback sub-command if no action is configured
     * @return true if the command was executed successfully
     */
    private boolean runCommand(User user, String label, String command, String defaultSubCommand) {
        if (command == null || command.isEmpty()) {
            command = defaultSubCommand;
        }
        // Call sub command or perform command if it does not exist
        if (this.getSubCommand(command).isPresent()) {
            return this.getSubCommand(command).
                    map(c -> c.call(user, c.getLabel(), Collections.emptyList())).
                    orElse(false);
        } else {
            // Command is not a known sub command - try to perform it directly - some plugins trap these commands, like Deluxe menus
            if (command.startsWith("/")) {
                // If commands starts with Slash, don't append the prefix
                return user.performCommand(command.substring(1));
            } else {
                return user.performCommand(label + " " + command);
            }
        }
    }
}