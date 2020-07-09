package world.bentobox.bentobox.api.commands.island;


import java.util.Collections;
import java.util.List;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;


/**
 * This is default player command class. It contains all necessary parts for main /[gamemode] command.
 * @since 1.13.0
 * @author BONNe
 */
public abstract class DefaultPlayerCommand extends CompositeCommand
{
    /**
     * This is the top-level command constructor for commands that have no parent.
     *
     * @param addon   - GameMode addon
     */
    public DefaultPlayerCommand(GameModeAddon addon)
    {
        // Register command with alias from config.
        super(addon,
                addon.getWorldSettings().getPlayerCommandAliases().split(" ")[0],
                addon.getWorldSettings().getPlayerCommandAliases().split(" "));
    }


    /**
     * Setups anything that is necessary for default main user command.
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#setup()
     */
    @Override
    public void setup()
    {
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
        new IslandSethomeCommand(this);
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
    }


    /**
     * Defines what will be executed when this command is run.
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#execute(User, String, List&lt;String&gt;)
     */
    @Override
    public boolean execute(User user, String label, List<String> args)
    {
        if (user == null)
        {
            return false;
        }

        if (!args.isEmpty())
        {
            user.sendMessage("general.errors.unknown-command", TextVariables.LABEL, this.getTopLabel());
            return false;
        }

        // Check if user has an island.
        if (this.getIslands().getIsland(this.getWorld(), user.getUniqueId()) != null)
        {
            // Default command if user has an island.
            String command = this.<GameModeAddon>getAddon().getWorldSettings().getDefaultPlayerAction();

            // Perform command or use "go" command.
            if (command != null && user.performCommand(label + " " + command))
            {
                return true;
            }
            else
            {
                return this.getSubCommand("go").
                        map(goCmd -> goCmd.call(user, goCmd.getLabel(), Collections.emptyList())).
                        orElse(false);
            }
        }
        else
        {
            // Default command if user does not have an island.
            String command = this.<GameModeAddon>getAddon().getWorldSettings().getDefaultNewPlayerAction();

            // Perform command or use "create" command.
            if (command != null && user.performCommand(label + " " + command))
            {
                return true;
            }
            else
            {
                return this.getSubCommand("create").
                        map(createCmd -> createCmd.call(user, createCmd.getLabel(), Collections.emptyList())).
                        orElse(false);
            }
        }
    }
}
